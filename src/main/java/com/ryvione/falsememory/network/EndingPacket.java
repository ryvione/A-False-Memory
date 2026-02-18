package com.ryvione.falsememory.network;

import com.ryvione.falsememory.FalseMemory;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EndingPacket(int endingType) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EndingPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "ending")
        );

    public static final StreamCodec<FriendlyByteBuf, EndingPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> buf.writeVarInt(pkt.endingType()),
            buf -> new EndingPacket(buf.readVarInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public static void handle(EndingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.ryvione.falsememory.client.SanityEffects.triggerInvert();
        });
    }
}