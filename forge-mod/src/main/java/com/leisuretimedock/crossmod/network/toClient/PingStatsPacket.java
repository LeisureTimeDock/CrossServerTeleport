package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.client.ClientPingHandler;
import com.leisuretimedock.crossmod.network.PingRequestManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PingStatsPacket {

    private final PingRequestManager.PingStats stats;

    public PingStatsPacket(PingRequestManager.PingStats stats) {
        this.stats = stats;
    }

    public static PingStatsPacket decode(FriendlyByteBuf buf) {
        return new PingStatsPacket(
                new PingRequestManager.PingStats(
                    buf.readDouble(),
                    buf.readLong(),
                    buf.readLong(),
                    buf.readInt(),
                    buf.readDouble(),
                    buf.readDouble()
                )
        );
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(stats.average());
        buf.writeLong(stats.max());
        buf.writeLong(stats.min());
        buf.writeInt(stats.sampleCount());
        buf.writeDouble(stats.averageLatency());
        buf.writeDouble(stats.packetLossRate());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ClientPingHandler.handlePingStats(stats);
        });
        ctx.get().setPacketHandled(true);
    }
}