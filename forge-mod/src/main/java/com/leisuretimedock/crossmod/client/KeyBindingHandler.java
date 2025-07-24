package com.leisuretimedock.crossmod.client;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.leisuretimedock.crossmod.client.gui.CrossServerGui;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;
@Mod.EventBusSubscriber(modid = CrossTeleportMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindingHandler {
    public static final KeyMapping OPEN_GUI_KEY = new KeyMapping("ltd.mod.client.name.trans_server", GLFW.GLFW_KEY_HOME, "ltd.mod.client.key");

    @SubscribeEvent
    public static void onRegisterKey(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientRegistry.registerKeyBinding(OPEN_GUI_KEY));
    }

    @Mod.EventBusSubscriber(modid = CrossTeleportMod.MOD_ID, value = Dist.CLIENT)
    public static class KeyHandler {
        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (event.phase == TickEvent.Phase.END && OPEN_GUI_KEY.consumeClick()) {
                Minecraft.getInstance().setScreen(new CrossServerGui());
            }
        }
    }
}
