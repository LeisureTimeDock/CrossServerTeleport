package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.network.NetworkHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record GotoServerPayload(String serverName) {
    public static void encode(@NotNull GotoServerPayload payload, @NotNull FriendlyByteBuf buf) {
        buf.writeUtf(payload.serverName);
    }
    @Contract("_ -> new")
    public static @NotNull GotoServerPayload decode(@NotNull FriendlyByteBuf buf) {
        return new GotoServerPayload(buf.readUtf());
    }
    public static void handle(@NotNull GotoServerPayload msg, @NotNull Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();
        context.enqueueWork(() -> NetworkHandler.sendTeleportRequest(msg.serverName));
        context.setPacketHandled(true);
    }
}
