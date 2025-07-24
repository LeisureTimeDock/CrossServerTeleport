package com.leisuretimedock.crossmod;

import com.leisuretimedock.crossmod.command.PingCommand;
import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.PingRequestManager;
import com.leisuretimedock.crossmod.reset.ClientResetManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkConstants;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

@Mod(CrossTeleportMod.MOD_ID)
public class CrossTeleportMod {
    public static final String MOD_ID ="ltdcrossteleport";


    public CrossTeleportMod() {
        // 注册生命周期事件
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        if(!FMLEnvironment.dist.isDedicatedServer()) modEventBus.addListener(ClientResetManager::init);
        NetworkHandler.register();
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommonEvents {
        @Nullable
        public static MinecraftServer server;
        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            PingCommand.register(event.getDispatcher());
        }
        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PingRequestManager.monitor(player);
            }
        }
        @SubscribeEvent
        public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event.getEntity() instanceof ServerPlayer player) {
                PingRequestManager.unmonitor(player);
            }
        }
        @SubscribeEvent
        public static void onServerStart(ServerStartedEvent event) {
            server = event.getServer();
        }
        @SubscribeEvent
        public static void onServerStop(ServerStoppedEvent event) {
            server = null;
        }
        public static ServerPlayer getPlayerByUUID(UUID uuid) {
            Objects.requireNonNull(server, "server is null");
            return server.getPlayerList().getPlayer(uuid);
        }
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ClientEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
    @Mod.EventBusSubscriber(modid = MOD_ID, value = Dist.DEDICATED_SERVER, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ServerEvent {
        @SubscribeEvent
        public static void onServerSetup(FMLDedicatedServerSetupEvent event) {

        }

    }
}
