package Reika.ChromatiCraft.Block.Worldgen;

import java.util.Locale;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.util.ForgeDirection;
import Reika.ChromatiCraft.ChromatiCraft;
import Reika.DragonAPI.Libraries.World.ReikaBlockHelper;
import Reika.DragonAPI.Libraries.World.ReikaWorldHelper;
import Reika.GeoStrata.API.RockProofStone;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;


public class BlockCliffStone extends Block implements RockProofStone {

	private IIcon grassTop;

	private IIcon stoneBlendUp;
	private IIcon stoneBlendDown;

	private IIcon stoneBlendUp_Dirt;
	private IIcon stoneBlendUp_Sand;
	private IIcon stoneBlendUp_Clay;

	private IIcon farmlandTop;

	public BlockCliffStone(Material m) {
		super(m);

		this.setCreativeTab(ChromatiCraft.tabChromaGen);
	}

	@Override
	public float getPlayerRelativeBlockHardness(EntityPlayer ep, World world, int x, int y, int z) {
		return Variants.getVariant(world.getBlockMetadata(x, y, z)).proxy.getPlayerRelativeBlockHardness(ep, world, x, y, z);
	}

	@Override
	public float getExplosionResistance(Entity e, World world, int x, int y, int z, double ex, double ey, double ez) {
		return Variants.getVariant(world.getBlockMetadata(x, y, z)).proxy.getExplosionResistance(e, world, x, y, z, ex, ey, ez);
	}
	/*
	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int meta, int fortune) {
		return Variants.getVariant(meta).proxy.getDrops(world, x, y, z, 0, fortune);
	}
	 */

	@Override
	public int damageDropped(int meta) {
		Variants v = Variants.getVariant(meta);
		if (v == Variants.GRASS)
			v = Variants.DIRT;
		return v.getMeta(false, false);
	}

	@Override
	public int getRenderType() {
		return ChromatiCraft.proxy.cliffstoneRender;
	}

	@Override
	public IIcon getIcon(int s, int meta) {
		Variants v = Variants.getVariant(meta);
		//return v.proxy.getIcon(s, 0);
		switch(v) {
			case DIRT:
				return v.baseTexture;
			case GRASS:
				return s == 0 ? Variants.DIRT.baseTexture : s == 1 ? grassTop : v.baseTexture;
			case STONE:
				return v.baseTexture;
			case FARMLAND:
				return s == 1 ? farmlandTop : Blocks.dirt.getIcon(s, meta);
			default:
				return Blocks.lava.getIcon(0, 0);
		}
	}

	@Override
	public IIcon getIcon(IBlockAccess iba, int x, int y, int z, int s) {
		int meta = iba.getBlockMetadata(x, y, z);
		Variants v = Variants.getVariant(meta);
		if (v == Variants.DIRT) {
			if (s == 0) {
				if (iba.getBlock(x, y-1, z) == Blocks.air)
					return Variants.STONE.baseTexture;
			}
			else if (s > 1) {
				if (iba.getBlockMetadata(x, y-1, z) != meta || 	iba.getBlock(x, y-1, z) != this) {
					return v.blend[0];
				}
			}
		}
		if (v == Variants.STONE && s > 1) {
			Block b = iba.getBlock(x, y+1, z);
			if (b == Blocks.dirt || b == Blocks.grass) {
				return stoneBlendUp_Dirt;
			}
			if (b == Blocks.sand) {
				return stoneBlendUp_Sand;
			}
			if (b == Blocks.clay) {
				return stoneBlendUp_Clay;
			}
			if (b == Blocks.stone || ReikaBlockHelper.isOre(b, iba.getBlockMetadata(x, y+1, z))) {
				return stoneBlendUp;
			}
			b = iba.getBlock(x, y-1, z);
			if (b == Blocks.stone || ReikaBlockHelper.isOre(b, iba.getBlockMetadata(x, y-1, z)))
				return stoneBlendDown;
		}
		return super.getIcon(iba, x, y, z, s);
	}

	@Override
	public void registerBlockIcons(IIconRegister ico) {
		for (int i = 0; i < Variants.list.length; i++) {
			Variants v = Variants.list[i];
			v.registerIcons(ico);
		}
		grassTop = ico.registerIcon("chromaticraft:cliffstone/grass_top_base");
		farmlandTop = ico.registerIcon("chromaticraft:cliffstone/farmland_top");

		stoneBlendUp = ico.registerIcon("chromaticraft:cliffstone/stone_blend_up");
		stoneBlendDown = ico.registerIcon("chromaticraft:cliffstone/stone_blend_down");
		stoneBlendUp_Dirt = ico.registerIcon("chromaticraft:cliffstone/stone_vblend_up");
		stoneBlendUp_Clay = ico.registerIcon("chromaticraft:cliffstone/stone_vblend_up_clay");
		stoneBlendUp_Sand = ico.registerIcon("chromaticraft:cliffstone/stone_vblend_up_sand");
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, Block b)  {
		super.onNeighborBlockChange(world, x, y, z, b);
		if (Variants.getVariant(world.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			Material mat = world.getBlock(x, y + 1, z).getMaterial();
			if (mat.isSolid()) {
				world.setBlock(x, y, z, Blocks.dirt);
			}
		}
	}

	@Override
	public Item getItemDropped(int meta, Random rand, int fortune) {
		if (Variants.getVariant(meta) == Variants.FARMLAND) {
			return Blocks.dirt.getItemDropped(0, rand, fortune);
		}
		return super.getItemDropped(meta, rand, fortune);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public Item getItem(World world, int x, int y, int z) {
		if (Variants.getVariant(world.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			return Item.getItemFromBlock(Blocks.dirt);
		}
		return super.getItem(world, x, y, z);
	}

	@Override
	public boolean canSilkHarvest(World world, EntityPlayer player, int x, int y, int z, int metadata) {
		return false;
	}

	@Override
	public void onFallenUpon(World world, int x, int y, int z, Entity e, float dist) {
		if (Variants.getVariant(world.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			Blocks.farmland.onFallenUpon(world, x, y, z, e, dist);
		}
		else {
			super.onFallenUpon(world, x, y, z, e, dist);
		}
	}

	@Override
	public boolean canSustainPlant(IBlockAccess world, int x, int y, int z, ForgeDirection direction, IPlantable plantable) {
		if (Variants.getVariant(world.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			return Blocks.farmland.canSustainPlant(world, x, y, z, direction, plantable);
		}
		return super.canSustainPlant(world, x, y, z, direction, plantable);
	}

	@Override
	public boolean isFertile(World world, int x, int y, int z) {
		if (Variants.getVariant(world.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			return true;
		}
		return super.isFertile(world, x, y, z);
	}

	@Override
	public int getLightValue(IBlockAccess iba, int x, int y, int z) {
		if (Variants.getVariant(iba.getBlockMetadata(x, y, z)) == Variants.FARMLAND) {
			return 6;
		}
		return 0;
	}

	@Override
	public int getLightOpacity(IBlockAccess iba, int x, int y, int z) {
		return this.isTransparent(iba, x, y, z) ? 0 : super.getLightOpacity(iba, x, y, z);
	}

	@Override
	public boolean isReplaceableOreGen(World world, int x, int y, int z, Block b) {
		return b == this || b == Variants.getVariant(world.getBlockMetadata(x, y, z)).proxy;
	}

	public static boolean isTransparent(IBlockAccess iba, int x, int y, int z) {
		if (iba instanceof World) {
			World w = (World)iba;
			if (!ReikaWorldHelper.isChunkPastNoiseGen(w, x >> 4, z >> 4)) {
				return false;
			}
		}
		return (iba.getBlockMetadata(x, y, z) & 8) != 0;
	}

	@Override
	public boolean blockRockGeneration(World world, int x, int y, int z, Block b, int meta) {
		return true;
	}

	public static enum Variants {
		STONE(Blocks.stone),
		DIRT(Blocks.dirt),
		GRASS(Blocks.grass),
		FARMLAND(Blocks.farmland);

		private final Block proxy;

		private IIcon baseTexture;
		private IIcon[] blend = new IIcon[9];

		public static Variants[] list = values();

		private Variants(Block b) {
			proxy = b;
		}

		@SideOnly(Side.CLIENT)
		private void registerIcons(IIconRegister ico) {
			String s = this.name().toLowerCase(Locale.ENGLISH);
			baseTexture = ico.registerIcon("chromaticraft:cliffstone/"+s+"_base");
			for (int i = 0; i < 9; i++) {
				blend[i] = ico.registerIcon("chromaticraft:cliffstone/"+s+"_blend_down");//ico.registerIcon("chromaticraft:cliffstone/"+s+"_blend_"+i);
			}
		}

		public Block getBlockProxy() {
			return proxy;
		}

		public boolean isGround() {
			return proxy.getMaterial() == Material.ground || proxy.getMaterial() == Material.grass || proxy == Blocks.gravel || proxy == Blocks.clay || proxy == Blocks.sand;
		}

		public static Variants getVariant(int meta) {
			return list[meta%4];
		}

		public int getMeta(boolean unused, boolean transparent) {
			return this.ordinal()+(unused ? 4 : 0)+(transparent ? 8 : 0);
		}
	}

}
