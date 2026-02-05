package com.leisuretimedock.crossmod.reset;

import com.leisuretimedock.crossmod.client.gui.GenericIceMessageScreen;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.GameData;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.ChatFormatting.BOLD;

@Slf4j
@OnlyIn(Dist.CLIENT)
public class ResetHelper {
    @SuppressWarnings("UnstableApiUsage")
    public static boolean clearClient(NetworkEvent.Context context) {
        CompletableFuture<Void> future = context.enqueueWork(() -> {
            log.debug("Clearing");
            Minecraft minecraft = Minecraft.getInstance();
            Pack serverPack = Minecraft.getInstance().getDownloadedPackSource().serverPack;
            if (minecraft.level == null) {
                GameData.revertToFrozen();
            }
            Minecraft.getInstance().getDownloadedPackSource().serverPack = null;
            minecraft.clearLevel(new GenericIceMessageScreen(Component.translatable("ltd.mod.client.negotiating").withStyle(BOLD)));
            try {
                context.getNetworkManager().channel().pipeline().remove("forge:forge_fixes");
            } catch (NoSuchElementException ignored) {
            }
            try {
                context.getNetworkManager().channel().pipeline().remove("forge:vanilla_filter");
            } catch (NoSuchElementException ignored) {
            }
            Minecraft.getInstance().getDownloadedPackSource().serverPack = serverPack;
        });
        log.debug("Waiting for Clear to complete");
        try {
            future.get();
            log.debug("Clear complete, continuing reset");
            return true;
        } catch (Exception e) {
            log.error("Failed to clear client connection", e);
            Objects.requireNonNull(Minecraft.getInstance().getConnection()).onDisconnect(Component.translatable("ltd.mod.client.failed.reset_connection"));
            return false;
        }
    }
}