package com.leisuretimedock.crossplugin.manager;

import com.leisuretimedock.crossplugin.CrossPlugin;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.leisuretimedock.crossplugin.CrossPlugin.CROSS_TELEPORT_MOD;

@Slf4j
public class ConfigManager {
    private final Path configPath;
    private final YamlConfigurationLoader loader;
    private final Map<String, String> serverAliases = new ConcurrentHashMap<>();
    private final Set<String> overlayServers = ConcurrentHashMap.newKeySet();
    @Getter
    private boolean enablePing;
    @Getter
    private long intervalPing;
    @Getter
    private boolean enableErrorLog = true;
    @Getter
    private boolean enableListenerLog = true;
    @Getter
    private boolean enablePingLog = true;
    @Getter
    private int goodThreshold;
    @Getter
    private int moderateThreshold;
    @Getter
    private int badThreshold;

    private ConfigurationNode rootNode;

    public ConfigManager(Path configDir) throws IOException {
        this.configPath = configDir.resolve("config.yml");
        copyDefaultConfigIfAbsent(this.configPath);
        this.loader = YamlConfigurationLoader.builder()
                .path(configPath)
                .indent(2)
                .build();
        load();
    }

    /**
     * Loads or creates the configuration file
     * @throws IOException if configuration cannot be loaded
     */
    public synchronized void load() throws IOException {
        try {
            rootNode = loader.load();

            // Load server aliases
            ConfigurationNode aliasesNode = rootNode.node("server-aliases");
            if (aliasesNode.virtual() || aliasesNode.empty()) {
                aliasesNode.node("survival").set("survival");
                aliasesNode.node("lobby").set("lobby");
                loader.save(rootNode);
            }

            serverAliases.clear();
            aliasesNode.childrenMap().forEach((key, node) -> {
                String realName = node.getString();
                if (realName != null) serverAliases.put(key.toString(), realName);
            });

            // Load overlay servers
            ConfigurationNode overlayNode = rootNode.node("show-overlay-servers");
            if (overlayNode.virtual() || overlayNode.empty()) {
                overlayNode.setList(String.class, List.of("lobby"));
                loader.save(rootNode);
            }

            overlayServers.clear();
            for (ConfigurationNode node : overlayNode.childrenList()) {
                String name = node.getString();
                if (name != null) overlayServers.add(name.toLowerCase());
            }
            // Load ping setting
            ConfigurationNode pingSetting = rootNode.node("ping-setting");
            {
                ConfigurationNode enable_ping = pingSetting.node("enable-ping");
                ConfigurationNode interval = pingSetting.node("interval");

                if (pingSetting.virtual() || pingSetting.empty()) {
                    enable_ping.set(true);
                    interval.set(Long.class, 5);
                    loader.save(rootNode);
                }
                enablePing = enable_ping.getBoolean(true);
                intervalPing = interval.getLong(5);
                // 加载阈值配置
                ConfigurationNode thresholdsNode = rootNode.node("ping-setting", "thresholds");
                if (thresholdsNode.virtual() || thresholdsNode.empty()) {
                    // 设置默认阈值
                    thresholdsNode.node("good").set(100);
                    thresholdsNode.node("moderate").set(200);
                    thresholdsNode.node("bad").set(300);
                    loader.save(rootNode);
                }

                goodThreshold = thresholdsNode.node("good").getInt(100);
                moderateThreshold = thresholdsNode.node("moderate").getInt(200);
                badThreshold = thresholdsNode.node("bad").getInt(300);

                // 验证阈值有效性
                validateThresholds();

            }

            // Load log setting
            ConfigurationNode logInfoSetting = rootNode.node("log-info-setting");
            {
                ConfigurationNode enable_error_log = logInfoSetting.node("enable-error-log");
                ConfigurationNode enable_listener_log = logInfoSetting.node("enable-listener-log");
                ConfigurationNode enable_ping_log = logInfoSetting.node("enable-ping-log");
                if (logInfoSetting.virtual() || logInfoSetting.empty()) {
                    enable_error_log.set(true);
                    enable_listener_log.set(true);
                    enable_ping_log.set(true);
                    loader.save(rootNode);
                }
                enableErrorLog = enable_error_log.getBoolean(true);
                enableListenerLog = enable_listener_log.getBoolean(true);
                enablePingLog = enable_ping_log.getBoolean(true);
            }



            log.info(CROSS_TELEPORT_MOD, "Loaded {} server aliases from config", serverAliases.size());
            log.info(CROSS_TELEPORT_MOD, "Loaded {} overlay servers from config", overlayServers.size());
            log.info(CROSS_TELEPORT_MOD, "Loaded log setting from config ,error-log:{}, listener-log;{}, ping-log:{}", enableErrorLog, enableListenerLog, enablePingLog);
        } catch (IOException e) {
            log.error(CROSS_TELEPORT_MOD, "Failed to load configuration from {}", configPath, e);
            throw e;
        }
    }

    /**
     * Reloads the configuration from disk
     */
    public synchronized void reload() throws IOException {
        load();
    }

    /**
     * Validate Thresholds‘ valye
     */
    private void validateThresholds() {
        if (goodThreshold >= moderateThreshold || moderateThreshold >= badThreshold) {
            throw new IllegalStateException("Invalid latency thresholds configuration: " +
                    "must be good < moderate < bad");
        }
    }

    /**
     * Saves the current configuration to disk
     */
    public synchronized void save() throws IOException {
        loader.save(rootNode);
    }

    /**
     * Resolves a server name from its alias
     * @param key The alias or real server name to resolve
     * @return The real server name, or the input if no alias exists
     */
    public String resolveServerName(String key) {
        return serverAliases.getOrDefault(key, key);
    }

    /**
     * Gets all server aliases as an unmodifiable map
     */
    public Map<String, String> getServerAliases() {
        return Collections.unmodifiableMap(serverAliases);
    }

    /**
     * Adds or updates a server alias
     * @param alias The alias to add/update
     * @param realName The real server name
     */
    public synchronized void setServerAlias(String alias, String realName) throws IOException {
        rootNode.node("server-aliases", alias).set(realName);
        serverAliases.put(alias, realName);
        save();
    }

    /**
     * Removes a server alias
     * @param alias The alias to remove
     */
    public synchronized void removeServerAlias(String alias) throws IOException {
        rootNode.node("server-aliases").removeChild(alias);
        serverAliases.remove(alias);
        save();
    }

    /**
     * Servers should show overlay
     * @return lists Show OverLay when join they
     */
    public Set<String> getOverlayServers() {
        return Collections.unmodifiableSet(overlayServers);
    }

    private void copyDefaultConfigIfAbsent(Path configPath) throws IOException {
        if (Files.notExists(configPath)) {
            // 先创建父目录（如果不存在）
            Files.createDirectories(configPath.getParent());
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    throw new IOException("Missing embedded config.yml in resources!");
                }
                Files.copy(in, configPath);
                log.info(CROSS_TELEPORT_MOD, "Default config.yml copied to {}", configPath);
            }
        }
    }
}
