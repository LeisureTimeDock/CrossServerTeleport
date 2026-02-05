package com.leisuretimedock.crossmod.config;

import com.leisuretimedock.crossmod.network.NetworkHandler;
import com.leisuretimedock.crossmod.network.toClient.CommonConfigHashInformPacket;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@Slf4j
public class CrossServerConfigManager {
    public static CrossServerConfigManager INSTANCE = new CrossServerConfigManager();
    public static final Pattern SYNTAX =
            compile("([a-zA-Z]\\w+):\\s+([_.\\w]+)");
    /**
     * The constant cacheTag.
     */
    // ========= 缓存 ========
    public volatile CompoundTag cacheTag = null;
    /**
     * The constant cacheHash.
     */
    public volatile int cacheHash = -1;

    @Getter
    private final Map<String, String> servers = new TreeMap<>();

    private @NotNull @Unmodifiable Map<String, String> parseServer(@NotNull List<? extends String> servers) {
        Map<String, String> serverMap = new TreeMap<>();
        for (String server : servers) {
            Matcher matcher = SYNTAX.matcher(server);
            if (matcher.matches()) {
                String key = matcher.group(1);   // 第一部分：[a-zA-Z]\w+
                String value = matcher.group(2); // 第二部分：[_.\w]+
                if(!serverMap.containsKey(key)) {
                    serverMap.put(key, value);
                } else {
                    log.warn("Duplicate server name '{}' found in config, skip it", key);
                }
            }
        }
        return Collections.unmodifiableMap(serverMap);
    }

    public void reloadAll() {
        try {
            clear();
            servers.putAll(parseServer(CrossServerConfig.SERVER_LIST.get()));
            cacheTag = serializeToNBT();
        } catch (Exception e) {
            log.error("Failed to reload configs", e);
            cacheHash = -1;
            cacheTag = null;
        }

    }

    /**
     * Clear.
     */
    public void clear() {
        servers.clear();
    }

    public synchronized CompoundTag serializeToNBT() {
        if (cacheHash == calculateConfigHash() && cacheTag != null) return cacheTag;
        CompoundTag tag = new CompoundTag();
        serializeMap(tag, "servers", this.servers);
        cacheHash = calculateConfigHash();
        cacheTag = tag;
        return tag;
    }

    private void serializeMap(CompoundTag parent, String key, @NotNull Map<String, String> map) {
        CompoundTag mapTag = new CompoundTag();
        for (Map.Entry<String,String> entry : map.entrySet()) {
            mapTag.putString(entry.getKey(), entry.getValue());
        }
        parent.put(key, mapTag);
    }


    /**
     * 从NBT反序列化配置管理器状态
     *
     * @param tag the tag
     */
    public void deserializeFromNBT(@NotNull CompoundTag tag) {
        clear();
        deserializeMap(tag, "servers", servers);
    }

    private void deserializeMap(@NotNull CompoundTag parent, String key, Map<String, String> map) {
        if (parent.contains(key)) {
            CompoundTag mapTag = parent.getCompound(key);
            for (String key_ : mapTag.getAllKeys()) {
                map.put(key_, mapTag.getString(key_));
            }
        }
    }

    /**
     * Loading.
     *
     * @param manager the manager
     */
    public static void loading(@NotNull CrossServerConfigManager manager) {
        manager.reloadAll();
    }

    /**
     * Reloading.
     *
     * @param manager the manager
     */
    public static void reloading(@NotNull CrossServerConfigManager manager) {
        manager.reloadAll();
    }

    /**
     * Unloading.
     *
     * @param manager the manager
     */
    public static void unloading(CrossServerConfigManager manager) {
        if(manager != null) manager.clear();
    }

    public int calculateConfigHash() {
        // 使用FNV-1a哈希算法
        int hash = 0x811c9dc5; // FNV偏移基础值
        hash = fnv1aHashMap(hash, servers);
        return hash;
    }

    private int fnv1aHashString(int hash, @NotNull String str) {
        for (int i = 0; i < str.length(); i++) {
            hash ^= str.charAt(i);
            hash *= 0x01000193;
        }
        return hash;
    }
    private int fnv1aHashMap(int hash, @NotNull Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            hash = fnv1aHashString(hash, entry.getKey());
            hash = fnv1aHashString(hash, entry.getValue());

        }
        return hash;
    }
    /**
     * Broad hash packet.
     */
    public void broadHashPacket() {
        if (cacheHash != -1){
            NetworkHandler.sendToAllPlayer(new CommonConfigHashInformPacket(cacheHash));
        }
    }
}
