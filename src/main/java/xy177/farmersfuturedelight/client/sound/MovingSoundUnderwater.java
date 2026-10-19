package xy177.farmersfuturedelight.client.sound;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.MovingSound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;

public final class MovingSoundUnderwater extends MovingSound {
    private final EntityPlayerSP player;
    private int fade;

    public MovingSoundUnderwater(EntityPlayerSP player, SoundEvent sound, boolean looping) {
        super(sound, SoundCategory.AMBIENT);
        this.player = player;
        repeat = looping;
        repeatDelay = 0;
        attenuationType = ISound.AttenuationType.NONE;
        volume = looping ? 1.0F / 40.0F : 1.0F;
    }

    @Override
    public void update() {
        Minecraft minecraft = Minecraft.getMinecraft();
        if (player.isDead || minecraft.player != player || minecraft.world != player.world
                || repeat && fade < 0) {
            donePlaying = true;
            return;
        }
        boolean underwater = player.isInsideOfMaterial(Material.WATER);
        if (!repeat) {
            donePlaying = !underwater;
            return;
        }
        fade = Math.min(40, underwater ? fade + 1 : fade - 2);
        volume = Math.max(0.0F, Math.min(fade / 40.0F, 1.0F));
    }
}
