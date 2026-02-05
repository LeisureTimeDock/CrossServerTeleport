package com.leisuretimedock.crossmod.network;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.leisuretimedock.crossmod.network.toClient.*;
import com.leisuretimedock.crossmod.network.toServer.PongMessagePayload;
import com.leisuretimedock.crossmod.network.toServer.SyncCommonConfigRequestPacket;
import io.netty.buffer.Unpooled;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * NetworkHandler
 * 目前只用 plugin message 方式进行通信。
 */
@Slf4j
public class NetworkHandler {

    // 自定义插件消息通道标识
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "channel");
    private static final String PROTOCOL_VERSION = "1";
    public static SimpleChannel CHANNEL;
    private static int messageId = 0;

    /**
     * 注册网络通道和消息处理器
     */
    public static void register() {
        // 支持双端注册，以便服务器端也能处理相关命令
        CHANNEL = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(CrossTeleportMod.MOD_ID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        CHANNEL.registerMessage(
                messageId++,
                PongMessagePayload.class,
                PongMessagePayload::encode,
                PongMessagePayload::decode,
                PongMessagePayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_SERVER)
        );
        CHANNEL.registerMessage(
                messageId++,
                PingMessagePayload.class,
                PingMessagePayload::encode,
                PingMessagePayload::decode,
                PingMessagePayload::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );
        CHANNEL.registerMessage(
                messageId++,
                PingResultPacket.class,
                PingResultPacket::encode,
                PingResultPacket::decode,
                PingResultPacket::handle
        );
        CHANNEL.registerMessage(
                messageId++,
                PingStatsPacket.class,
                PingStatsPacket::encode,
                PingStatsPacket::decode,
                PingStatsPacket::handle
        );
        CHANNEL.messageBuilder(SyncCommonConfigPacket.class, messageId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(SyncCommonConfigPacket::decode)
                .encoder(SyncCommonConfigPacket::encode)
                .consumerNetworkThread(SyncCommonConfigPacket::handle)
                .add();
        CHANNEL.messageBuilder(SyncCommonConfigRequestPacket.class, messageId++, NetworkDirection.PLAY_TO_SERVER)
                .decoder(SyncCommonConfigRequestPacket::decode)
                .encoder(SyncCommonConfigRequestPacket::encode)
                .consumerNetworkThread(SyncCommonConfigRequestPacket::handle)
                .add();
        CHANNEL.messageBuilder(CommonConfigHashInformPacket.class, messageId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(CommonConfigHashInformPacket::decode)
                .encoder(CommonConfigHashInformPacket::encode)
                .consumerNetworkThread(CommonConfigHashInformPacket::handle)
                .add();
        CHANNEL.messageBuilder(GotoServerPayload.class, messageId++, NetworkDirection.PLAY_TO_CLIENT)
                .decoder(GotoServerPayload::decode)
                .encoder(GotoServerPayload::encode)
                .consumerMainThread(GotoServerPayload::handle)
                .add();
    }
    // 新增发送报告方法
    public static void sendPingReport(ServerPlayer player,
                                      Map<UUID, Long> pingResults,
                                      Map<UUID, Double> averageLatencies,
                                      PingRequestManager.PingStats globalStats) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PingResultPacket(pingResults, averageLatencies));

        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PingStatsPacket(globalStats));
    }


    public static void sendPingResults(ServerPlayer player, @NotNull Map<UUID, Long> results) {
        // 创建平均时延映射
        Map<UUID, Double> averageLatencies = new HashMap<>();

        // 为每个结果获取平均时延
        results.forEach((uuid, ping) -> {
            double avgLatency = PingRequestManager.getAverageLatency(uuid);
            averageLatencies.put(uuid, avgLatency);
        });

        // 发送包含ping值和平均时延的数据包
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PingResultPacket(results, averageLatencies));
    }


    public static void sendPingStats(ServerPlayer player, PingRequestManager.PingStats stats) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new PingStatsPacket(stats));
    }

    /**
     * 发送Ping请求
     * @param player 服务器玩家
     */
    public static void sendPingRequest(ServerPlayer player, UUID requestId) {
        try {
            CHANNEL.sendTo(new PingMessagePayload(requestId),
                    player.connection.connection,
                    NetworkDirection.PLAY_TO_CLIENT);
        } catch (Exception e) {
            log.error("发送ping请求失败", e);
        }
    }


    /**
     * 发送自定义插件消息
     * @param subChannel 子通道标识
     * @param payload   负载数据（字节数组）
     */
    public static void sendPluginMessage(ResourceLocation subChannel, byte[] payload) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer(payload.length));
        buf.writeBytes(payload);

        // 获取当前连接并发送自定义负载包
        Objects.requireNonNull(Minecraft.getInstance().getConnection())
                .send(new ServerboundCustomPayloadPacket(subChannel, buf));
    }

    /**
     * 发送客户端已准备好消息（示例方法，调用具体实现）
     */
    public static void sendClientReady() {
        PluginMessageListener.sendClientReady();
    }

    /**
     * 发送传送请求到代理服务器
     * @param serverName 目标服务器名
     */
    public static void sendTeleportRequest(String serverName) {
        PluginMessageListener.sendTeleport(serverName);
    }
    /**
     * Send to player.
     *
     * @param <MSG>   the type parameter
     * @param message the message
     * @param player  the player
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player){
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    /**
     * Send to all player.
     *
     * @param <MSG>   the type parameter
     * @param message the message
     */
    public static <MSG> void sendToAllPlayer(MSG message){
        CHANNEL.send(PacketDistributor.ALL.noArg(), message);
    }

    /**
     * Send to player.
     *
     * @param <MSG>             the type parameter
     * @param <T>               the type parameter
     * @param message           the message
     * @param entity            the entity
     * @param packetDistributor the packet distributor
     */
    public static <MSG, T> void sendToPlayer(MSG message, T entity, PacketDistributor<T> packetDistributor){
        CHANNEL.send(packetDistributor.with(() -> entity), message);
    }
}