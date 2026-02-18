package com.ryvione.falsememory.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

public class TitleUtil {

    public static void send(ServerPlayer player, String title, String subtitle,
                             int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title)));
        player.connection.send(new ClientboundSetSubtitleTextPacket(
            Component.literal(subtitle).withStyle(ChatFormatting.DARK_GRAY)));
    }

    public static void sendFormatted(ServerPlayer player, Component title, Component subtitle,
                                      int fadeIn, int stay, int fadeOut) {
        player.connection.send(new ClientboundSetTitlesAnimationPacket(fadeIn, stay, fadeOut));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
    }

    public static void clear(ServerPlayer player) {
        player.connection.send(new ClientboundClearTitlesPacket(true));
    }
}