package com.leisuretimedock.crossplugin;

import com.google.inject.Inject;
import com.leisuretimedock.crossplugin.command.ReloadConfigCommand;
import com.leisuretimedock.crossplugin.listener.PingMessageListener;
import com.leisuretimedock.crossplugin.listener.PluginMessageListener;
import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.leisuretimedock.crossplugin.manager.PingManager;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = Static.PLUGIN_ID,
        name = Static.PLUGIN_NAME,
        version = Static.PLUGIN_VERSION,
        authors = "R3944Realms"
)
public class CrossPlugin {
    public static final Marker CROSS_TELEPORT_MOD = MarkerFactory.getMarker("[CrossTeleportMod]");
    private final ProxyServer server;
    public final Logger logger;
    public final PluginMessageListener pluginMessageListener;
    public final PingMessageListener pingMessageListener;
    public static boolean isLuckPermsEnabled;
    public final PluginContainer pluginContainer;
    public final ConfigManager config;
    public final PingManager pingManager;
    @Inject
    public CrossPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory , PluginContainer pluginContainer) throws IOException {
        this.server = server;
        this.logger = logger;
        this.config = new ConfigManager(dataDirectory);
        this.pingMessageListener = new PingMessageListener(config);
        this.pingManager = new PingManager(server, config);

        I18n.addBundle(Locale.US);
        I18n.addBundle(Locale.SIMPLIFIED_CHINESE);
        I18n.init();
        this.pluginMessageListener = new PluginMessageListener(server, logger, config);
        this.pluginContainer = pluginContainer;
        server.getCommandManager().register(
                server.getCommandManager()
                        .metaBuilder("ltdcs")
                        .aliases("ltd", "l")
                        .plugin(pluginContainer)
                        .build()
                ,
                new ReloadConfigCommand(config)
        );
    }

    @Subscribe
    public void onProxyInit(ProxyInitializeEvent event) {
        server.getChannelRegistrar().register(
                PluginMessageListener.CHANNEL_ID,
                PluginMessageListener.TELEPORT_ID,
                PingMessageListener.PING_ID,
                PingMessageListener.PONG_ID
        );
        if (config.isEnablePing()) {
            server.getEventManager().register(this, pluginMessageListener);
            server.getEventManager().register(this, pingMessageListener);
            server.getScheduler()
                    .buildTask(this, pingManager::measureAllBackendPing)
                    .repeat(config.getIntervalPing(), TimeUnit.SECONDS)
                    .schedule();
        }
        isLuckPermsEnabled = server.getPluginManager().getPlugin("luckperms").isPresent();
        logger.info(CROSS_TELEPORT_MOD, "[INIT] Plugin initialized, channel registered.");
    }



}
