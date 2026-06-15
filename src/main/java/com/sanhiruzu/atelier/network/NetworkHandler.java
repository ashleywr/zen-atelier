package com.sanhiruzu.atelier.network;

import com.sanhiruzu.atelier.ui.network.SynthesisCatalogSyncPayload;
import com.sanhiruzu.atelier.ui.network.DiscoveryDataSyncPayload;
import com.sanhiruzu.atelier.ui.network.ExtractionKnowledgeSyncPayload;
import com.sanhiruzu.atelier.ui.network.ReagentVaultSyncPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisBoardFusionPayload;
import com.sanhiruzu.atelier.ui.network.SynthesisResultPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class NetworkHandler {
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1.0")
                .playToClient(ToggleDebugPayload.TYPE, ToggleDebugPayload.CODEC,
                        NetworkHandler::handleToggleDebug)
                .playToClient(DiscoveryDataSyncPayload.TYPE, DiscoveryDataSyncPayload.CODEC,
                        NetworkHandler::handleDiscoveryDataSync)
                .playToClient(ExtractionKnowledgeSyncPayload.TYPE, ExtractionKnowledgeSyncPayload.CODEC,
                        NetworkHandler::handleExtractionKnowledgeSync)
                .playToClient(SynthesisCatalogSyncPayload.TYPE, SynthesisCatalogSyncPayload.CODEC,
                        NetworkHandler::handleSynthesisCatalogSync)
                .playToClient(ReagentVaultSyncPayload.TYPE, ReagentVaultSyncPayload.CODEC,
                        NetworkHandler::handleReagentVaultSync)
                .playToClient(SynthesisResultPayload.TYPE, SynthesisResultPayload.CODEC,
                        NetworkHandler::handleSynthesisResult)
                .playToServer(SynthesisBoardFusionPayload.TYPE, SynthesisBoardFusionPayload.CODEC,
                        NetworkHandler::handleBoardFusion);
    }

    private static void handleToggleDebug(ToggleDebugPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientDebugToggle(payload));
    }

    private static void handleDiscoveryDataSync(DiscoveryDataSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientDiscoveryDataSync(payload));
    }

    private static void handleExtractionKnowledgeSync(ExtractionKnowledgeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientExtractionKnowledgeSync(payload));
    }

    private static void handleSynthesisCatalogSync(SynthesisCatalogSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientSynthesisCatalogSync(payload));
    }

    private static void handleReagentVaultSync(ReagentVaultSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientReagentVaultSync(payload));
    }

    private static void handleBoardFusion(SynthesisBoardFusionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (player.containerMenu instanceof com.sanhiruzu.atelier.synthesis.menu.SynthesisStationMenu menu
                    && menu.containerId == payload.containerId()) {
                menu.setPendingFusionData(payload);
            }
        });
    }

    private static void handleClientDebugToggle(ToggleDebugPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleDebugToggle", boolean.class).invoke(null, payload.enabled());
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleClientDiscoveryDataSync(DiscoveryDataSyncPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleDiscoveryDataSync", DiscoveryDataSyncPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleClientExtractionKnowledgeSync(ExtractionKnowledgeSyncPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleExtractionKnowledgeSync", ExtractionKnowledgeSyncPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleClientSynthesisCatalogSync(SynthesisCatalogSyncPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleSynthesisCatalogSync", SynthesisCatalogSyncPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleSynthesisResult(SynthesisResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> handleClientSynthesisResult(payload));
    }

    private static void handleClientSynthesisResult(SynthesisResultPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleSynthesisResult", SynthesisResultPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }

    private static void handleClientReagentVaultSync(ReagentVaultSyncPayload payload) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            try {
                Class<?> handlers = Class.forName("com.sanhiruzu.atelier.ui.client.ClientPayloadHandlers");
                handlers.getMethod("handleReagentVaultSync", ReagentVaultSyncPayload.class).invoke(null, payload);
            } catch (ReflectiveOperationException ignored) {
            }
        }
    }
}
