package com.mat37dev.creator;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Gère la pose et le retrait des marqueurs laine autour d'une Import Table.
 *
 * <p>La table se situe EN DEHORS de la zone de capture, au-dessus du coin bas-gauche
 * (position X=min−1, Z=min−1 de la zone). La zone réelle commence à tablePos+1 en X et Z.
 * Les marqueurs sont placés à {@code worldFloorY = tablePos.y + config.floorHeight()}.</p>
 *
 * <p>Disposition des marqueurs (la table est à la position du coin bas-gauche) :</p>
 * <ul>
 *   <li>4 coins : laine noire — dont un directement sous la table</li>
 *   <li>Bords sur X (z=tableZ et z=tableZ+length+1) : alternance rouge/blanche</li>
 *   <li>Bords sur Z (x=tableX et x=tableX+width+1) : alternance rouge/blanche</li>
 * </ul>
 *
 * <p>La zone de capture va de {@code (tx+1, ty-depth, tz+1)} à
 * {@code (tx+width, ty+height-1, tz+length)} — la table n'est jamais incluse.</p>
 */
public class WoolMarkerManager {

    private static final BlockState BLACK  = Blocks.BLACK_WOOL.defaultBlockState();
    private static final BlockState RED    = Blocks.RED_WOOL.defaultBlockState();
    private static final BlockState WHITE  = Blocks.WHITE_WOOL.defaultBlockState();
    private static final BlockState AIR    = Blocks.AIR.defaultBlockState();

    /**
     * Place les marqueurs laine autour de la zone définie par la config.
     *
     * @param level    dimension serveur
     * @param tablePos position de la Import Table dans le monde
     * @param config   configuration de la structure
     */
    public static void placeMarkers(ServerLevel level, BlockPos tablePos, ImportTableConfig config) {
        int tx      = tablePos.getX();
        int tz      = tablePos.getZ();
        int floorY  = tablePos.getY() + config.floorHeight();
        int width   = config.width();
        int length  = config.length();

        // --- 4 coins (laine noire) ---
        // Coin bas-gauche : directement sous la table (tx, tz)
        setMarker(level, tx,             floorY, tz,              BLACK);
        setMarker(level, tx + width + 1, floorY, tz,              BLACK);
        setMarker(level, tx,             floorY, tz + length + 1, BLACK);
        setMarker(level, tx + width + 1, floorY, tz + length + 1, BLACK);

        // --- Bords sur X (z=tz et z=tz+length+1), de tx+1 à tx+width ---
        for (int x = 1; x <= width; x++) {
            BlockState color = (x % 2 == 0) ? RED : WHITE;
            setMarker(level, tx + x, floorY, tz,              color);
            setMarker(level, tx + x, floorY, tz + length + 1, color);
        }

        // --- Bords sur Z (x=tx et x=tx+width+1), de tz+1 à tz+length ---
        for (int z = 1; z <= length; z++) {
            BlockState color = (z % 2 == 0) ? RED : WHITE;
            setMarker(level, tx,             floorY, tz + z, color);
            setMarker(level, tx + width + 1, floorY, tz + z, color);
        }
    }

    /**
     * Retire les marqueurs laine aux positions attendues.
     * Ne touche que les blocs de laine ; les autres blocs sont laissés intacts.
     *
     * @param level    dimension serveur
     * @param tablePos position de la Import Table
     * @param config   configuration de la structure (pour calculer les positions)
     */
    public static void removeMarkers(ServerLevel level, BlockPos tablePos, ImportTableConfig config) {
        for (BlockPos pos : getExpectedMarkerPositions(tablePos, config)) {
            if (isWool(level.getBlockState(pos))) {
                level.setBlock(pos, AIR, 3);
            }
        }
    }

    /**
     * Vide la zone de capture (blocs à l'intérieur, sans les marqueurs).
     *
     * @param level    dimension serveur
     * @param tablePos position de la Import Table
     * @param config   configuration de la structure
     */
    public static void clearZone(ServerLevel level, BlockPos tablePos, ImportTableConfig config) {
        int tx     = tablePos.getX() + 1; // zone commence à tx+1 (table est en dehors)
        int ty     = tablePos.getY();
        int tz     = tablePos.getZ() + 1; // zone commence à tz+1 (table est en dehors)
        int minY   = ty - config.depth();
        int maxY   = ty + config.height() - 1;

        for (int x = tx; x < tx + config.width(); x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = tz; z < tz + config.length(); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, AIR, 3);
                    }
                }
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static void setMarker(ServerLevel level, int x, int y, int z, BlockState state) {
        // Outil créateur : on place toujours le marqueur (même sur terrain solide)
        level.setBlock(new BlockPos(x, y, z), state, 3);
    }

    private static boolean isWool(BlockState state) {
        return state.getBlock() == Blocks.BLACK_WOOL
            || state.getBlock() == Blocks.RED_WOOL
            || state.getBlock() == Blocks.WHITE_WOOL;
    }

    private static List<BlockPos> getExpectedMarkerPositions(BlockPos tablePos, ImportTableConfig config) {
        int tx     = tablePos.getX();
        int tz     = tablePos.getZ();
        int floorY = tablePos.getY() + config.floorHeight();
        int width  = config.width();
        int length = config.length();

        List<BlockPos> positions = new ArrayList<>();

        // Coins
        positions.add(new BlockPos(tx,             floorY, tz));
        positions.add(new BlockPos(tx + width + 1, floorY, tz));
        positions.add(new BlockPos(tx,             floorY, tz + length + 1));
        positions.add(new BlockPos(tx + width + 1, floorY, tz + length + 1));

        // Bords X
        for (int x = 1; x <= width; x++) {
            positions.add(new BlockPos(tx + x, floorY, tz));
            positions.add(new BlockPos(tx + x, floorY, tz + length + 1));
        }

        // Bords Z
        for (int z = 1; z <= length; z++) {
            positions.add(new BlockPos(tx,             floorY, tz + z));
            positions.add(new BlockPos(tx + width + 1, floorY, tz + z));
        }

        return positions;
    }
}
