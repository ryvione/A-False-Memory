package com.ryvione.falsememory.network;

import com.ryvione.falsememory.FalseMemory;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record StructureSpawnPacket(int structureType, int x, int y, int z) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StructureSpawnPacket> TYPE =
        new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(FalseMemory.MOD_ID, "structure_spawn")
        );

    public static final StreamCodec<FriendlyByteBuf, StructureSpawnPacket> STREAM_CODEC =
        StreamCodec.of(
            (buf, pkt) -> {
                buf.writeVarInt(pkt.structureType());
                buf.writeVarInt(pkt.x());
                buf.writeVarInt(pkt.y());
                buf.writeVarInt(pkt.z());
            },
            buf -> new StructureSpawnPacket(buf.readVarInt(), buf.readVarInt(),
                buf.readVarInt(), buf.readVarInt())
        );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }

    @OnlyIn(Dist.CLIENT)
    public static void handle(StructureSpawnPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            com.ryvione.falsememory.client.SanityEffects.triggerStaticFlicker(5);
            com.ryvione.falsememory.client.SanityEffects.triggerVignettePulse(0.6f);
        });
    }
}