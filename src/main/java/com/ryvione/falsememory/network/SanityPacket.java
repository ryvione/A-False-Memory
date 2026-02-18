package com.ryvione.falsememory.network;

import com.ryvione.falsememory.FalseMemory;
import com.ryvione.falsememory.client.SanityEffects;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SanityPacket(int tier) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SanityPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "sanity_sync")
        );

    public static final StreamCodec<FriendlyByteBuf, SanityPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, packet) -> buf.writeVarInt(packet.tier()),
            buf -> new SanityPacket(buf.readVarInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(SanityPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            SanityEffects.setTier(packet.tier());
        });
    }
}