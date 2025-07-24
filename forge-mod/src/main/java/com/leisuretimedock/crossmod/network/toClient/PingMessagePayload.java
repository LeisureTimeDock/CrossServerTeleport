package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.network.toServer.PongMessagePayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import static com.leisuretimedock.crossmod.network.NetworkHandler.CHANNEL;

//Server -> Client
public record PingMessagePayload(UUID requestId) {
    public static void encode(PingMessagePayload payload, FriendlyByteBuf buf) {
        buf.writeLong(payload.requestId().getMostSignificantBits());
        buf.writeLong(payload.requestId().getLeastSignificantBits());
    }

    public static PingMessagePayload decode(FriendlyByteBuf buf) {
        long mostSignificantBits = buf.readLong();
        long leastSignificantBits = buf.readLong();
        return new PingMessagePayload(new UUID(mostSignificantBits, leastSignificantBits));
    }
    //客户端处理
    public static void handle(PingMessagePayload msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // 客户端收到ping请求，立即返回pong
            CHANNEL.sendToServer(new PongMessagePayload(msg.requestId));
        }));
        ctx.get().setPacketHandled(true);
    }
}
