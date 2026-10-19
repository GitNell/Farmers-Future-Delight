package xy177.farmersfuturedelight.common;

import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import xy177.farmersfuturedelight.FarmerFutureDelight;
import xy177.farmersfuturedelight.core.FFDGameplayHooks;

@Mod.EventBusSubscriber(modid = FarmerFutureDelight.MODID)
public final class FFDWaterSinkingEvents {
    private static final Map<PathNavigateGround, Boolean> SWIMMING_NAVIGATORS = new WeakHashMap<>();

    private FFDWaterSinkingEvents() {
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !(entity instanceof EntityLiving)) {
            return;
        }
        EntityLiving living = (EntityLiving) entity;
        if (!(living.getNavigator() instanceof PathNavigateGround)) {
            return;
        }
        PathNavigateGround navigator = (PathNavigateGround) living.getNavigator();
        if (FFDGameplayHooks.shouldSinkInWater(entity)) {
            if (!SWIMMING_NAVIGATORS.containsKey(navigator)) {
                SWIMMING_NAVIGATORS.put(navigator, navigator.getCanSwim());
                navigator.clearPath();
            }
            navigator.setCanSwim(false);
        } else {
            Boolean canSwim = SWIMMING_NAVIGATORS.remove(navigator);
            if (canSwim != null) {
                navigator.setCanSwim(canSwim);
                navigator.clearPath();
            }
        }
    }
}
