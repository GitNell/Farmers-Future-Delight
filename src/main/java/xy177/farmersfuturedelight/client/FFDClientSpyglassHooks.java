package xy177.farmersfuturedelight.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import xy177.farmersfuturedelight.common.registry.FFDItems;

public final class FFDClientSpyglassHooks {
    private FFDClientSpyglassHooks() {
    }

    public static boolean renderHeldToEye(RenderLivingBase<?> renderer, EntityLivingBase entity,
                                          ItemStack stack, EnumHandSide side) {
        if (!(entity instanceof EntityPlayer) || stack.isEmpty() || stack.getItem() != FFDItems.SPYGLASS
                || !entity.isHandActive() || entity.getItemInUseCount() <= 0
                || entity.getActiveItemStack().getItem() != FFDItems.SPYGLASS
                || entity.getSwingProgress(Minecraft.getMinecraft().getRenderPartialTicks()) >= 1.0E-5F
                || !(renderer.getMainModel() instanceof ModelBiped)) {
            return false;
        }
        EnumHandSide activeSide = entity.getActiveHand() == EnumHand.MAIN_HAND
                ? entity.getPrimaryHand() : entity.getPrimaryHand().opposite();
        if (side != activeSide) {
            return false;
        }
        ModelRenderer head = ((ModelBiped) renderer.getMainModel()).bipedHead;
        float pitch = head.rotateAngleX;
        GlStateManager.pushMatrix();
        try {
            if (entity.isSneaking()) {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }
            head.rotateAngleX = MathHelper.clamp(pitch, -0.5235988F, 1.5707964F);
            head.postRender(0.0625F);
            GlStateManager.translate(0.0F, -0.25F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(0.625F, -0.625F, -0.625F);
            GlStateManager.translate((side == EnumHandSide.LEFT ? -2.5F : 2.5F) / 16.0F,
                    -0.0625F, 0.0F);
            Minecraft.getMinecraft().getItemRenderer().renderItemSide(entity, stack,
                    ItemCameraTransforms.TransformType.HEAD, side == EnumHandSide.LEFT);
            return true;
        } finally {
            head.rotateAngleX = pitch;
            GlStateManager.popMatrix();
        }
    }
}
