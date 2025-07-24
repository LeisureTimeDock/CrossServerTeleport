package com.leisuretimedock.crossmod.network.toClient;

import com.leisuretimedock.crossmod.client.ClientPingHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public class PingResultPacket {
    private final long timestamp;
    private final Map<UUID, Long> pingResults; // UUID -> ping值
    private final Map<UUID, Double> averageLatencies; // UUID -> 平均时延

    public PingResultPacket(Map<UUID, Long> pingResults, Map<UUID, Double> averageLatencies) {
        this.timestamp = System.currentTimeMillis();
        this.pingResults = new HashMap<>(pingResults);
        this.averageLatencies = new HashMap<>(averageLatencies);
    }
    public PingResultPacket(FriendlyByteBuf buf) {
        this.timestamp = System.currentTimeMillis();
        int size = buf.readVarInt();
        this.pingResults = new HashMap<>(size);
        this.averageLatencies = new HashMap<>(size);
        for (int i = 0; i < size; i++) {
            UUID uuid = buf.readUUID();
            long ping = buf.readLong();
            double avgLatency = buf.readDouble();
            pingResults.put(uuid, ping);
            averageLatencies.put(uuid, avgLatency);
        }
    }
    public static PingResultPacket decode(FriendlyByteBuf buf) {
        return new PingResultPacket(buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(pingResults.size());
        pingResults.forEach((uuid, ping) -> {
            buf.writeUUID(uuid);
            buf.writeLong(ping);
            buf.writeDouble(averageLatencies.getOrDefault(uuid, 0.0));
        });
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 检查数据时效性(5秒内有效)
            if (System.currentTimeMillis() - timestamp < 5000) {
                ClientPingHandler.handlePingResults(pingResults, averageLatencies);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}