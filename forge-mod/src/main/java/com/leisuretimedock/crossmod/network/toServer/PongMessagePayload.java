package com.leisuretimedock.crossmod.network.toServer;

import com.leisuretimedock.crossmod.network.PingRequestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

//Server
public record PongMessagePayload(UUID requestId) {
    public static void encode(PongMessagePayload payload, FriendlyByteBuf buf) {
        buf.writeLong(payload.requestId().getMostSignificantBits());
        buf.writeLong(payload.requestId().getLeastSignificantBits());
    }

    public static PongMessagePayload decode(FriendlyByteBuf buf) {
        long mostSignificantBits = buf.readLong();
        long leastSignificantBits = buf.readLong();
        return new PongMessagePayload(new UUID(mostSignificantBits, leastSignificantBits));
    }
    //服务器处理
    public static void handle(PongMessagePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PingRequestManager.complete(ctx.get().getSender(),msg.requestId);
        });
        ctx.get().setPacketHandled(true);
    }
}
