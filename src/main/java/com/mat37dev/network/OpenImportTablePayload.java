package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import com.mat37dev.creator.ImportTableConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * S→C : ouvre la GUI Import Table avec l'état actuel du bloc.
 *
 * <p>{@code availableStructures} contient la liste des structures disponibles
 * pour le mode IMPORT_SELECT, envoyée en une seule fois pour éviter un aller-retour réseau.</p>
 */
public record OpenImportTablePayload(
        BlockPos tablePos,
        boolean isConfigured,
        @Nullable String structureId,
        @Nullable ImportTableConfig config,
        boolean particlesEnabled,
        List<String> availableStructures
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenImportTablePayload> ID = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(MillenaireNewAge.MOD_ID, "open_import_table")
    );

    public static final StreamCodec<FriendlyByteBuf, OpenImportTablePayload> CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.tablePos());
                buf.writeBoolean(p.isConfigured());
                buf.writeBoolean(p.structureId() != null);
                if (p.structureId() != null) buf.writeUtf(p.structureId());
                buf.writeBoolean(p.config() != null);
                if (p.config() != null) {
                    buf.writeVarInt(p.config().width());
                    buf.writeVarInt(p.config().length());
                    buf.writeVarInt(p.config().height());
                    buf.writeVarInt(p.config().depth());
                    buf.writeVarInt(p.config().floorHeight());
                }
                buf.writeBoolean(p.particlesEnabled());
                buf.writeCollection(p.availableStructures(), FriendlyByteBuf::writeUtf);
            },
            buf -> {
                BlockPos pos        = buf.readBlockPos();
                boolean configured  = buf.readBoolean();
                String structureId  = buf.readBoolean() ? buf.readUtf() : null;
                ImportTableConfig config = null;
                if (buf.readBoolean()) {
                    config = new ImportTableConfig(
                        buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt(), buf.readVarInt(),
                        buf.readVarInt()
                    );
                }
                boolean particles          = buf.readBoolean();
                List<String> structures    = buf.readList(FriendlyByteBuf::readUtf);
                return new OpenImportTablePayload(pos, configured, structureId, config, particles, structures);
            }
        );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
