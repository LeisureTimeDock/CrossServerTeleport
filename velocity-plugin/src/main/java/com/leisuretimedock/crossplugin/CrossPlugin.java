package com.leisuretimedock.crossplugin;

import com.google.inject.Inject;
import com.leisuretimedock.crossplugin.command.ReloadConfigCommand;
import com.leisuretimedock.crossplugin.listener.PluginMessageListener;
import com.leisuretimedock.crossplugin.manager.ConfigManager;
import com.leisuretimedock.crossplugin.messages.I18n;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

@Plugin(
        id = Static.PLUGIN_ID,
        name = Static.PLUGIN_NAME,
        version = Static.PLUGIN_VERSION,
        authors = "R3944Realms"
)
public class CrossPlugin {

    private final ProxyServer server;
    public final Logger logger;
    public final PluginMessageListener listener;
    public static boolean isLuckPermsEnabled;
    public final PluginContainer pluginContainer;
    @Inject
    public CrossPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory ,PluginContainer pluginContainer) throws IOException {
        this.server = server;
        this.logger = logger;
        ConfigManager config = new ConfigManager(dataDirectory);
        I18n.addBundle(Locale.US);
        I18n.addBundle(Locale.SIMPLIFIED_CHINESE);
        I18n.init();
        this.listener = new PluginMessageListener(server, logger, config);
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
        server.getChannelRegistrar().register(PluginMessageListener.CHANNEL_ID, PluginMessageListener.TELEPORT_ID);
        server.getEventManager().register(this, listener);
        isLuckPermsEnabled = server.getPluginManager().getPlugin("luckperms").isPresent();
        logger.info("[INIT] Plugin initialized, channel registered.");
    }



}
