package com.mat37dev.creator;

import com.mat37dev.init.MillBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity de l'Import Table.
 *
 * <p>Stocke :</p>
 * <ul>
 *   <li>{@code structureId} — ID de la structure associée (null si aucune)</li>
 *   <li>{@code config} — dimensions/configuration (null si aucune structure)</li>
 *   <li>{@code particlesEnabled} — affichage des particules autour de la zone</li>
 * </ul>
 */
public class ImportTableBlockEntity extends BlockEntity {

    @Nullable
    private String structureId = null;

    @Nullable
    private ImportTableConfig config = null;

    private boolean particlesEnabled = false;

    public ImportTableBlockEntity(BlockPos pos, BlockState state) {
        super(MillBlockEntities.IMPORT_TABLE_ENTITY, pos, state);
    }

    // ── Sérialisation ─────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (structureId != null) {
            output.putString("structure_id", structureId);
        }
        if (config != null) {
            ValueOutput cfg = output.child("config");
            cfg.putInt("width",        config.width());
            cfg.putInt("length",       config.length());
            cfg.putInt("height",       config.height());
            cfg.putInt("depth",        config.depth());
            cfg.putInt("floor_height", config.floorHeight());
        }
        output.putBoolean("particles", particlesEnabled);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.structureId = input.getStringOr("structure_id", null);

        ValueInput cfg = input.childOrEmpty("config");
        if (cfg.getIntOr("width", 0) > 0) {
            this.config = new ImportTableConfig(
                cfg.getIntOr("width",        10),
                cfg.getIntOr("length",       10),
                cfg.getIntOr("height",        5),
                cfg.getIntOr("depth",         1),
                cfg.getIntOr("floor_height", -1)
            );
        } else {
            this.config = null;
        }
        this.particlesEnabled = input.getBooleanOr("particles", false);
    }

    // ── Getters / Setters ─────────────────────────────────────────────────────

    @Nullable
    public String getStructureId() { return structureId; }

    @Nullable
    public ImportTableConfig getConfig() { return config; }

    public boolean isParticlesEnabled() { return particlesEnabled; }

    public void setStructureId(@Nullable String id) {
        this.structureId = id;
        setChanged();
    }

    public void setConfig(@Nullable ImportTableConfig cfg) {
        this.config = cfg;
        setChanged();
    }

    public void toggleParticles() {
        this.particlesEnabled = !this.particlesEnabled;
        setChanged();
    }

    public boolean isConfigured() {
        return structureId != null && config != null;
    }

}
