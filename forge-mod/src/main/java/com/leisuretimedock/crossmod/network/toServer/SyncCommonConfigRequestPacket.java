package com.leisuretimedock.crossmod.network.toServer;

import com.leisuretimedock.crossmod.config.CrossServerConfigManager;
import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.toClient.SyncCommonConfigPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * The type Sync common config request packet.
 */
public record SyncCommonConfigRequestPacket(int hash) {
    /**
     * Encode.
     *
     * @param msg the msg
     * @param buf the buf
     */
    public static void encode(SyncCommonConfigRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.hash);
    }


    /**
     * Decode sync common config request packet.
     *
     * @param buf the buf
     * @return the sync common config request packet
     */
    public static SyncCommonConfigRequestPacket decode(FriendlyByteBuf buf) {
        return new SyncCommonConfigRequestPacket(buf.readInt());
    }

    /**
     * Handle.
     *
     * @param msg the msg
     * @param ctx the ctx
     */
    public static void handle(SyncCommonConfigRequestPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.hash != CrossServerConfigManager.INSTANCE.cacheHash) {
                NetworkHandler.sendToPlayer(new SyncCommonConfigPacket(CrossServerConfigManager.INSTANCE.serializeToNBT(), CrossServerConfigManager.INSTANCE.calculateConfigHash()), ctx.get().getSender());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
