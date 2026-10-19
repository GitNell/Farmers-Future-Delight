package xy177.farmersfuturedelight.common.worldgen;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import xy177.farmersfuturedelight.common.FFDConfig;
import xy177.farmersfuturedelight.common.registry.FFDItems;

public final class WorldGenLegacyAmethystGeodes {
    private long seed;
    private WorldGenAmethystGeodes geodes;

    public void generate(World world, int chunkX, int chunkZ) {
        if (!isNaturalWorld(world.getWorldType()) || !FFDItems.isAmethystEnabled()) {
            return;
        }
        if (geodes == null || seed != world.getSeed()) {
            seed = world.getSeed();
            geodes = new WorldGenAmethystGeodes(seed);
        }
        Chunk chunk = world.getChunkFromChunkCoords(chunkX, chunkZ);
        geodes.generateChunk(chunkX, chunkZ, new WorldGenAmethystGeodes.Access() {
            private final int[] surfaceHeights = new int[256];

            @Override
            public IBlockState getState(BlockPos pos) {
                if (!isTargetChunk(pos)) {
                    return Blocks.STONE.getDefaultState();
                }
                int localX = pos.getX() & 15;
                int localZ = pos.getZ() & 15;
                int y = pos.getY();
                if (y < 16 || y >= 256) {
                    return Blocks.BEDROCK.getDefaultState();
                }
                int column = (localX << 4) | localZ;
                if (surfaceHeights[column] == 0) {
                    for (int surfaceY = 255; surfaceY > 0; surfaceY--) {
                        if (chunk.getBlockState(new BlockPos(pos.getX(), surfaceY, pos.getZ()))
                                .getMaterial().isSolid()) {
                            surfaceHeights[column] = surfaceY;
                            break;
                        }
                    }
                }
                if (y > surfaceHeights[column] - 5) {
                    return Blocks.BEDROCK.getDefaultState();
                }
                IBlockState state = chunk.getBlockState(pos);
                return state.getMaterial().isSolid() ? state : Blocks.BEDROCK.getDefaultState();
            }

            @Override
            public boolean isTargetChunk(BlockPos pos) {
                return pos.getX() >> 4 == chunkX && pos.getZ() >> 4 == chunkZ;
            }

            @Override
            public void setState(BlockPos pos, IBlockState state) {
                world.setBlockState(pos, state, 2 | 16);
            }

            @Override
            public void markFluidUpdate(BlockPos pos) {
            }
        }, FFDConfig.amethystGeodeLegacyMinY, FFDConfig.amethystGeodeLegacyMaxY, false);
    }

    private static boolean isNaturalWorld(WorldType type) {
        return type == WorldType.DEFAULT || type == WorldType.DEFAULT_1_1
                || type == WorldType.LARGE_BIOMES || type == WorldType.AMPLIFIED
                || type == WorldType.CUSTOMIZED;
    }
}
