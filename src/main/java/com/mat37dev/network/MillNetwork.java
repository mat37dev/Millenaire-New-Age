package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import com.mat37dev.creator.ImportTableBlockEntity;
import com.mat37dev.creator.ImportTableConfig;
import com.mat37dev.creator.StructurePlacerItem;
import com.mat37dev.creator.StructureSaveManager;
import com.mat37dev.creator.WoolMarkerManager;
import com.mat37dev.init.MillItems;
import com.mat37dev.culture.Culture;
import com.mat37dev.culture.CultureRegistry;
import com.mat37dev.culture.VillageType;
import com.mat37dev.village.VillagePlacer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.List;
import java.util.Optional;

/**
 * Centralise l'enregistrement de tous les {@link net.minecraft.network.protocol.common.custom.CustomPacketPayload}.
 *
 * <p>Appelé depuis {@link com.mat37dev.MillenaireNewAge#onInitialize()}.</p>
 */
public class MillNetwork {

    public static void registerServerPayloads() {
        // S→C
        PayloadTypeRegistry.playS2C().register(StructurePreviewPayload.ID,       StructurePreviewPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StructureRotationPayload.ID,      StructureRotationPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenStructureListPayload.ID,      OpenStructureListPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(OpenVillageCreationPayload.ID,    OpenVillageCreationPayload.CODEC);

        // S→C Import Table
        PayloadTypeRegistry.playS2C().register(OpenImportTablePayload.ID,        OpenImportTablePayload.CODEC);

        // C→S
        PayloadTypeRegistry.playC2S().register(SelectStructurePayload.ID,        SelectStructurePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(DeleteStructurePayload.ID,        DeleteStructurePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CreateVillagePayload.ID,          CreateVillagePayload.CODEC);

        // C→S Import Table
        PayloadTypeRegistry.playC2S().register(ImportTableCreatePayload.ID,         ImportTableCreatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportTableUpdatePayload.ID,         ImportTableUpdatePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportTableSavePayload.ID,           ImportTableSavePayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportTableImportPayload.ID,         ImportTableImportPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(ImportTableToggleParticlesPayload.ID, ImportTableToggleParticlesPayload.CODEC);
    }

    public static void registerServerHandlers() {
        // C→S : le joueur a sélectionné une structure dans la GUI
        ServerPlayNetworking.registerGlobalReceiver(SelectStructurePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player = ctx.player();
                String structureId  = payload.structureId();

                ctx.server().execute(() -> handleSelectStructure(player, structureId));
            }
        );

        // C→S : suppression d'une structure
        ServerPlayNetworking.registerGlobalReceiver(DeleteStructurePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player = ctx.player();
                String structureId  = payload.structureId();

                ctx.server().execute(() -> {
                    handleDeleteStructure(player, structureId);
                    // Renvoyer la liste mise à jour pour rafraîchir la GUI
                    List<String> updatedList = StructureSaveManager.listStructures(ctx.server());
                    ServerPlayNetworking.send(player, new OpenStructureListPayload(updatedList));
                });
            }
        );

        // C→S : création d'un village
        ServerPlayNetworking.registerGlobalReceiver(CreateVillagePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player = ctx.player();
                String cultureId     = payload.cultureId();
                String villageTypeId = payload.villageTypeId();
                net.minecraft.core.BlockPos goldPos = payload.goldPos();

                ctx.server().execute(() -> handleCreateVillage(player, cultureId, villageTypeId, goldPos));
            }
        );

        // ── Import Table handlers ─────────────────────────────────────────────

        // C→S : création d'une nouvelle structure
        ServerPlayNetworking.registerGlobalReceiver(ImportTableCreatePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player       = ctx.player();
                BlockPos tablePos         = payload.tablePos();
                String name               = payload.structureName();
                ImportTableConfig config  = payload.config();

                ctx.server().execute(() -> {
                    ServerLevel level = player.level();
                    if (!(level.getBlockEntity(tablePos) instanceof ImportTableBlockEntity be)) return;

                    if (name.isBlank() || !name.contains("/")) {
                        player.sendSystemMessage(Component.translatable("chat.millenaire-new-age.error_prefix")
                            .append("Format invalide — utiliser 'culture/nom'"));
                        return;
                    }

                    be.setStructureId(name);
                    be.setConfig(config);

                    WoolMarkerManager.placeMarkers(level, tablePos, config);

                    level.sendBlockUpdated(tablePos, level.getBlockState(tablePos),
                                           level.getBlockState(tablePos), 3);

                    List<String> structures = StructureSaveManager.listStructures(ctx.server());
                    ServerPlayNetworking.send(player, new OpenImportTablePayload(
                        tablePos, true, name, config, be.isParticlesEnabled(), structures));
                });
            }
        );

        // C→S : mise à jour des dimensions
        ServerPlayNetworking.registerGlobalReceiver(ImportTableUpdatePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player       = ctx.player();
                BlockPos tablePos         = payload.tablePos();
                String name               = payload.structureName();
                ImportTableConfig newCfg  = payload.config();

                ctx.server().execute(() -> {
                    ServerLevel level = player.level();
                    if (!(level.getBlockEntity(tablePos) instanceof ImportTableBlockEntity be)) return;

                    ImportTableConfig oldCfg = be.getConfig();
                    if (oldCfg != null) {
                        WoolMarkerManager.removeMarkers(level, tablePos, oldCfg);
                    }

                    be.setStructureId(name);
                    be.setConfig(newCfg);

                    WoolMarkerManager.placeMarkers(level, tablePos, newCfg);

                    level.sendBlockUpdated(tablePos, level.getBlockState(tablePos),
                                           level.getBlockState(tablePos), 3);

                    List<String> structures = StructureSaveManager.listStructures(ctx.server());
                    ServerPlayNetworking.send(player, new OpenImportTablePayload(
                        tablePos, true, name, newCfg, be.isParticlesEnabled(), structures));
                });
            }
        );

        // C→S : sauvegarde de la structure
        ServerPlayNetworking.registerGlobalReceiver(ImportTableSavePayload.ID,
            (payload, ctx) -> {
                ServerPlayer player = ctx.player();
                BlockPos tablePos   = payload.tablePos();

                ctx.server().execute(() -> {
                    ServerLevel level = player.level();
                    if (!(level.getBlockEntity(tablePos) instanceof ImportTableBlockEntity be)) return;

                    if (!be.isConfigured()) {
                        player.sendSystemMessage(Component.translatable("chat.millenaire-new-age.error_prefix")
                            .append("Table non configurée"));
                        return;
                    }

                    String structureId = be.getStructureId();
                    ImportTableConfig config = be.getConfig();
                    try {
                        assert config != null;
                        assert structureId != null;
                        StructureSaveManager.saveStructureFromTable(ctx.server(), level,
                                                                    tablePos, config, structureId);
                        player.sendSystemMessage(Component.translatable("chat.millenaire-new-age.success_prefix")
                            .append(Component.translatable("chat.millenaire-new-age.creator.save_success")));
                    } catch (java.io.IOException e) {
                        player.sendSystemMessage(Component.translatable("chat.millenaire-new-age.error_prefix")
                            .append(Component.translatable("chat.millenaire-new-age.creator.save_error", e.getMessage())));
                    }
                });
            }
        );

        // C→S : import d'une structure existante
        ServerPlayNetworking.registerGlobalReceiver(ImportTableImportPayload.ID,
            (payload, ctx) -> {
                ServerPlayer player    = ctx.player();
                BlockPos tablePos      = payload.tablePos();
                String structureId     = payload.structureId();

                ctx.server().execute(() -> {
                    ServerLevel level = player.level();
                    if (!(level.getBlockEntity(tablePos) instanceof ImportTableBlockEntity be)) return;

                    StructureTemplate template = StructureSaveManager.loadTemplate(ctx.server(), structureId);
                    if (template == null) {
                        player.sendSystemMessage(Component.translatable("chat.millenaire-new-age.error_prefix")
                            .append(Component.translatable("chat.millenaire-new-age.creator.not_found", structureId)));
                        return;
                    }

                    ImportTableConfig config = StructureSaveManager.loadImportTableConfig(structureId);
                    if (config == null) {
                        Vec3i size = template.getSize();
                        config = new ImportTableConfig(
                            size.getX(), size.getZ(), size.getY(), 0, -1);
                    }

                    // Retirer les anciens marqueurs si configuré
                    ImportTableConfig oldCfg = be.getConfig();
                    if (oldCfg != null) {
                        WoolMarkerManager.removeMarkers(level, tablePos, oldCfg);
                    }

                    // Placer la structure (la table est en dehors de la zone, elle ne sera pas écrasée)
                    BlockPos origin = StructureSaveManager.computePlacementOrigin(tablePos, config);
                    StructureSaveManager.placeStructure(ctx.server(), level, structureId,
                                                        origin, Mirror.NONE, Rotation.NONE);

                    // Placer les marqueurs laine autour de la zone
                    WoolMarkerManager.placeMarkers(level, tablePos, config);

                    // Mettre à jour le BlockEntity
                    be.setStructureId(structureId);
                    be.setConfig(config);
                    be.setChanged();

                    level.sendBlockUpdated(tablePos, level.getBlockState(tablePos),
                                           level.getBlockState(tablePos), 3);

                    List<String> structures = StructureSaveManager.listStructures(ctx.server());
                    ServerPlayNetworking.send(player, new OpenImportTablePayload(
                        tablePos, true, structureId, config, be.isParticlesEnabled(), structures));
                });
            }
        );

        // C→S : toggle particules
        ServerPlayNetworking.registerGlobalReceiver(ImportTableToggleParticlesPayload.ID,
            (payload, ctx) -> {
                ServerPlayer player = ctx.player();
                BlockPos tablePos   = payload.tablePos();

                ctx.server().execute(() -> {
                    ServerLevel level = player.level();
                    if (!(level.getBlockEntity(tablePos) instanceof ImportTableBlockEntity be)) return;
                    be.toggleParticles();
                    level.sendBlockUpdated(tablePos, level.getBlockState(tablePos),
                                           level.getBlockState(tablePos), 3);
                });
            }
        );
    }

    // ── Handlers serveur ─────────────────────────────────────────────────────

    private static void handleDeleteStructure(ServerPlayer player, String structureId) {
        String sanitized = structureId.replace(':', '/');
        java.nio.file.Path outputDir = StructureSaveManager.creatorOutputDir().resolve("creator_structures");
        java.nio.file.Path nbtPath = outputDir.resolve(sanitized + ".nbt");

        try {
            boolean deleted = java.nio.file.Files.deleteIfExists(nbtPath);

            if (deleted) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.success_prefix")
                    .append(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.creator.deleted", structureId))
                );
            }
        } catch (java.io.IOException e) {
            MillenaireNewAge.LOGGER.error("Erreur suppression structure : {}", e.getMessage());
        }
    }

    private static void handleSelectStructure(ServerPlayer player, String structureId) {
        // Charger les positions de blocs pour le preview
        List<net.minecraft.core.BlockPos> blocks =
            StructureSaveManager.loadBlockPositions(player.level().getServer(), structureId);

        if (blocks.isEmpty()) {
            MillenaireNewAge.LOGGER.warn("Structure '{}' : aucun bloc trouvé pour le preview.", structureId);
        }

        // Calculer la taille depuis les positions max
        Vec3i size = computeSize(blocks);

        // Configurer la baguette de placement dans la main principale
        ItemStack stack = new ItemStack(MillItems.STRUCTURE_PLACER);
        StructurePlacerItem.setStructureId(stack, structureId);
        StructurePlacerItem.setRotation(stack, 0);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);

        // Envoyer les données de preview au client
        ServerPlayNetworking.send(player, new StructurePreviewPayload(structureId, blocks, size));

        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.success_prefix")
            .append(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.creator.placer_configured", structureId))
        );
    }

    private static void handleCreateVillage(ServerPlayer player,
                                             String cultureId, String villageTypeId,
                                             net.minecraft.core.BlockPos goldPos) {
        ServerLevel level = player.level();

        // Vérifier la distance avec les villages existants
        net.minecraft.network.chat.Component spacingError = com.mat37dev.village.VillagePlacer.checkSpacing(level, goldPos);
        if (spacingError != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.error_prefix")
                .append(spacingError));
            return;
        }

        // Vérifier la présence de blocs dangereux près du bâtiment principal
        net.minecraft.network.chat.Component dangerError = com.mat37dev.village.VillagePlacer.checkDanger(
            player.level().getServer(), level, cultureId, villageTypeId, goldPos);
        if (dangerError != null) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.error_prefix")
                .append(dangerError));
            return;
        }

        VillagePlacer.placeVillage(player.level().getServer(), level, cultureId, villageTypeId, goldPos)
            .ifPresentOrElse(
                village -> {
                    player.sendSystemMessage(
                        net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.success_prefix")
                            .append(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.village.created",
                                village.getName(), village.getBuildings().size()))
                    );

                    // Téléporter le joueur au nord du bâtiment central
                    Optional<Culture> culture = CultureRegistry.get(cultureId);
                    Optional<VillageType> vt = culture.flatMap(c -> c.getVillageType(villageTypeId));
                    if (culture.isPresent() && vt.isPresent()) {
                        BlockPos safePos = VillagePlacer.findSafeTeleportPos(
                            player.level().getServer(), level, culture.get(), vt.get(), goldPos);
                        player.teleportTo(safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5);
                    }
                },
                () -> player.sendSystemMessage(
                    net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.error_prefix")
                        .append(net.minecraft.network.chat.Component.translatable("chat.millenaire-new-age.village.create_fail"))
                )
            );
    }

    public static Vec3i computeSizePublic(List<net.minecraft.core.BlockPos> blocks) {
        return computeSize(blocks);
    }

    private static Vec3i computeSize(List<net.minecraft.core.BlockPos> blocks) {
        if (blocks.isEmpty()) return new Vec3i(1, 1, 1);
        int maxX = 1, maxY = 1, maxZ = 1;
        for (net.minecraft.core.BlockPos p : blocks) {
            if (p.getX() + 1 > maxX) maxX = p.getX() + 1;
            if (p.getY() + 1 > maxY) maxY = p.getY() + 1;
            if (p.getZ() + 1 > maxZ) maxZ = p.getZ() + 1;
        }
        return new Vec3i(maxX, maxY, maxZ);
    }
}
