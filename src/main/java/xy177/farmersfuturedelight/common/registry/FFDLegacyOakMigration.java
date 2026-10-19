package xy177.farmersfuturedelight.common.registry;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.FarmerFutureDelight;
import xy177.farmersfuturedelight.api.WaterloggedBlockApi;

@Mod.EventBusSubscriber(modid = FarmerFutureDelight.MODID)
public final class FFDLegacyOakMigration {
    private FFDLegacyOakMigration() {
    }

    @SubscribeEvent
    public static void load(ChunkDataEvent.Load event) {
        if (event.getWorld().isRemote) {
            return;
        }
        Chunk chunk = event.getChunk();
        NBTTagList sections = event.getData().getCompoundTag("Level").getTagList("Sections", 10);
        int buttonId = Block.getIdFromBlock(FFDBlocks.OVERWORLD_BUTTONS[0]);
        int plateId = Block.getIdFromBlock(FFDBlocks.OVERWORLD_PRESSURE_PLATES[0]);
        int doorId = Block.getIdFromBlock(FFDBlocks.OVERWORLD_TRAPDOORS[0]);
        ResourceLocation carrierName = new ResourceLocation(FarmerFutureDelight.MODID,
                "waterlogged_state/" + FarmerFutureDelight.MODID + "/oak_trapdoor");
        int carrierId = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.containsKey(carrierName)
                ? Block.getIdFromBlock(net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS
                        .getValue(carrierName)) : -1;
        for (int sectionIndex = 0; sectionIndex < sections.tagCount(); sectionIndex++) {
            NBTTagCompound section = sections.getCompoundTagAt(sectionIndex);
            byte[] ids = section.getByteArray("Blocks");
            byte[] high = section.getByteArray("Add");
            int sectionY = section.getByte("Y");
            ExtendedBlockStorage storage = null;
            for (ExtendedBlockStorage candidate : chunk.getBlockStorageArray()) {
                if (candidate != null && candidate.getYLocation() == sectionY * 16) {
                    storage = candidate;
                    break;
                }
            }
            if (storage == null) {
                continue;
            }
            for (int index = 0; index < Math.min(4096, ids.length); index++) {
                int blockId = ids[index] & 255;
                if (high.length > index / 2) {
                    blockId |= ((high[index / 2] >> ((index & 1) * 4)) & 15) << 8;
                }
                if (blockId != buttonId && blockId != plateId && blockId != doorId && blockId != carrierId) {
                    continue;
                }
                int localX = index & 15;
                int localY = index >> 8;
                int localZ = index >> 4 & 15;
                IBlockState previous = storage.get(localX, localY, localZ);
                Block target = previous.getBlock() == FFDBlocks.OVERWORLD_BUTTONS[0]
                        ? Blocks.WOODEN_BUTTON : previous.getBlock() == FFDBlocks.OVERWORLD_PRESSURE_PLATES[0]
                        ? Blocks.WOODEN_PRESSURE_PLATE : previous.getBlock() == FFDBlocks.OVERWORLD_TRAPDOORS[0]
                        ? Blocks.TRAPDOOR : null;
                if (target == null) {
                    continue;
                }
                IBlockState replacement = target.getStateFromMeta(previous.getBlock().getMetaFromState(previous));
                if (previous.getPropertyKeys().contains(WaterloggedBlockApi.UNIVERSAL_WATERLOGGED)
                        && previous.getValue(WaterloggedBlockApi.UNIVERSAL_WATERLOGGED)
                        && replacement.getPropertyKeys().contains(WaterloggedBlockApi.UNIVERSAL_WATERLOGGED)) {
                    replacement = replacement.withProperty(WaterloggedBlockApi.UNIVERSAL_WATERLOGGED, true);
                }
                storage.set(localX, localY, localZ, replacement);
                chunk.markDirty();
            }
        }
    }
}
