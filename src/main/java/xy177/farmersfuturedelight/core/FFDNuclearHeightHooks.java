package xy177.farmersfuturedelight.core;

import java.nio.ByteBuffer;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

public final class FFDNuclearHeightHooks {
    private FFDNuclearHeightHooks() {
    }

    public static int ic2ExplosionHeight(int original, World world) {
        return FFDHeightHooks.minY(world) < 0
                ? FFDHeightHooks.maxYExclusive(world) : original;
    }

    public static boolean hbmTraceOutsideHeight(int y, World world) {
        return y < FFDHeightHooks.minY(world) || y >= FFDHeightHooks.maxYExclusive(world);
    }

    public static ExtendedBlockStorage hbmTraceStorage(ExtendedBlockStorage[] storage,
            int sectionY, World world) {
        int index = FFDHeightHooks.storageIndexForSectionY(sectionY, world);
        return storage != null && index >= 0 && index < storage.length ? storage[index] : null;
    }

    public static int radiationSectionCount(Chunk chunk) {
        return FFDHeightHooks.sectionCount(chunk.getWorld());
    }

    public static int radiationIndex(int y, Chunk chunk) {
        World world = chunk.getWorld();
        if (y < FFDHeightHooks.minY(world) || y >= FFDHeightHooks.maxYExclusive(world)) {
            return -1;
        }
        if (y >= 0 && y < 256) {
            return y >> 4;
        }
        return y < 0 ? 16 + ((y - FFDHeightHooks.MIN_Y) >> 4)
                : 20 + ((y - 256) >> 4);
    }

    public static byte readRadiationSectionMarker(ByteBuffer data) {
        return data.hasRemaining() ? data.get() : 0;
    }

    public static int explosionYIndex(int y) {
        return y < 256 ? 255 - y
                : 256 - FFDHeightHooks.MIN_Y + FFDHeightHooks.MAX_Y_EXCLUSIVE - 1 - y;
    }

    public static int explosionYFromIndex(int index) {
        return index < 256 - FFDHeightHooks.MIN_Y ? 255 - index
                : 256 - FFDHeightHooks.MIN_Y + FFDHeightHooks.MAX_Y_EXCLUSIVE - 1 - index;
    }
}
