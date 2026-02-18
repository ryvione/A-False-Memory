package com.ryvione.falsememory.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.Random;

public class SoundUtil {

    private static final Random RNG = new Random();

    public static void playForPlayer(ServerPlayer player, String soundId,
                                      SoundSource source, float volume, float pitch) {
        try {
            var soundEvent = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
            if (soundEvent != null) {
                player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
                    source,
                    player.getX(), player.getY(), player.getZ(),
                    volume, pitch,
                    RNG.nextLong()
                ));
            }
        } catch (Exception ignored) {}
    }

    public static void playAtPosition(ServerPlayer player, String soundId,
                                       SoundSource source, double x, double y, double z,
                                       float volume, float pitch) {
        try {
            var soundEvent = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(soundId));
            if (soundEvent != null) {
                player.connection.send(new ClientboundSoundPacket(
                    BuiltInRegistries.SOUND_EVENT.wrapAsHolder(soundEvent),
                    source, x, y, z, volume, pitch, RNG.nextLong()
                ));
            }
        } catch (Exception ignored) {}
    }
}