package com.mat37dev;

import com.mat37dev.client.creator.CreatorClientState;
import com.mat37dev.client.creator.StructureListScreen;
import com.mat37dev.client.creator.StructurePreviewRenderer;
import com.mat37dev.client.gui.ImportTableScreen;
import com.mat37dev.client.gui.MillChestScreen;
import com.mat37dev.client.gui.VillageCreationScreen;
import com.mat37dev.client.render.entity.MillVillagerEntityRenderer;
import com.mat37dev.client.render.entity.MillVillagerModel;
import com.mat37dev.creator.ImportTableConfig;
import com.mat37dev.init.MillBlockEntities;
import com.mat37dev.init.MillEntities;
import com.mat37dev.network.OpenImportTablePayload;
import com.mat37dev.network.OpenStructureListPayload;
import com.mat37dev.network.OpenVillageCreationPayload;
import com.mat37dev.network.StructurePreviewPayload;
import com.mat37dev.network.StructureRotationPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class MillenaireNewAgeClient implements ClientModInitializer {

    /**
     * État des particules par position de table, mis à jour par le receiver et par l'écran.
     * Indépendant du sync BE — évite les problèmes de handleUpdateTag en 1.21.10.
     */
    private static final Map<BlockPos, Boolean>           PARTICLES_ENABLED = new HashMap<>();
    private static final Map<BlockPos, ImportTableConfig> PARTICLES_CONFIG   = new HashMap<>();
    private static int particleTick = 0;

    /** Appelé depuis le receiver et depuis l'écran pour mettre à jour l'état des particules. */
    public static void updateParticlesState(BlockPos pos, boolean enabled,
                                            @Nullable ImportTableConfig config) {
        PARTICLES_ENABLED.put(pos, enabled);
        if (config != null) PARTICLES_CONFIG.put(pos, config);
    }

    @Override
    public void onInitializeClient() {
        // Entités — layer model + renderer
        EntityModelLayerRegistry.registerModelLayer(MillVillagerModel.LAYER_LOCATION,
                MillVillagerModel::createBodyLayer);
        EntityRendererRegistry.register(MillEntities.VILLAGER, MillVillagerEntityRenderer::new);

        // Block Entity Renderer — MillChestBlock utilise ChestRenderer (coffre vanilla animé)
        BlockEntityRendererRegistry.register(MillBlockEntities.MILL_CHEST_ENTITY, ChestRenderer::new);

        // Screen handlers
        MenuScreens.register(MillBlockEntities.MILL_CHEST_MENU, MillChestScreen::new);

        // Rendu ghost preview
        StructurePreviewRenderer.init();

        // S→C : données de preview structure
        ClientPlayNetworking.registerGlobalReceiver(StructurePreviewPayload.ID, (payload, ctx) ->
            ctx.client().execute(() ->
                CreatorClientState.setPreview(
                    payload.structureId(),
                    payload.relativeBlocks(),
                    payload.structureSize()
                )
            )
        );

        // Besoin de ça pour render les textures transparente
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_DIRT, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_DIRT_SLAB, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_GRAVEL, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_GRAVEL_SLAB, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_SLABS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_SLABS_SLAB, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STAINED_GLASS_WHITE, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STAINED_GLASS_YELLOW, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STAINED_GLASS_YELLOW_RED, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STAINED_GLASS_RED_BLUE, ChunkSectionLayer.TRANSLUCENT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STAINED_GLASS_GREEN_BLUE, ChunkSectionLayer.TRANSLUCENT);

        // Positions (Tapis) — Toujours besoin du CUTOUT pour la forme
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SLEEPING_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SELLING_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.CRAFTING_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.DEFENDING_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SHELTER_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.LEISURE_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.STALL_POS, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.PATH_START_POS, ChunkSectionLayer.CUTOUT);
        
        // Sources et Sols (Overlay transparent)
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOURCE_ROCK, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOURCE_SAND, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOIL_FLOWER, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOIL_WHEAT, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOIL_CARROT, ChunkSectionLayer.CUTOUT);
        BlockRenderLayerMap.putBlock(com.mat37dev.init.MillBlocks.SOIL_POTATO, ChunkSectionLayer.CUTOUT);
        
        // NOTE: Les Plantations et Spawns d'animaux ne sont PAS dans le CUTOUT 
        // pour garder leur fond noir opaque d'origine (couche SOLID par défaut).

        // S→C : sync rotation
        ClientPlayNetworking.registerGlobalReceiver(StructureRotationPayload.ID, (payload, ctx) ->
            ctx.client().execute(() -> CreatorClientState.setRotation(payload.rotation()))
        );

        // S→C : ouvrir la GUI de liste des structures
        ClientPlayNetworking.registerGlobalReceiver(OpenStructureListPayload.ID, (payload, ctx) ->
            ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(new StructureListScreen(payload.structureIds()))
            )
        );

        // S→C : ouvrir le GUI de création de village
        ClientPlayNetworking.registerGlobalReceiver(OpenVillageCreationPayload.ID, (payload, ctx) ->
            ctx.client().execute(() ->
                Minecraft.getInstance().setScreen(
                    new VillageCreationScreen(payload.goldPos(), payload.cultures())
                )
            )
        );

        // S→C : ouvrir la GUI Import Table
        ClientPlayNetworking.registerGlobalReceiver(OpenImportTablePayload.ID, (payload, ctx) ->
            ctx.client().execute(() -> {
                // Mettre à jour l'état des particules depuis le payload serveur
                updateParticlesState(payload.tablePos(), payload.particlesEnabled(), payload.config());
                Minecraft.getInstance().setScreen(new ImportTableScreen(payload));
            })
        );

        // Tick : particules autour des Import Tables avec particlesEnabled
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;
            if (++particleTick % 5 != 0) return;

            Level level = client.level;
            for (Map.Entry<BlockPos, Boolean> entry : PARTICLES_ENABLED.entrySet()) {
                if (!entry.getValue()) continue;
                ImportTableConfig cfg = PARTICLES_CONFIG.get(entry.getKey());
                if (cfg == null) continue;
                spawnImportTableParticles(level, entry.getKey(), cfg);
            }
        });
    }

    /**
     * Spawn des particules en cage 3D (arêtes d'un cube) autour de la zone de la structure.
     * — 4 colonnes verticales aux coins (sol → plafond, 1 particule tous les 2 blocs)
     * — 1 anneau horizontal au niveau du sol (floorY)
     * — 1 anneau horizontal au niveau du plafond (floorY + height)
     * — 1 anneau horizontal au fond si depth > 0 (floorY - depth)
     */
    private static void spawnImportTableParticles(Level level, BlockPos tablePos, ImportTableConfig config) {
        int tx     = tablePos.getX();
        int tz     = tablePos.getZ();
        int floorY = tablePos.getY() + config.floorHeight();
        int w      = config.width();
        int l      = config.length();
        int h      = config.height();
        int d      = config.depth();

        // Bords XZ (zone commence à tx+1, tz+1 — la table est en dehors au coin bas-gauche)
        double x0 = tx + 0.5;        // bord gauche
        double x1 = tx + w + 0.5;    // bord droit
        double z0 = tz + 0.5;        // bord avant
        double z1 = tz + l + 0.5;    // bord arrière

        int yBase = floorY - d;       // fond de la zone (= floorY si depth == 0)
        int yTop  = floorY + h;       // plafond (1 au-dessus du dernier bloc)

        // Arêtes verticales aux 4 coins (tous les 2 blocs en Y)
        for (int y = yBase; y <= yTop; y += 2) {
            double py = y + 0.5;
            level.addParticle(ParticleTypes.FLAME, x0, py, z0, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, x1, py, z0, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, x0, py, z1, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, x1, py, z1, 0, 0, 0);
        }

        // Anneau horizontal au niveau sol
        spawnHorizontalRing(level, tx, tz, w, l, floorY + 0.5, x0, x1, z0, z1);
        // Anneau horizontal au niveau plafond
        spawnHorizontalRing(level, tx, tz, w, l, yTop   + 0.5, x0, x1, z0, z1);
        // Anneau horizontal au fond si profondeur > 0
        if (d > 0) {
            spawnHorizontalRing(level, tx, tz, w, l, yBase + 0.5, x0, x1, z0, z1);
        }
    }

    /** Anneau horizontal de particules aux 4 bords d'un plan XZ, tous les 2 blocs. */
    private static void spawnHorizontalRing(Level level, int tx, int tz, int w, int l,
                                             double py,
                                             double x0, double x1, double z0, double z1) {
        // Bords sur X (de tx+1 à tx+width)
        for (int x = 1; x <= w; x += 2) {
            double px = tx + x + 0.5;
            level.addParticle(ParticleTypes.FLAME, px, py, z0, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, px, py, z1, 0, 0, 0);
        }
        // Bords sur Z (de tz+1 à tz+length)
        for (int z = 1; z <= l; z += 2) {
            double pz = tz + z + 0.5;
            level.addParticle(ParticleTypes.FLAME, x0, py, pz, 0, 0, 0);
            level.addParticle(ParticleTypes.FLAME, x1, py, pz, 0, 0, 0);
        }
    }
}
