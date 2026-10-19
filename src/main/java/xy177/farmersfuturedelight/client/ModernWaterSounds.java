package xy177.farmersfuturedelight.client;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import xy177.farmersfuturedelight.FarmerFutureDelight;
import xy177.farmersfuturedelight.client.sound.MovingSoundUnderwater;
import xy177.farmersfuturedelight.common.registry.FFDSounds;

@Mod.EventBusSubscriber(modid = FarmerFutureDelight.MODID, value = Side.CLIENT)
public final class ModernWaterSounds {
    private static EntityPlayerSP trackedPlayer;
    private static boolean wasUnderwater;
    private static MovingSoundUnderwater loop;

    private ModernWaterSounds() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        EntityPlayerSP player = minecraft.player;
        if (minecraft.world == null || player == null) {
            reset(minecraft);
            return;
        }
        if (minecraft.isGamePaused()) {
            return;
        }
        if (trackedPlayer != player) {
            reset(minecraft);
            trackedPlayer = player;
        }
        boolean underwater = player.isInsideOfMaterial(Material.WATER);
        if (!player.isSpectator()) {
            if (underwater && !wasUnderwater) {
                minecraft.world.playSound(player.posX, player.posY, player.posZ,
                        FFDSounds.UNDERWATER_ENTER, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
                if (loop != null) {
                    minecraft.getSoundHandler().stopSound(loop);
                }
                loop = new MovingSoundUnderwater(player, FFDSounds.UNDERWATER_LOOP, true);
                minecraft.getSoundHandler().playSound(loop);
            } else if (!underwater && wasUnderwater) {
                minecraft.world.playSound(player.posX, player.posY, player.posZ,
                        FFDSounds.UNDERWATER_EXIT, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
            }
        }
        wasUnderwater = underwater;
        if (underwater) {
            float random = minecraft.world.rand.nextFloat();
            SoundEvent addition = random < 0.0001F ? FFDSounds.UNDERWATER_ADDITIONS_ULTRA_RARE
                    : random < 0.001F ? FFDSounds.UNDERWATER_ADDITIONS_RARE
                    : random < 0.01F ? FFDSounds.UNDERWATER_ADDITIONS : null;
            if (addition != null) {
                minecraft.getSoundHandler().playSound(new MovingSoundUnderwater(player, addition, false));
            }
        }
    }

    private static void reset(Minecraft minecraft) {
        if (loop != null) {
            minecraft.getSoundHandler().stopSound(loop);
        }
        loop = null;
        trackedPlayer = null;
        wasUnderwater = false;
    }
}
