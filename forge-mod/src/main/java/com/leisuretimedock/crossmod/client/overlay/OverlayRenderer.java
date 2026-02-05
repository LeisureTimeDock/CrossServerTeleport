package com.leisuretimedock.crossmod.client.overlay;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CrossTeleportMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OverlayRenderer {

    @SubscribeEvent
    public static void onRender(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("cross_server_tip", CrossServerTipOverLay.INSTANCE);
        event.registerAboveAll(
                "ping_debug",
                PingOverlayManager.INSTANCE
        );
    }

}
