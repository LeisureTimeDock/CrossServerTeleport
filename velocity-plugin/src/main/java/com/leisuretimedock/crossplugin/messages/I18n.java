package com.leisuretimedock.crossplugin.messages;

import com.leisuretimedock.crossplugin.Static;
import lombok.extern.slf4j.Slf4j;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.translation.TranslationRegistry;
import net.kyori.adventure.util.UTF8ResourceBundleControl;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

@Slf4j
public class I18n {
    public static TranslationRegistry registry = TranslationRegistry.create(Key.key(Static.MOD_ID +":value"));
    public static HashMap<Locale,ResourceBundle> bundles = new HashMap<>();
    public static void init() {
        for (Map.Entry<Locale, ResourceBundle> bundle : bundles.entrySet()) {
            registry.registerAll(bundle.getKey(), bundle.getValue(), true);
        }
        GlobalTranslator.translator().addSource(registry);
    }
    public static ResourceBundle getBundle(Locale locale) {
        return ResourceBundle.getBundle("crossserver.Bundle."+ locale.toLanguageTag().replace('-','_'), locale, UTF8ResourceBundleControl.get());
    }

    public static void addBundle(Locale locale) {
        if (bundles.containsKey(locale)) {
            log.warn("Duplicate bundle locale: {}", locale);
        }
        else bundles.put(locale, getBundle(locale));
    }
    // 基础无颜色版本
    public static Component translatable(String key, ComponentLike... args) {
        return Component.translatable(key, args);
    }

    public static Component translatable(String key) {
        return Component.translatable(key);
    }

    public static Component translatable(String key, String fallback, ComponentLike... args) {
        return Component.translatable(key, fallback, args);
    }

    public static Component translatable(String key, String fallback) {
        return Component.translatable(key, fallback);
    }

    public static Component translatable(I18nKeyEnum key, ComponentLike... args) {
        return Component.translatable(key.getKey(), args);
    }

    public static Component translatable(I18nKeyEnum key) {
        return Component.translatable(key.getKey());
    }

    public static Component translatable(I18nKeyEnum key, String fallback, ComponentLike... args) {
        return Component.translatable(key.getKey(), fallback, args);
    }

    public static Component translatable(I18nKeyEnum key, String fallback) {
        return Component.translatable(key.getKey(), fallback);
    }

    // 下面是带颜色或样式版本，核心是先创建基础 Component，再调用 color 或 style

    public static Component translatable(String key, NamedTextColor color, ComponentLike... args) {
        return Component.translatable(key, args).color(color);
    }

    public static Component translatable(String key, Style style, ComponentLike... args) {
        return Component.translatable(key, args).style(style);
    }

    public static Component translatable(I18nKeyEnum key, NamedTextColor color, ComponentLike... args) {
        return Component.translatable(key.getKey(), args).color(color);
    }

    public static Component translatable(I18nKeyEnum key, Style style, ComponentLike... args) {
        return Component.translatable(key.getKey(), args).style(style);
    }

    // 带 fallback 的带颜色或样式版本

    public static Component translatable(String key, String fallback, NamedTextColor color, ComponentLike... args) {
        return Component.translatable(key, fallback, args).color(color);
    }

    public static Component translatable(String key, String fallback, Style style, ComponentLike... args) {
        return Component.translatable(key, fallback, args).style(style);
    }

    public static Component translatable(I18nKeyEnum key, String fallback, NamedTextColor color, ComponentLike... args) {
        return Component.translatable(key.getKey(), fallback, args).color(color);
    }

    public static Component translatable(I18nKeyEnum key, String fallback, Style style, ComponentLike... args) {
        return Component.translatable(key.getKey(), fallback, args).style(style);
    }
}
