package com.leisuretimedock.crossmod.client;

import com.leisuretimedock.crossmod.CrossTeleportMod;
import com.leisuretimedock.crossmod.NetworkHandler;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;
@Slf4j
@Mod.EventBusSubscriber(modid = CrossTeleportMod.MOD_ID, value = Dist.CLIENT)
public class PluginChannelClient {
    public static final ResourceLocation CHANNEL_ID = new ResourceLocation(CrossTeleportMod.MOD_ID, "channel");
    private static final String HANDLER_NAME = CrossTeleportMod.MOD_ID+":channel";

    @SubscribeEvent
    public static void onLogin(ClientPlayerNetworkEvent.LoggedInEvent event) {
        log.info("[CrossTeleportMod] 玩家登录事件触发");

        Connection connection = Objects.requireNonNull(Minecraft.getInstance().getConnection()).getConnection();
        ChannelPipeline pipeline = connection.channel().pipeline();

        log.info("[CrossTeleportMod] 当前管线内容: {}", pipeline.names());

        if (pipeline.get(HANDLER_NAME) == null) {
            pipeline.addBefore("packet_handler", HANDLER_NAME, new SimpleChannelInboundHandler<ClientboundCustomPayloadPacket>() {
                @Override
                protected void channelRead0(ChannelHandlerContext ctx, ClientboundCustomPayloadPacket packet) {
                    log.info("[CrossTeleportMod] 收到插件消息包: {}", packet.getIdentifier());

                    if (!packet.getIdentifier().equals(CHANNEL_ID)) {
                        log.warn("[CrossTeleportMod] 未识别插件消息频道: {}", packet.getIdentifier());
                        return;
                    }

                    FriendlyByteBuf buf = packet.getData();
                    try {
                        // 先读一个字符串但不使用它,出现空消息
                        buf.readUtf();

                        // 再读
                        String command = buf.readUtf();

                        log.info("[CrossTeleportMod] 收到指令: {}", command);

                        Minecraft.getInstance().execute(() -> {
                            PluginCommand.fromId(command).ifPresentOrElse(cmd -> {
                                switch (cmd) {
                                    case OVERLAY_SHOW -> {
                                        log.info("[CrossTeleportMod] 执行 OVERLAY_SHOW");
                                        OverlayRenderer.setShow(true);
                                    }
                                    case OVERLAY_HIDE -> {
                                        log.info("[CrossTeleportMod] 执行 OVERLAY_HIDE");
                                        OverlayRenderer.setShow(false);
                                    }
                                }
                            }, () -> log.error("未知指令: {}", command));
                        });

                    } catch (Exception e) {
                        log.error("[CrossTeleportMod] 处理插件消息时发生错误: {}", e.getMessage());
                    }
                }
            });

            log.info("[CrossTeleportMod] 已添加插件消息处理器: {}", HANDLER_NAME);
            NetworkHandler.sendClientReady();
        }
        else {
            log.debug("[CrossTeleportMod] 管线中已存在插件消息处理器: {}", HANDLER_NAME);
            NetworkHandler.sendClientReady();
        }
    }


    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        log.info("[CrossTeleportMod] 玩家注销事件触发");

        Connection connection = event.getConnection();
        if (connection != null) {
            ChannelPipeline pipeline = connection.channel().pipeline();

            log.info("[CrossTeleportMod] 当前管线内容: {}", pipeline.names());

            if (pipeline.get(HANDLER_NAME) != null) {
                pipeline.remove(HANDLER_NAME);
                log.info("[CrossTeleportMod] 成功移除插件消息处理器: {}", HANDLER_NAME);
            } else {
                log.warn("[CrossTeleportMod] 未找到插件消息处理器: {}", HANDLER_NAME);
            }
        } else {
            log.warn("[CrossTeleportMod] 玩家连接为空，无法移除插件处理器");
        }
    }

    @SubscribeEvent
    public static void onRegisterCommand(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("goto")
                        .then(Commands.argument("server", StringArgumentType.string())
                                .executes(ctx -> {
                                    String server = StringArgumentType.getString(ctx, "server");
                                    NetworkHandler.sendTeleportMessage(server);
                                    ctx.getSource().sendSuccess(
                                            new TextComponent("请求传送到 " + server), false);
                                    return 1;
                                }))
        );
    }
}
