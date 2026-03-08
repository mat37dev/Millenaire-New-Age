package com.mat37dev.network;

import com.mat37dev.MillenaireNewAge;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * C→S : active/désactive les particules d'une Import Table.
 */
public record ImportTableToggleParticlesPayload(BlockPos tablePos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ImportTableToggleParticlesPayload> ID = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(MillenaireNewAge.MOD_ID, "import_table_toggle_particles")
    );

    public static final StreamCodec<FriendlyByteBuf, ImportTableToggleParticlesPayload> CODEC =
        StreamCodec.of(
            (buf, p) -> buf.writeBlockPos(p.tablePos()),
            buf -> new ImportTableToggleParticlesPayload(buf.readBlockPos())
        );

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
