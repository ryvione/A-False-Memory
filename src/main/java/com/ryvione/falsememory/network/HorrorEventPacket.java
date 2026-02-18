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

public record HorrorEventPacket(int eventType, float intensity) implements CustomPacketPayload {

    public static final int EVENT_SHAKE = 0;
    public static final int EVENT_STATIC = 1;
    public static final int EVENT_VIGNETTE = 2;
    public static final int EVENT_INVERT = 3;

    public static final CustomPacketPayload.Type<HorrorEventPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "horror_event")
        );

    public static final StreamCodec<FriendlyByteBuf, HorrorEventPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> { buf.writeVarInt(pkt.eventType()); buf.writeFloat(pkt.intensity()); },
            buf -> new HorrorEventPacket(buf.readVarInt(), buf.readFloat())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @OnlyIn(Dist.CLIENT)
    public static void handle(HorrorEventPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            switch (packet.eventType()) {
                case EVENT_SHAKE -> SanityEffects.triggerShake(10, packet.intensity());
                case EVENT_STATIC -> SanityEffects.triggerStaticFlicker((int)(packet.intensity() * 10));
                case EVENT_VIGNETTE -> SanityEffects.triggerVignettePulse(packet.intensity());
                case EVENT_INVERT -> SanityEffects.triggerInvert();
            }
        });
    }
}