/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2014
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ChromatiCraft.TileEntity;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import Reika.ChromatiCraft.ChromatiCraft;
import Reika.ChromatiCraft.Base.TileEntity.CrystalTransmitterBase;
import Reika.ChromatiCraft.Magic.CrystalSource;
import Reika.ChromatiCraft.Registry.ChromaPackets;
import Reika.ChromatiCraft.Registry.ChromaSounds;
import Reika.ChromatiCraft.Registry.ChromaTiles;
import Reika.ChromatiCraft.Registry.CrystalElement;
import Reika.ChromatiCraft.Render.Particle.EntityBlurFX;
import Reika.ChromatiCraft.Render.Particle.EntityFlareFX;
import Reika.ChromatiCraft.Render.Particle.EntityRuneFX;
import Reika.DragonAPI.Instantiable.Data.BlockArray;
import Reika.DragonAPI.Libraries.ReikaAABBHelper;
import Reika.DragonAPI.Libraries.IO.ReikaColorAPI;
import Reika.DragonAPI.Libraries.IO.ReikaPacketHelper;
import Reika.DragonAPI.Libraries.Java.ReikaRandomHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
//Make player able to manufacture in the very late game, otherwise rare worldgen
public class TileEntityCrystalPylon extends CrystalTransmitterBase implements CrystalSource {

	private boolean hasMultiblock = false;
	private CrystalElement color = CrystalElement.WHITE;
	public int randomOffset = rand.nextInt(360);
	public static final int MAX_ENERGY = 180000;
	private int energy = MAX_ENERGY;

	@Override
	public ChromaTiles getTile() {
		return ChromaTiles.PYLON;
	}

	@Override
	public boolean isConductingElement(CrystalElement e) {
		return e == color;
	}

	public CrystalElement getColor() {
		return color;
	}

	public int getEnergy(CrystalElement e) {
		return e == color ? energy : 0;
	}

	public int getRenderColor() {
		return ReikaColorAPI.mixColors(color.getColor(), 0x888888, (float)energy/MAX_ENERGY);
	}

	@Override
	public void updateEntity(World world, int x, int y, int z, int meta) {
		super.updateEntity(world, x, y, z, meta);

		/*
		if (!hasMultiblock) {
			color = CrystalElement.BLACK;
			FilledBlockArray b = ChromaStructures.getPylonStructure(world, x, y-9, z, color);
			b.place();
			hasMultiblock = true;
		}*/

		if (hasMultiblock) {
			//ReikaJavaLibrary.pConsole(energy, Side.SERVER, color == CrystalElement.BLUE);

			this.charge(world, x, y, z);
			energy = Math.min(energy, MAX_ENERGY);

			if (world.isRemote) {
				this.spawnParticle(world, x, y, z);
			}

			if (!world.isRemote && rand.nextInt(40) == 0) {
				int r = 8+rand.nextInt(8);
				AxisAlignedBB box = ReikaAABBHelper.getBlockAABB(x, y, z).expand(r, r, r);
				List<EntityLivingBase> li = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
				for (int i = 0; i < li.size(); i++) {
					EntityLivingBase e = li.get(i);
					boolean attack = !e.isDead && e.getHealth() > 0;
					if (e instanceof EntityPlayer) {
						attack = attack && !((EntityPlayer)e).capabilities.isCreativeMode;
					}
					if (attack)
						this.attackEntity(e);
				}
			}

			if (this.getTicksExisted()%72 == 0) {
				ChromaSounds.POWER.playSoundAtBlock(this);
			}
		}
	}

	private void charge(World world, int x, int y, int z) {
		if (energy < MAX_ENERGY) {
			energy++;
		}

		int a = 1;
		if (energy <= MAX_ENERGY-a) {
			BlockArray blocks = this.getBoosterCrystals(world, x, y, z);
			for (int i = 0; i < blocks.getSize(); i++) {
				energy += a;
				a *= 2;
				if (energy >= MAX_ENERGY) {
					return;
				}
			}
			if (world.isRemote && !blocks.isEmpty())
				this.spawnRechargeParticles(world, x, y, z, blocks);
		}
	}

	@SideOnly(Side.CLIENT)
	private void spawnRechargeParticles(World world, int x, int y, int z, BlockArray blocks) {
		for (int i = 0; i < blocks.getSize(); i++) {
			int[] xyz = blocks.getNthBlock(i);//blocks.getNthBlock(this.getTicksExisted()%blocks.getSize());
			int dx = xyz[0];
			int dy = xyz[1];
			int dz = xyz[2];
			double ddx = dx-x;
			double ddy = dy-y-0.25;
			double ddz = dz-z;
			double dd = ReikaMathLibrary.py3d(ddx, ddy, ddz);
			double v = 0.125;
			double vx = -v*ddx/dd;
			double vy = -v*ddy/dd;
			double vz = -v*ddz/dd;
			double px = dx+0.5;
			double py = dy+0.125;
			double pz = dz+0.5;
			//EntityRuneFX fx = new EntityRuneFX(world, dx+0.5, dy+0.5, dz+0.5, vx, vy, vz, color);
			float sc = (float)(2F+Math.sin(4*Math.toRadians(this.getTicksExisted()+i*90/blocks.getSize())));
			EntityBlurFX fx = new EntityBlurFX(color, world, px, py, pz, vx, vy, vz).setScale(sc).setLife(38).setNoSlowdown();
			//EntityLaserFX fx = new EntityLaserFX(color, world, px, py, pz, vx, vy, vz).setScale(3);
			Minecraft.getMinecraft().effectRenderer.addEffect(fx);
		}
	}

	private BlockArray getBoosterCrystals(World world, int x, int y, int z) {
		BlockArray blocks = new BlockArray();
		Block b = ChromaTiles.CRYSTAL.getBlock();
		int meta = ChromaTiles.CRYSTAL.getBlockMetadata();
		blocks.addBlockCoordinateIf(world, x-3, y-3, z-1, b, meta);
		blocks.addBlockCoordinateIf(world, x-1, y-3, z-3, b, meta);

		blocks.addBlockCoordinateIf(world, x+3, y-3, z-1, b, meta);
		blocks.addBlockCoordinateIf(world, x+1, y-3, z-3, b, meta);

		blocks.addBlockCoordinateIf(world, x-3, y-3, z+1, b, meta);
		blocks.addBlockCoordinateIf(world, x-1, y-3, z+3, b, meta);

		blocks.addBlockCoordinateIf(world, x+3, y-3, z+1, b, meta);
		blocks.addBlockCoordinateIf(world, x+1, y-3, z+3, b, meta);
		return blocks;
	}

	@SideOnly(Side.CLIENT)
	public void particleAttack(int x, int y, int z) {
		int n = 8+rand.nextInt(24);
		for (int i = 0; i < n; i++) {
			float rx = xCoord+rand.nextFloat();
			float ry = yCoord+rand.nextFloat();
			float rz = zCoord+rand.nextFloat();
			double dx = x-xCoord;
			double dy = y-yCoord;
			double dz = z-zCoord;
			double dd = ReikaMathLibrary.py3d(dx, dy, dz);
			double vx = 2*dx/dd;
			double vy = 2*dy/dd;
			double vz = 2*dz/dd;
			EntityFlareFX f = new EntityFlareFX(color, worldObj, rx, ry, rz, vx, vy, vz);
			Minecraft.getMinecraft().effectRenderer.addEffect(f);
		}
	}

	private void attackEntity(EntityLivingBase e) {
		ChromaSounds.DISCHARGE.playSoundAtBlock(this);
		ChromaSounds.DISCHARGE.playSound(worldObj, e.posX, e.posY, e.posZ, 1, 1);

		e.attackEntityFrom(DamageSource.magic, 5);

		int x = MathHelper.floor_double(e.posX);
		int y = MathHelper.floor_double(e.posY);
		int z = MathHelper.floor_double(e.posZ);
		ReikaPacketHelper.sendDataPacket(ChromatiCraft.packetChannel, ChromaPackets.PYLONATTACK.ordinal(), this, x, y, z);
	}

	public void invalidateMultiblock() {
		if (hasMultiblock) {
			ChromaSounds.POWERDOWN.playSoundAtBlock(this);
			ChromaSounds.POWERDOWN.playSound(worldObj, xCoord, yCoord, zCoord, 1F, 2F);
			ChromaSounds.POWERDOWN.playSound(worldObj, xCoord, yCoord, zCoord, 1F, 0.5F);

			if (worldObj.isRemote)
				this.invalidatationParticles();
		}
		hasMultiblock = false;
		this.clearTargets();
		energy = 0;
	}

	@SideOnly(Side.CLIENT)
	private void invalidatationParticles() {
		double d = 1.25;
		int n = 64+rand.nextInt(64);
		for (int i = 0; i < n; i++) {
			double rx = ReikaRandomHelper.getRandomPlusMinus(xCoord+0.5, d);
			double ry = ReikaRandomHelper.getRandomPlusMinus(yCoord+0.5, d);
			double rz = ReikaRandomHelper.getRandomPlusMinus(zCoord+0.5, d);
			double vx = rand.nextDouble()-0.5;
			double vy = rand.nextDouble()-0.5;
			double vz = rand.nextDouble()-0.5;
			EntityRuneFX fx = new EntityRuneFX(worldObj, rx, ry, rz, vx, vy, vz, color);
			Minecraft.getMinecraft().effectRenderer.addEffect(fx);
		}
	}

	public void validateMultiblock() {
		hasMultiblock = true;
	}

	@SideOnly(Side.CLIENT)
	private void spawnParticle(World world, int x, int y, int z) {
		double d = 1.25;
		double rx = ReikaRandomHelper.getRandomPlusMinus(x+0.5, d);
		double ry = ReikaRandomHelper.getRandomPlusMinus(y+0.5, d);
		double rz = ReikaRandomHelper.getRandomPlusMinus(z+0.5, d);
		EntityFlareFX fx = new EntityFlareFX(color, world, rx, ry, rz);
		Minecraft.getMinecraft().effectRenderer.addEffect(fx);
	}

	@Override
	protected void readSyncTag(NBTTagCompound NBT) {
		super.readSyncTag(NBT);

		color = CrystalElement.elements[NBT.getInteger("color")];
		hasMultiblock = NBT.getBoolean("multi");
		energy = NBT.getInteger("energy");
	}

	@Override
	protected void writeSyncTag(NBTTagCompound NBT) {
		super.writeSyncTag(NBT);

		NBT.setInteger("color", color.ordinal());
		NBT.setBoolean("multi", hasMultiblock);
		NBT.setInteger("energy", energy);
	}

	@Override
	public int getSendRange() {
		return 32;
	}

	@Override
	public boolean canConduct() {
		return hasMultiblock;
	}

	@Override
	public int maxThroughput() {
		return 1000;
	}

	@Override
	public int getTransmissionStrength() {
		return 100;
	}

	public void setColor(CrystalElement e) {
		color = e;
	}

	@Override
	public void drain(CrystalElement e, int amt) {
		if (e == color)
			energy -= amt;
	}

}