package com.leisuretimedock.crossplugin.manager;

import com.leisuretimedock.crossplugin.listener.PluginMessageListener;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.leisuretimedock.crossplugin.messages.I18nKeyEnum;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public class OverlayManager {


    public static void showOverlay(Player player) {
        sendRawCommand(player, "overlay:show");
    }

    public static void hideOverlay(Player player) {
        sendRawCommand(player, "overlay:hide");
    }

    private static void sendRawCommand(Player player, String command) {
        if (!player.isActive()) return;
        try (var out = new java.io.ByteArrayOutputStream();
             var data = new java.io.DataOutputStream(out)) {

            data.writeUTF(command);  // 这里写入字符串（包括长度前缀）
            data.flush();

            player.sendPluginMessage(
                    PluginMessageListener.CHANNEL_ID,
                    out.toByteArray()
            );
        } catch (Exception e) {
            // 处理异常，日志等等
            throw new RuntimeException(e);
        }

    }
    //TODO : 客户端模组未来待实现
    public static void sendServerList(Player player, Iterable<ServerManager.ServerInfo> servers) {
        try (var out = new java.io.ByteArrayOutputStream();
             var data = new java.io.DataOutputStream(out)) {

            data.writeUTF("gui:server_list");

            List<ServerManager.ServerInfo> list = new ArrayList<>();
            servers.forEach(list::add);

            data.writeInt(list.size());
            for (var server : list) {
                data.writeUTF(server.id());     // 名称
                data.writeUTF(server.motd());   // MOTD
                data.writeUTF(server.id());     // 目标 ID
            }

            player.sendPluginMessage(
                    PluginMessageListener.CHANNEL_ID,
                    out.toByteArray()
            );

        } catch (Exception e) {
            player.sendMessage(I18n.translatable(I18nKeyEnum.FAILED_TO_SEND_SERVER_LIST, NamedTextColor.RED));
        }
    }
}
