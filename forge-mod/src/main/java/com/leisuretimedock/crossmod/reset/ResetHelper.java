package com.leisuretimedock.crossmod.reset;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.GameData;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.ChatFormatting.BOLD;

@Slf4j
@OnlyIn(Dist.CLIENT)
public class ResetHelper {
    public static boolean clearClient(NetworkEvent.Context context) {
        CompletableFuture<Void> future = context.enqueueWork(() -> {
            log.debug("Clearing");
            Minecraft minecraft = Minecraft.getInstance();
            ServerData serverData = minecraft.getCurrentServer();
            if (minecraft.level == null) {
                GameData.revertToFrozen();
            }

            minecraft.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("ltd.mod.client.negotiating").withStyle(BOLD)));
            minecraft.setCurrentServer(serverData);
        });
        log.debug("Waiting for Clear to complete");
        try {
            future.get();
            log.debug("Clear complete, continuing reset");
            return true;
        } catch (Exception e) {
            log.error("Failed to clear client connection", e);
            Objects.requireNonNull(Minecraft.getInstance().getConnection()).onDisconnect(new TranslatableComponent("ltd.mod.client.failed.reset_connection"));
            return false;
        }
    }
}