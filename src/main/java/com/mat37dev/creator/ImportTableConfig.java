package com.mat37dev.creator;

/**
 * Configuration d'une structure associée à une Import Table.
 *
 * <p>Toutes les dimensions sont en blocs, relativement à la position de la table.</p>
 *
 * @param width      largeur (axe X)
 * @param length     longueur (axe Z)
 * @param height     hauteur au-dessus de la table
 * @param depth      profondeur en dessous de la table
 * @param floorHeight offset Y du sol relatif à la table (défaut -1 = sol un bloc sous la table)
 */
public record ImportTableConfig(
        int width,
        int length,
        int height,
        int depth,
        int floorHeight
) {
    /** Config par défaut utilisée quand les champs ne sont pas renseignés. */
    public static ImportTableConfig defaults() {
        return new ImportTableConfig(10, 10, 5, 1, -1);
    }
}
