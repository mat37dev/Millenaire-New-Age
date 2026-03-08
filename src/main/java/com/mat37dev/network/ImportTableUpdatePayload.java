package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import com.mat37dev.creator.ImportTableConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C→S : met à jour les dimensions d'une structure existante.
 */
public record ImportTableUpdatePayload(
        BlockPos tablePos,
        String structureName,
        ImportTableConfig config
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImportTableUpdatePayload> ID = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(MillenaireNewAge.MOD_ID, "import_table_update")
    );

    public static final StreamCodec<FriendlyByteBuf, ImportTableUpdatePayload> CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.tablePos());
                buf.writeUtf(p.structureName());
                buf.writeVarInt(p.config().width());
                buf.writeVarInt(p.config().length());
                buf.writeVarInt(p.config().height());
                buf.writeVarInt(p.config().depth());
                buf.writeVarInt(p.config().floorHeight());
            },
            buf -> new ImportTableUpdatePayload(
                buf.readBlockPos(),
                buf.readUtf(),
                new ImportTableConfig(
                    buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt(), buf.readVarInt(),
                    buf.readVarInt()
                )
            )
        );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
