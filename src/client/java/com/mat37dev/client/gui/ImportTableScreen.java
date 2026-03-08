package com.mat37dev.client.gui;

import com.mat37dev.MillenaireNewAgeClient;
import com.mat37dev.creator.ImportTableConfig;
import com.mat37dev.network.ImportTableCreatePayload;
import com.mat37dev.network.ImportTableImportPayload;
import com.mat37dev.network.ImportTableSavePayload;
import com.mat37dev.network.ImportTableToggleParticlesPayload;
import com.mat37dev.network.ImportTableUpdatePayload;
import com.mat37dev.network.OpenImportTablePayload;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Écran Import Table — 4 états :
 * <ul>
 *   <li>WELCOME : non configurée, boutons Créer / Importer</li>
 *   <li>CREATE  : formulaire de création</li>
 *   <li>IMPORT_SELECT : liste des structures existantes (onglets culture + scroll)</li>
 *   <li>CONFIGURE : formulaire de mise à jour + sauvegarde + particules</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class ImportTableScreen extends Screen {

    private enum State { WELCOME, CREATE, IMPORT_SELECT, CONFIGURE }

    // ── Données du payload ────────────────────────────────────────────────────

    private final BlockPos tablePos;
    private final List<String> availableStructures;
    private boolean particlesEnabled;
    @Nullable private final String currentStructureId;
    @Nullable private final ImportTableConfig currentConfig;

    // ── État interne ──────────────────────────────────────────────────────────

    private State state;

    // Formulaire
    private EditBox nameField;
    private EditBox widthField, lengthField, heightField, depthField, floorHeightField;

    // Liste import
    private final List<String> cultures;
    @Nullable private String selectedCulture    = null;
    @Nullable private String selectedStructure  = null;
    private int listScrollOffset = 0;

    // Boutons à suivre pour activer/désactiver
    @Nullable private Button importBtn;
    @Nullable private Button particlesBtn;

    // ── Layout ────────────────────────────────────────────────────────────────

    private static final int W      = 240;
    private static final int H      = 220;
    private static final int MARGIN = 10;
    private static final int FIELD_H = 16;
    private static final int LABEL_W = 72;
    private static final int ITEM_H  = 16;
    private static final int TAB_H   = 18;

    private int guiLeft, guiTop;
    /** Zone de la liste (calculée dans init) */
    private int listX, listY, listWidth, listHeight;

    // ── Constructeur ──────────────────────────────────────────────────────────

    public ImportTableScreen(OpenImportTablePayload payload) {
        super(Component.translatable("gui.millenaire-new-age.import_table.title"));
        this.tablePos           = payload.tablePos();
        this.availableStructures = new ArrayList<>(payload.availableStructures());
        this.particlesEnabled   = payload.particlesEnabled();
        this.currentStructureId = payload.structureId();
        this.currentConfig      = payload.config();
        this.state              = payload.isConfigured() ? State.CONFIGURE : State.WELCOME;

        // Cultures extraites des IDs de structures
        Set<String> cultureSet = new LinkedHashSet<>();
        for (String s : availableStructures) {
            cultureSet.add(s.contains("/") ? s.substring(0, s.indexOf('/')) : "other");
        }
        this.cultures = new ArrayList<>(cultureSet);
        if (!cultures.isEmpty()) this.selectedCulture = cultures.getFirst();
    }

    // ── Init ─────────────────────────────────────────────────────────────────

    @Override
    protected void init() {
        guiLeft = (this.width  - W) / 2;
        guiTop  = (this.height - H) / 2;

        clearWidgets();

        switch (state) {
            case WELCOME       -> initWelcome();
            case CREATE        -> initForm(false);
            case IMPORT_SELECT -> initImportSelect();
            case CONFIGURE     -> initForm(true);
        }
    }

    private void initWelcome() {
        int btnW = 100;
        int btnY = guiTop + H / 2 - 10;

        addRenderableWidget(Button.builder(
            Component.translatable("gui.millenaire-new-age.import_table.create"),
            btn -> { state = State.CREATE; rebuildWidgets(); }
        ).bounds(guiLeft + W / 2 - btnW - 5, btnY, btnW, 20).build());

        addRenderableWidget(Button.builder(
            Component.translatable("gui.millenaire-new-age.import_table.import"),
            btn -> { state = State.IMPORT_SELECT; selectedStructure = null; listScrollOffset = 0; rebuildWidgets(); }
        ).bounds(guiLeft + W / 2 + 5, btnY, btnW, 20).build());
    }

    private void initForm(boolean configure) {
        int fieldW  = 60;
        int fieldX  = guiLeft + LABEL_W + MARGIN + 2;
        int y       = guiTop + 20;

        // Champ Nom
        nameField = new EditBox(this.font, fieldX, y, W - LABEL_W - MARGIN * 2 - 12, FIELD_H,
                                Component.translatable("gui.millenaire-new-age.import_table.name"));
        nameField.setMaxLength(64);
        if (configure && currentStructureId != null) nameField.setValue(currentStructureId);
        addRenderableWidget(nameField);
        y += FIELD_H + 4;

        // Largeur
        widthField = makeIntField(fieldX, y, fieldW,
                                  configure && currentConfig != null ? currentConfig.width() : 10);
        addRenderableWidget(widthField);
        y += FIELD_H + 4;

        // Longueur
        lengthField = makeIntField(fieldX, y, fieldW,
                                   configure && currentConfig != null ? currentConfig.length() : 10);
        addRenderableWidget(lengthField);
        y += FIELD_H + 4;

        // Hauteur
        heightField = makeIntField(fieldX, y, fieldW,
                                   configure && currentConfig != null ? currentConfig.height() : 5);
        addRenderableWidget(heightField);
        y += FIELD_H + 4;

        // Profondeur
        depthField = makeIntField(fieldX, y, fieldW,
                                  configure && currentConfig != null ? currentConfig.depth() : 1);
        addRenderableWidget(depthField);
        y += FIELD_H + 4;

        // Hauteur sol
        floorHeightField = new EditBox(this.font, fieldX, y, fieldW, FIELD_H, Component.literal(""));
        floorHeightField.setMaxLength(5);
        floorHeightField.setValue(configure && currentConfig != null
                                  ? String.valueOf(currentConfig.floorHeight()) : "-1");
        addRenderableWidget(floorHeightField);

        // ── Boutons ──────────────────────────────────────────────────────────
        int btnY = guiTop + H - 26;

        if (!configure) {
            // État CREATE : Créer + Retour
            int btnW = 90;
            addRenderableWidget(Button.builder(
                Component.translatable("gui.millenaire-new-age.import_table.create"),
                btn -> onCreateSubmit()
            ).bounds(guiLeft + W / 2 - btnW - 4, btnY, btnW, 20).build());

            addRenderableWidget(Button.builder(
                Component.translatable("gui.millenaire-new-age.import_table.back"),
                btn -> { state = State.WELCOME; rebuildWidgets(); }
            ).bounds(guiLeft + W / 2 + 4, btnY, btnW, 20).build());

        } else {
            // État CONFIGURE : Mettre à jour + Sauvegarder + Particules
            addRenderableWidget(Button.builder(
                Component.translatable("gui.millenaire-new-age.import_table.update_dimensions"),
                btn -> onUpdateDimensions()
            ).bounds(guiLeft + MARGIN, btnY, 95, 20).build());

            addRenderableWidget(Button.builder(
                Component.translatable("gui.millenaire-new-age.import_table.save"),
                btn -> onSave()
            ).bounds(guiLeft + MARGIN + 99, btnY, 60, 20).build());

            particlesBtn = Button.builder(
                particlesLabel(),
                btn -> onToggleParticles()
            ).bounds(guiLeft + MARGIN + 163, btnY, 67, 20).build();
            addRenderableWidget(particlesBtn);
        }
    }

    private void initImportSelect() {
        int tabY    = guiTop + 20;
        listX       = guiLeft + MARGIN;
        listY       = tabY + TAB_H + 4;
        listWidth   = W - MARGIN * 2;
        listHeight  = H - (listY - guiTop) - 30;

        int btnY = guiTop + H - 26;
        int btnW = 90;

        Button imp = Button.builder(
            Component.translatable("gui.millenaire-new-age.import_table.import"),
            btn -> onImport()
        ).bounds(guiLeft + W / 2 - btnW - 4, btnY, btnW, 20).build();
        imp.active = (selectedStructure != null);
        this.importBtn = imp;
        addRenderableWidget(imp);

        addRenderableWidget(Button.builder(
            Component.translatable("gui.millenaire-new-age.import_table.back"),
            btn -> { state = State.WELCOME; rebuildWidgets(); }
        ).bounds(guiLeft + W / 2 + 4, btnY, btnW, 20).build());
    }

    // ── Rendu ─────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {
        super.renderBackground(g, mx, my, pt);
        // Fond principal
        g.fill(guiLeft,     guiTop,      guiLeft + W, guiTop + H, 0xEE1A1A2E);
        // Barre de titre
        g.fill(guiLeft,     guiTop,      guiLeft + W, guiTop + 14, 0xFF222244);
        // Bordures
        g.fill(guiLeft,     guiTop,      guiLeft + W,     guiTop + 1,      0xFF4455AA);
        g.fill(guiLeft,     guiTop + H - 1, guiLeft + W, guiTop + H,      0xFF4455AA);
        g.fill(guiLeft,     guiTop,      guiLeft + 1,     guiTop + H,      0xFF4455AA);
        g.fill(guiLeft + W - 1, guiTop, guiLeft + W,     guiTop + H,      0xFF4455AA);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        // renderBackground est appelé automatiquement par MC avant render() — ne pas le rappeler ici
        g.drawCenteredString(this.font, this.title, this.width / 2, guiTop + 3, 0xFFFFFFFF);

        switch (state) {
            case WELCOME       -> { /* titre centré suffisant */ }
            case CREATE        -> renderFormLabels(g, false);
            case CONFIGURE     -> renderFormLabels(g, true);
            case IMPORT_SELECT -> renderImportSelect(g, mx, my);
        }

        super.render(g, mx, my, pt);
    }

    private void renderFormLabels(GuiGraphics g, boolean configure) {
        int lx    = guiLeft + MARGIN;
        int y     = guiTop + 20;
        int color = 0xFFCCCCCC;

        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.name"),         lx, y + 4, color); y += FIELD_H + 4;
        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.width"),        lx, y + 4, color); y += FIELD_H + 4;
        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.length"),       lx, y + 4, color); y += FIELD_H + 4;
        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.height"),       lx, y + 4, color); y += FIELD_H + 4;
        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.depth"),        lx, y + 4, color); y += FIELD_H + 4;
        g.drawString(this.font, Component.translatable("gui.millenaire-new-age.import_table.floor_height"), lx, y + 4, color);
    }

    private void renderImportSelect(GuiGraphics g, int mx, int my) {
        // Onglets cultures
        int tabY = guiTop + 20;
        if (!cultures.isEmpty()) {
            int tabW = Math.min(70, (W - MARGIN * 2) / cultures.size());
            for (int i = 0; i < cultures.size(); i++) {
                int tx  = guiLeft + MARGIN + i * tabW;
                boolean sel = cultures.get(i).equals(selectedCulture);
                boolean hov = mx >= tx && mx < tx + tabW && my >= tabY && my < tabY + TAB_H;
                g.fill(tx, tabY, tx + tabW, tabY + TAB_H,
                       sel ? 0xFF4477CC : (hov ? 0xFF3A3A3A : 0xFF2A2A2A));
                g.fill(tx, tabY, tx + tabW, tabY + 1, 0xFF666688);
                g.drawCenteredString(this.font, Component.literal(capitalize(cultures.get(i))),
                    tx + tabW / 2, tabY + (TAB_H - 8) / 2,
                    sel ? 0xFFFFFFFF : 0xFFAAAAAA);
            }
        }

        // Fond de la liste
        g.fill(listX - 1, listY - 1, listX + listWidth + 1, listY + listHeight + 1, 0xFF2A2A2A);
        g.fill(listX, listY, listX + listWidth, listY + listHeight, 0xFF1A1A1A);

        // Items
        g.enableScissor(listX, listY, listX + listWidth, listY + listHeight);
        List<String> filtered = getFilteredStructures();
        for (int i = 0; i < filtered.size(); i++) {
            String s   = filtered.get(i);
            int iy     = listY + i * ITEM_H - listScrollOffset;
            if (iy + ITEM_H <= listY || iy >= listY + listHeight) continue;

            boolean sel = s.equals(selectedStructure);
            boolean hov = mx >= listX && mx < listX + listWidth && my >= iy && my < iy + ITEM_H;

            if      (sel)        g.fill(listX, iy, listX + listWidth, iy + ITEM_H, 0xFF2255AA);
            else if (hov)        g.fill(listX, iy, listX + listWidth, iy + ITEM_H, 0x55FFFFFF);
            else if (i % 2 == 0) g.fill(listX, iy, listX + listWidth, iy + ITEM_H, 0x15FFFFFF);

            String label = s.contains("/") ? s.substring(s.indexOf('/') + 1) : s;
            g.drawString(this.font, label, listX + 4, iy + 4, sel ? 0xFFFFFFFF : 0xFFDDDDDD);
        }
        g.disableScissor();
    }

    // ── Saisie ────────────────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        if (state == State.IMPORT_SELECT) {
            double mx = event.x(), my = event.y();

            // Clic sur un onglet
            int tabY = guiTop + 20;
            if (!cultures.isEmpty()) {
                int tabW = Math.min(70, (W - MARGIN * 2) / cultures.size());
                if (my >= tabY && my < tabY + TAB_H) {
                    for (int i = 0; i < cultures.size(); i++) {
                        int tx = guiLeft + MARGIN + i * tabW;
                        if (mx >= tx && mx < tx + tabW) {
                            selectedCulture    = cultures.get(i);
                            selectedStructure  = null;
                            listScrollOffset   = 0;
                            if (importBtn != null) importBtn.active = false;
                            return true;
                        }
                    }
                }
            }

            // Clic dans la liste
            if (mx >= listX && mx < listX + listWidth && my >= listY && my < listY + listHeight) {
                List<String> filtered = getFilteredStructures();
                int idx = ((int) my - listY + listScrollOffset) / ITEM_H;
                if (idx >= 0 && idx < filtered.size()) {
                    selectedStructure = filtered.get(idx);
                    if (importBtn != null) importBtn.active = true;
                    return true;
                }
            }
        }

        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double dx, double dy) {
        if (state == State.IMPORT_SELECT
                && mx >= listX && mx < listX + listWidth
                && my >= listY && my < listY + listHeight) {
            List<String> filtered = getFilteredStructures();
            int maxScroll = Math.max(0, filtered.size() * ITEM_H - listHeight);
            listScrollOffset = Mth.clamp(listScrollOffset - (int) (dy * ITEM_H), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mx, my, dx, dy);
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onCreateSubmit() {
        ImportTableConfig config = buildConfig();
        String name = nameField != null ? nameField.getValue().trim() : "";
        if (config == null || name.isBlank()) return;

        ClientPlayNetworking.send(new ImportTableCreatePayload(tablePos, name, config));
        this.onClose();
    }

    private void onUpdateDimensions() {
        sendUpdate();
    }

    private void sendUpdate() {
        ImportTableConfig config = buildConfig();
        String name = nameField != null ? nameField.getValue().trim() : "";
        if (config == null || name.isBlank()) return;

        ClientPlayNetworking.send(new ImportTableUpdatePayload(tablePos, name, config));
        this.onClose();
    }

    private void onSave() {
        ClientPlayNetworking.send(new ImportTableSavePayload(tablePos));
        this.onClose();
    }

    private void onToggleParticles() {
        particlesEnabled = !particlesEnabled;
        // Mise à jour optimiste côté client (le map statique pilote les particules)
        MillenaireNewAgeClient.updateParticlesState(tablePos, particlesEnabled, currentConfig);
        ClientPlayNetworking.send(new ImportTableToggleParticlesPayload(tablePos));
        if (particlesBtn != null) particlesBtn.setMessage(particlesLabel());
    }

    private void onImport() {
        if (selectedStructure == null) return;
        ClientPlayNetworking.send(new ImportTableImportPayload(tablePos, selectedStructure));
        this.onClose();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private EditBox makeIntField(int x, int y, int w, int defaultValue) {
        EditBox field = new EditBox(this.font, x, y, w, FIELD_H, Component.literal(""));
        field.setMaxLength(5);
        field.setValue(String.valueOf(defaultValue));
        return field;
    }

    @Nullable
    private ImportTableConfig buildConfig() {
        try {
            int wi = Integer.parseInt(widthField.getValue().trim());
            int le = Integer.parseInt(lengthField.getValue().trim());
            int he = Integer.parseInt(heightField.getValue().trim());
            int de = Integer.parseInt(depthField.getValue().trim());
            int fh = Integer.parseInt(floorHeightField.getValue().trim());
            return new ImportTableConfig(wi, le, he, de, fh);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Component particlesLabel() {
        return particlesEnabled
            ? Component.translatable("gui.millenaire-new-age.import_table.toggle_particles_off")
            : Component.translatable("gui.millenaire-new-age.import_table.toggle_particles_on");
    }

    private List<String> getFilteredStructures() {
        if (selectedCulture == null) return availableStructures;
        List<String> result = new ArrayList<>();
        for (String s : availableStructures) {
            String culture = s.contains("/") ? s.substring(0, s.indexOf('/')) : "other";
            if (culture.equals(selectedCulture)) result.add(s);
        }
        return result;
    }

    private String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
