package com.leisuretimedock.crossmod.reset;

import lombok.extern.slf4j.Slf4j;
import net.minecraft.network.Connection;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class ClientResetManager {
    public static final Field handshakeField;
    public static final Constructor<NetworkEvent.Context> contextConstructor;
    public static AtomicBoolean isNegotiating = new AtomicBoolean(false);
    public static SimpleChannel handshakeChannel;

    public static void init(FMLCommonSetupEvent event) {

        event.enqueueWork(() -> {
            if (handshakeField == null) {
                log.error( "Failed to find FML's handshake channel. Disabling mod.");
                return;
            }
            if (contextConstructor == null) {
                log.error("Failed to find FML's network event context constructor. Disabling mod.");
                return;
            }
            try {
                Object handshake = handshakeField.get(null);
                if (handshake instanceof SimpleChannel) {
                    handshakeChannel = (SimpleChannel)handshake;
                    log.info("Registering forge reset packet.");
                    handshakeChannel.messageBuilder(ResetPacket.class, 98)
                            .loginIndex(ResetPacket::getLoginIndex, ResetPacket::setLoginIndex)
                            .decoder(ResetPacket::decode)
                            .encoder(ResetPacket::encode)
                            .consumer(HandshakeHandler.biConsumerFor(ResetPacket::handler))
                            .add();
                    log.info( "Registered forge reset packet successfully.");
                }
            }
            catch (Exception e) {
                log.error("Caught exception when attempting to utilize FML's handshake. Disabling mod. Exception: {}", e.getMessage());
            }
        });
    }
    private static Field fetchHandshakeChannel() {
        try {
            return ObfuscationReflectionHelper.findField(NetworkConstants.class, "handshakeChannel");
        }
        catch (Exception e) {
            log.error("Exception occurred while accessing handshakeChannel: {}", e.getMessage(), e);
            return null;
        }
    }

    private static Constructor<NetworkEvent.Context> fetchNetworkEventContext() {
        try {
            return ObfuscationReflectionHelper.findConstructor(NetworkEvent.Context.class, Connection.class, NetworkDirection.class, int.class);
        }
        catch (Exception e) {
            log.error("Exception occurred while accessing getLoginIndex: {}", e.getMessage(), e);
            return null;
        }
    }

    static {
        handshakeField = fetchHandshakeChannel();
        contextConstructor = fetchNetworkEventContext();
    }
}
