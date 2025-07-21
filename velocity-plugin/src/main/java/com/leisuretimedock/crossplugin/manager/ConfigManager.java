package com.leisuretimedock.crossplugin.manager;

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

@Slf4j
public class ConfigManager {
    private final Path configPath;
    private final YamlConfigurationLoader loader;
    private final Map<String, String> serverAliases = new ConcurrentHashMap<>();
    private final Set<String> overlayServers = ConcurrentHashMap.newKeySet();
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

            log.info("Loaded {} server aliases from config", serverAliases.size());
            log.info("Loaded {} overlay servers from config", overlayServers.size());
        } catch (IOException e) {
            log.error("Failed to load configuration from {}", configPath, e);
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
                log.info("Default config.yml copied to {}", configPath);
            }
        }
    }
}
