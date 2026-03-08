package com.mat37dev.creator;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bloc Import Table : interactif, ouvre la GUI de gestion des structures créateur.
 *
 * <p>Remplace l'ancien workflow baguette + commande. En faisant clic droit dessus,
 * le serveur envoie un {@link com.mat37dev.network.OpenImportTablePayload} au joueur
 * pour ouvrir l'écran côté client.</p>
 */
public class ImportTableBlock extends BaseEntityBlock {

    public static final MapCodec<ImportTableBlock> CODEC = simpleCodec(ImportTableBlock::new);

    public ImportTableBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @NotNull RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    // ── BlockEntity ──────────────────────────────────────────────────────────

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImportTableBlockEntity(pos, state);
    }

    // ── Interaction ──────────────────────────────────────────────────────────

    @Override
    public @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                     Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        if (!(level.getBlockEntity(pos) instanceof ImportTableBlockEntity be)) {
            return InteractionResult.PASS;
        }

        // Envoyer l'état actuel au client pour ouvrir la GUI
        java.util.List<String> structures =
            com.mat37dev.creator.StructureSaveManager.listStructures(level.getServer());
        net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer,
            new com.mat37dev.network.OpenImportTablePayload(
                pos,
                be.isConfigured(),
                be.getStructureId(),
                be.getConfig(),
                be.isParticlesEnabled(),
                structures
            )
        );

        return InteractionResult.SUCCESS;
    }
}
