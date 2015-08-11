/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2015
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ChromatiCraft.Block.Dimension.Structure;

import java.util.HashMap;
import java.util.List;

import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import mcp.mobius.waila.api.IWailaDataProvider;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.DragonAPICore;
import Reika.DragonAPI.ModList;
import Reika.DragonAPI.ASM.APIStripper.Strippable;
import Reika.DragonAPI.ASM.DependentMethodStripper.ModDependent;
import Reika.DragonAPI.Instantiable.Data.BlockStruct.StructuredBlockArray;
import Reika.DragonAPI.Instantiable.Data.Immutable.BlockVector;
import Reika.DragonAPI.Instantiable.Data.Immutable.Coordinate;
import Reika.DragonAPI.Libraries.ReikaDirectionHelper;
import Reika.DragonAPI.Libraries.Java.ReikaObfuscationHelper;
import Reika.DragonAPI.Libraries.MathSci.ReikaVectorHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


@Strippable(value="mcp.mobius.waila.api.IWailaDataProvider")
public class BlockTeleport extends Block implements IWailaDataProvider {

	private static boolean debug = ReikaObfuscationHelper.isDeObfEnvironment() && DragonAPICore.isReikasComputer();

	public BlockTeleport(Material mat) {
		super(mat);

		this.setResistance(60000);
		this.setBlockUnbreakable();
		this.setCreativeTab(ChromatiCraft.tabChromaGen);
	}

	@Override
	public int getLightOpacity(IBlockAccess world, int x, int y, int z) {
		return 0;
	}

	@Override
	public TileEntity createTileEntity(World world, int meta) {
		return meta == 0 ? new TileEntityTeleport() : null;
	}

	@Override
	public boolean hasTileEntity(int meta) {
		return meta == 0;
	}

	@Override
	public int getRenderType() {
		return debug ? 0 : -1;
	}

	@Override
	public boolean renderAsNormalBlock() {
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public AxisAlignedBB getCollisionBoundingBoxFromPool(World world, int x, int y, int z) {
		return null;
	}

	@Override
	public boolean canCollideCheck(int meta, boolean boat) {
		return debug;
	}

	@Override
	public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer ep, int s, float a, float b, float c) {
		return false;
	}

	@Override
	public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity e) {
		if (world.isRemote)
			return;
		TileEntity te = this.getTileEntity(world, x, y, z);
		if (te instanceof TileEntityTeleport)
			((TileEntityTeleport)te).teleport(e);
		else
			ChromatiCraft.logger.logError("Teleport at "+x+","+y+","+z+" is null?!");
	}

	private TileEntityTeleport getTileEntity(World world, int x, int y, int z) {
		int meta = world.getBlockMetadata(x, y, z);
		if (meta == 0) {
			return (TileEntityTeleport)world.getTileEntity(x, y, z);
		}
		else {
			/*
			switch(meta) {
				case 1:
					return this.getTileEntity(world, x, y+1, z);
				case 2:
					return this.getTileEntity(world, x, y-1, z);
				case 3:
					return this.getTileEntity(world, x+1, y, z); //right, EW plane
				case 4:
					return this.getTileEntity(world, x-1, y, z); //left, EW plane
				case 5:
					return this.getTileEntity(world, x, y, z+1); //right, NS plane
				case 6:
					return this.getTileEntity(world, x, y, z-1); //left, NS plane
			}
			 */
			StructuredBlockArray arr = new StructuredBlockArray(world);
			arr.recursiveAddWithBounds(world, x, y, z, this, x-4, y-4, z-4, x+4, y+4, z+4);
			int mx = arr.getMidX();
			int my = arr.getMinY();
			int mz = arr.getMidZ();
			Block b = world.getBlock(mx, my, mz);
			if (b == this)
				return (TileEntityTeleport)world.getTileEntity(mx, my, mz);
		}
		return null;
	}

	@Override
	@ModDependent(ModList.WAILA)
	public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return new ItemStack(Blocks.air);
	}

	@Override
	@ModDependent(ModList.WAILA)
	public List<String> getWailaHead(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return tip;
	}

	@Override
	@ModDependent(ModList.WAILA)
	public List<String> getWailaBody(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return tip;
	}

	@Override
	@ModDependent(ModList.WAILA)
	public List<String> getWailaTail(ItemStack itemStack, List<String> tip, IWailaDataAccessor accessor, IWailaConfigHandler config) {
		return tip;
	}

	@Override
	@ModDependent(ModList.WAILA)
	public NBTTagCompound getNBTData(EntityPlayerMP player, TileEntity te, NBTTagCompound tag, World world, int x, int y, int z) {
		return tag;
	}

	public static class TileEntityTeleport extends TileEntity {

		/** Is a relative position! */
		public BlockVector destination;
		public ForgeDirection facing;

		private long lastActiveTick = -1;

		private final HashMap<Coordinate, TeleportTriggerAction> actions = new HashMap();

		public boolean isActive = true;

		@Override
		public boolean canUpdate() {
			return false;
		}

		private void teleport(Entity e) {
			if (worldObj.isRemote)
				return;
			if (!isActive)
				return;
			if (destination == null) {
				if (debug)
					ChromatiCraft.logger.logError("Could not teleport "+e+"; null destination!");
				return;
			}
			long time = worldObj.getTotalWorldTime();
			if (time <= lastActiveTick) {
				return;
			}
			lastActiveTick = time;

			double dx = e.posX-xCoord-0.5;
			double dy = e.posY-yCoord-0.5;
			double dz = e.posZ-zCoord-0.5;
			float yaw = this.getYaw(e);

			Vec3 vec = Vec3.createVectorHelper(dx, dy, dz);
			vec = ReikaVectorHelper.rotateVector(vec, 0, yaw, 0);

			double nx = xCoord+0.5+destination.xCoord+vec.xCoord;
			double ny = yCoord+0.5+destination.yCoord+vec.yCoord;
			double nz = zCoord+0.5+destination.zCoord+vec.zCoord;

			//Vec3 vvec = Vec3.createVectorHelper(e.motionX, e.motionY, e.motionZ);
			//vvec = ReikaVectorHelper.rotateVector(vvec, 0, -yaw, 0);
			//e.setVelocity(vvec.xCoord, vvec.yCoord, vvec.zCoord);
			e.setVelocity(0, 0, 0);

			if (e instanceof EntityPlayer) {
				e.rotationYaw += yaw;
				((EntityPlayer)e).rotationYawHead += yaw;
				((EntityPlayer)e).prevRotationYawHead += yaw;
				e.prevRotationYaw += yaw;
				((EntityPlayer)e).setPositionAndUpdate(nx, ny, nz);
				byte a = (byte)(MathHelper.floor_float(((EntityPlayer)e).rotationYawHead*256.0F/360.0F));
				((EntityPlayerMP)e).playerNetServerHandler.sendPacket(new S19PacketEntityHeadLook(e, a));
				onTeleport((EntityPlayer)e, this);
			}
			else {
				e.setLocationAndAngles(nx, ny, nz, e.rotationYaw+yaw, e.rotationPitch);
			}
		}

		private float getYaw(Entity e) {
			int rel = ReikaDirectionHelper.getRelativeAngle(facing, destination.direction);
			if (rel > 180)
				rel = rel-360;
			return rel;
		}

		@SideOnly(Side.CLIENT)
		private void setRenderPos(EntityPlayer ep) {
			RenderManager rm = RenderManager.instance;
			if (ep.getUniqueID().equals(Minecraft.getMinecraft().thePlayer.getUniqueID())) {
				//RenderManager.renderPosX = RenderManager.instance.viewerPosX = nx;
				//RenderManager.renderPosY = RenderManager.instance.viewerPosY = ny;
				//RenderManager.renderPosZ = RenderManager.instance.viewerPosZ = nz;
				rm.cacheActiveRenderInfo(rm.worldObj, rm.renderEngine, Minecraft.getMinecraft().fontRenderer, ep, rm.field_147941_i, rm.options, 0);
			}
		}

		@Override
		public void writeToNBT(NBTTagCompound NBT) {
			super.writeToNBT(NBT);

			if (facing != null)
				NBT.setInteger("face", facing.ordinal());
			if (destination != null) {
				NBTTagCompound tag = new NBTTagCompound();
				destination.writeToNBT(tag);
				NBT.setTag("pos", tag);
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound NBT) {
			super.readFromNBT(NBT);

			facing = ForgeDirection.VALID_DIRECTIONS[NBT.getInteger("face")];
			NBTTagCompound tag = NBT.getCompoundTag("pos");
			destination = BlockVector.readFromNBT(tag);
		}

		@Override
		public Packet getDescriptionPacket() {
			NBTTagCompound NBT = new NBTTagCompound();
			this.writeToNBT(NBT);
			S35PacketUpdateTileEntity pack = new S35PacketUpdateTileEntity(xCoord, yCoord, zCoord, 0, NBT);
			return pack;
		}

		@Override
		public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity p)  {
			this.readFromNBT(p.field_148860_e);
			worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
		}

		private void onPlayerTeleported(EntityPlayer ep, TileEntityTeleport te) {
			if (!actions.isEmpty()) {
				Coordinate rel = new Coordinate(this).offset(xCoord, yCoord, zCoord);
				TeleportTriggerAction a = actions.get(rel);
				a.trigger(rel, this);
			}
		}

		public void putAction(Coordinate rel, TeleportTriggerAction a) {
			actions.put(rel, a);
		}

	}

	private static void onTeleport(EntityPlayer ep, TileEntityTeleport te) {
		for (TileEntity tile : ((List<TileEntity>)te.worldObj.loadedTileEntityList)) {
			if (!tile.isInvalid() && tile instanceof TileEntityTeleport) {
				((TileEntityTeleport)tile).onPlayerTeleported(ep, te);
			}
		}
	}

	public static interface TeleportTriggerAction {

		void trigger(Coordinate c, TileEntityTeleport te);

	}

	public static class Activate implements TeleportTriggerAction {

		public static final Activate instance = new Activate();

		private Activate() {

		}

		@Override
		public void trigger(Coordinate c, TileEntityTeleport te) {
			te.isActive = true;
		}

	}

	public static class Deactivate implements TeleportTriggerAction {

		public static final Deactivate instance = new Deactivate();

		private Deactivate() {

		}

		@Override
		public void trigger(Coordinate c, TileEntityTeleport te) {
			te.isActive = false;
		}

	}

	public static class DeactivateOneOf implements TeleportTriggerAction {

		public static final DeactivateOneOf instance = new DeactivateOneOf();

		private DeactivateOneOf() {

		}

		@Override
		public void trigger(Coordinate c, TileEntityTeleport te) {
			te.actions.remove(c);
			if (this.shouldDeactivate(te))
				te.isActive = false;
		}

		private boolean shouldDeactivate(TileEntityTeleport te) {
			for (TeleportTriggerAction a : te.actions.values()) {
				if (a instanceof DeactivateOneOf)
					return false;
			}
			return true;
		}

	}

	public static class Reroute implements TeleportTriggerAction {

		private final BlockVector newDestination;

		public Reroute(BlockVector vec) {
			newDestination = vec;
		}

		@Override
		public void trigger(Coordinate c, TileEntityTeleport te) {
			te.destination = newDestination;
		}

	}

	public static class RerouteIf implements TeleportTriggerAction {

		private final BlockVector newDestination;

		public RerouteIf(BlockVector vec) {
			newDestination = vec;
		}

		@Override
		public void trigger(Coordinate c, TileEntityTeleport te) {
			te.actions.remove(c);
			if (this.shouldReroute(te))
				te.destination = newDestination;
		}

		private boolean shouldReroute(TileEntityTeleport te) {
			for (TeleportTriggerAction a : te.actions.values()) {
				if (a instanceof RerouteIf)
					return false;
			}
			return true;
		}

	}



}