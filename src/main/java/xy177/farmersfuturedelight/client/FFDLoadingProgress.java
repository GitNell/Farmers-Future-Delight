package xy177.farmersfuturedelight.client;

import java.awt.image.BufferedImage;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import org.apache.logging.log4j.LogManager;
import xy177.farmersfuturedelight.FarmerFutureDelight;

public final class FFDLoadingProgress {
    private static final int RADIUS = 15;
    private static final int DIAMETER = RADIUS * 2 + 1;
    private static final int CELL_SIZE = 2;
    private static final int CENTER_CELL_COUNT = 49;
    private static final ResourceLocation STAGES = new ResourceLocation(
            FarmerFutureDelight.MODID, "textures/gui/loading/stages_1_20_6.png");
    private static volatile ProgressState active;

    private FFDLoadingProgress() {
    }

    static void begin() {
        active = new ProgressState();
    }

    static void end() {
        active = null;
    }

    static void draw(int centerX, int centerY, int percentage) {
        ProgressState state = active;
        int width = DIAMETER * CELL_SIZE;
        int left = centerX - width / 2;
        int top = centerY - width / 2;
        if (state == null) {
            Gui.drawRect(left, top, left + width, top + width, 0xFF000000);
            return;
        }
        state.frame = ((MathHelper.clamp(percentage, 0, 100) + 1) * CENTER_CELL_COUNT - 1) / 100;
        int frameOffset = state.frame * DIAMETER * DIAMETER;
        for (int row = 0; row < DIAMETER; row++) {
            int rowOffset = frameOffset + row * DIAMETER;
            for (int start = 0; start < DIAMETER;) {
                int color = state.colors[rowOffset + start] | 0xFF000000;
                int end = start + 1;
                while (end < DIAMETER && (state.colors[rowOffset + end] | 0xFF000000) == color) {
                    end++;
                }
                Gui.drawRect(left + start * CELL_SIZE, top + row * CELL_SIZE,
                        left + end * CELL_SIZE, top + (row + 1) * CELL_SIZE, color);
                start = end;
            }
        }
    }

    static int textOffset(int fontHeight) {
        return DIAMETER + fontHeight + 2;
    }

    private static final class ProgressState {
        private final int[] colors = new int[(CENTER_CELL_COUNT + 1) * DIAMETER * DIAMETER];
        private int frame;

        private ProgressState() {
            try (IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(STAGES)) {
                BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
                if (image.getWidth() != DIAMETER
                        || image.getHeight() != (CENTER_CELL_COUNT + 1) * DIAMETER) {
                    throw new IOException("Invalid loading visualization dimensions");
                }
                image.getRGB(0, 0, image.getWidth(), image.getHeight(), colors, 0, DIAMETER);
            } catch (IOException | RuntimeException failure) {
                LogManager.getLogger().warn("Unable to load the world-loading visualization", failure);
            }
        }
    }
}
