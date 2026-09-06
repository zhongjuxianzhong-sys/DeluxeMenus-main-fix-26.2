package com.extendedclip.deluxemenus.utils;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

public class SoundUtils {

    public static Sound getSound(String name) {
        try {
            // As of Minecraft 1.21.3, the org.bukkit.Sound class type changed from Enum to Interface.
            // This fixes java.lang.IncompatibleClassChangeError when trying to use versions prior to 1.21.3.
            Method valueOfMethod = Class.forName("org.bukkit.Sound").getMethod("valueOf", String.class);
            return (Sound) valueOfMethod.invoke(null, name);
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            return getSoundFromRegistry(name);
        }
    }

    private static Sound getSoundFromRegistry(String name) {
        final NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        if (key != null) {
            final Sound sound = Registry.SOUND_EVENT.get(key);
            if (sound != null) {
                return sound;
            }
        }

        // Registry keys use dots while legacy enum constants use underscores, so keep field lookup as a fallback.
        try {
            return (Sound) Sound.class.getField(name.toUpperCase(Locale.ROOT)).get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalArgumentException("No sound found with the name " + name, e);
        }
    }
}
