package com.leisuretimedock.crossmod.client.overlay;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.OverlayRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = CrossTeleportMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OverlayRenderer {

    @SubscribeEvent
    public static void onRender(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            OverlayRegistry.registerOverlayTop(
                    "cross_server_tip",
                    CrossServerTipOverLay.INSTANCE
            );
            OverlayRegistry.registerOverlayTop(
                    "ping_debug",
                    PingOverlayManager.INSTANCE
            );
        });
    }

}
