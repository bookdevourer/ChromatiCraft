/*******************************************************************************
 * @author Reika Kalseki
 * 
 * Copyright 2016
 * 
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package Reika.ChromatiCraft.World.Nether;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import Reika.ChromatiCraft.Block.Worldgen.BlockStructureShield.BlockType;
import Reika.ChromatiCraft.Registry.ChromaBlocks;
import Reika.DragonAPI.Instantiable.Data.Maps.ThresholdMapping;
import Reika.DragonAPI.Libraries.MathSci.ReikaMathLibrary;
import Reika.DragonAPI.Libraries.MathSci.SimplexNoiseGenerator;


public class LavaRiverGenerator {

	private static final double RIVER_THRESH = 0.2;
	private static final double RIVER_CENTER_THRESH = 0.1;

	private static final double MIN_HEIGHT = 127;
	private static final double MAX_HEIGHT = 240;

	private static final ThresholdMapping<Block> blockTypes = new ThresholdMapping();

	public final long seed;

	private final SimplexNoiseGenerator placementNoise;
	private final SimplexNoiseGenerator heightNoise;
	//sometimes lava, sometimes pyrotheum
	private final SimplexNoiseGenerator blockNoise;

	public LavaRiverGenerator(World world) {
		seed = world.getSeed();
		placementNoise = new SimplexNoiseGenerator(seed);
		heightNoise = new SimplexNoiseGenerator(-seed);
		blockNoise = new SimplexNoiseGenerator(~seed);
	}

	static {
		addFluid(20, "pyrotheum");
		addFluid(80, Blocks.flowing_lava);
		addFluid(40, "ic2pahoehoelava");
		addFluid(5, "iron.molten");
		addFluid(3, "gold.molten");
		addFluid(2, "ardite.molten");
		addFluid(2, "cobalt.molten");
		addFluid(4, "obsidian.molten");
		addFluid(10, "poison");
		addFluid(1, "fluiddeath");
	}

	private static void addFluid(double weight, String s) {
		Fluid f = FluidRegistry.getFluid(s);
		if (f != null && f.canBePlacedInWorld()) {
			addFluid(weight, f.getBlock());
		}
	}

	private static void addFluid(double weight, Block b) {
		blockTypes.addMapping(weight+blockTypes.maxValue(), b);
	}

	public void generate(World world, int chunkX, int chunkZ) {
		for (int i = 0; i < 16; i++) {
			for (int k = 0; k < 16; k++) {
				int dx = chunkX*16+i;
				int dz = chunkZ*16+k;
				double rx = dx/32D;
				double rz = dz/32D;
				double val = Math.abs(placementNoise.getValue(rx, rz));
				if (val <= RIVER_THRESH) {
					double h = ReikaMathLibrary.normalizeToBounds(heightNoise.getValue(rx/8, rz/8), MIN_HEIGHT, MAX_HEIGHT);
					int y = (int)h;
					if (val < RIVER_CENTER_THRESH) {
						for (int j = 0; j < 1; j++) {
							int dy = y+j;
							world.setBlock(dx, dy, dz, ChromaBlocks.STRUCTSHIELD.getBlockInstance(), BlockType.STONE.metadata, 2);
						}
						for (int j = 1; j <= 1; j++) {
							int dy = y+j;
							world.setBlock(dx, dy, dz, this.getLiquid(rx, rz), 0, 11);
						}
					}
					else {
						for (int j = 0; j < 3; j++) {
							int dy = y+j;
							world.setBlock(dx, dy, dz, ChromaBlocks.STRUCTSHIELD.getBlockInstance(), BlockType.STONE.metadata, 2);
						}
					}
				}
			}
		}
	}

	private Block getLiquid(double rx, double rz) {
		double t = ReikaMathLibrary.normalizeToBounds(blockNoise.getValue(rx, rz), 0, blockTypes.maxValue());
		return blockTypes.getForValue(t, true);
	}

}
