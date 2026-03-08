package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C→S : sauvegarde la structure associée à une Import Table.
 */
public record ImportTableSavePayload(BlockPos tablePos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImportTableSavePayload> ID = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(MillenaireNewAge.MOD_ID, "import_table_save")
    );

    public static final StreamCodec<FriendlyByteBuf, ImportTableSavePayload> CODEC =
        StreamCodec.of(
            (buf, p) -> buf.writeBlockPos(p.tablePos()),
            buf -> new ImportTableSavePayload(buf.readBlockPos())
        );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
