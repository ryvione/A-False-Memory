package com.ryvione.falsememory.network;

import com.ryvione.falsememory.FalseMemory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetwork {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(FalseMemory.MOD_ID);
        registrar.playToClient(SanityPacket.TYPE, SanityPacket.STREAM_CODEC, SanityPacket::handle);
        registrar.playToClient(HorrorEventPacket.TYPE, HorrorEventPacket.STREAM_CODEC, HorrorEventPacket::handle);
        registrar.playToClient(EndingPacket.TYPE, EndingPacket.STREAM_CODEC, EndingPacket::handle);
        registrar.playToClient(StructureSpawnPacket.TYPE, StructureSpawnPacket.STREAM_CODEC, StructureSpawnPacket::handle);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(FalseMemory.MOD_ID);

        registrar.playToClient(
            SanityPacket.TYPE,
            SanityPacket.STREAM_CODEC,
            SanityPacket::handle
        );

        registrar.playToClient(
            HorrorEventPacket.TYPE,
            HorrorEventPacket.STREAM_CODEC,
            HorrorEventPacket::handle
        );
    }
}