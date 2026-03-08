package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C→S : importe une structure existante dans une Import Table.
 */
public record ImportTableImportPayload(
        BlockPos tablePos,
        String structureId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImportTableImportPayload> ID = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(MillenaireNewAge.MOD_ID, "import_table_import")
    );

    public static final StreamCodec<FriendlyByteBuf, ImportTableImportPayload> CODEC =
        StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.tablePos());
                buf.writeUtf(p.structureId());
            },
            buf -> new ImportTableImportPayload(buf.readBlockPos(), buf.readUtf())
        );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
