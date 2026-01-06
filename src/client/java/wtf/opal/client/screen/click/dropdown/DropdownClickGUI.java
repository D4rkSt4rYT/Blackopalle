package wtf.opal.client.screen.click.dropdown;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import wtf.opal.client.OpalClient;
import wtf.opal.client.feature.module.ModuleCategory;
import wtf.opal.client.feature.module.impl.visual.ClickGUIModule;
import wtf.opal.client.renderer.NVGRenderer;
import wtf.opal.client.screen.click.dropdown.panel.CategoryPanel;
import wtf.opal.utility.misc.Multithreading;
import wtf.opal.utility.player.PlayerUtility;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static wtf.opal.client.Constants.mc;

public final class DropdownClickGUI extends Screen {

    private final List<CategoryPanel> categoryPanelList = new ArrayList<>();
    public static boolean displayingBinds, selectingBind, typingString;
    
    // Nuove variabili per il layout moderno
    private int selectedCategoryIndex = 0;
    private static final float CATEGORY_PANEL_WIDTH = 120;
    private static final float CATEGORY_ITEM_HEIGHT = 35;
    private static final float PADDING = 15;
    private static final float MODULE_PANEL_X_OFFSET = CATEGORY_PANEL_WIDTH + PADDING * 2;

    public DropdownClickGUI() {
        super(Text.empty());

        int index = 0;
        for (ModuleCategory category : ModuleCategory.VALUES) {
            categoryPanelList.add(new CategoryPanel(category, index));
            index++;
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        final boolean frameStarted = NVGRenderer.beginFrame();

        displayingBinds = PlayerUtility.isKeyPressed(GLFW.GLFW_KEY_TAB);

        final int screenWidth = mc.getWindow().getScaledWidth();
        final int screenHeight = mc.getWindow().getScaledHeight();
        
        // Posizione iniziale (centrata verticalmente, offset a sinistra)
        final float startY = (screenHeight - (categoryPanelList.size() * CATEGORY_ITEM_HEIGHT)) / 2;
        final float categoryX = PADDING;

        // RENDER CATEGORIE (SINISTRA)
        for (int i = 0; i < categoryPanelList.size(); i++) {
            final CategoryPanel panel = categoryPanelList.get(i);
            final float y = startY + i * CATEGORY_ITEM_HEIGHT;
            
            // Imposta dimensioni per il pannello categoria (solo visualizzazione nome)
            panel.setDimensions(categoryX, y, CATEGORY_PANEL_WIDTH, CATEGORY_ITEM_HEIGHT - 5);
            panel.setSelected(i == selectedCategoryIndex);
            
            // Render solo il tab della categoria (non espanso)
            panel.renderCategoryTab(context, mouseX, mouseY, delta);
        }

        // RENDER MODULI DELLA CATEGORIA SELEZIONATA (DESTRA)
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            final CategoryPanel selectedPanel = categoryPanelList.get(selectedCategoryIndex);
            
            // Posiziona il pannello moduli a destra
            selectedPanel.setDimensions(
                MODULE_PANEL_X_OFFSET, 
                PADDING, 
                screenWidth - MODULE_PANEL_X_OFFSET - PADDING, 
                screenHeight - PADDING * 2
            );
            
            // Render completo dei moduli e settings
            selectedPanel.render(context, mouseX, mouseY, delta);
        }

        if (frameStarted) {
            NVGRenderer.endFrameAndReset(true);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        final double mouseX = click.x();
        final double mouseY = click.y();
        final int button = click.button();
        
        final int screenHeight = mc.getWindow().getScaledHeight();
        final float startY = (screenHeight - (categoryPanelList.size() * CATEGORY_ITEM_HEIGHT)) / 2;
        final float categoryX = PADDING;

        // Check click su categorie (sinistra)
        for (int i = 0; i < categoryPanelList.size(); i++) {
            final float y = startY + i * CATEGORY_ITEM_HEIGHT;
            final float height = CATEGORY_ITEM_HEIGHT - 5;
            
            if (mouseX >= categoryX && mouseX <= categoryX + CATEGORY_PANEL_WIDTH &&
                mouseY >= y && mouseY <= y + height) {
                selectedCategoryIndex = i;
                return true;
            }
        }

        // Click sui moduli della categoria selezionata (destra)
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            categoryPanelList.get(selectedCategoryIndex).mouseClicked(mouseX, mouseY, button);
        }

        return true;
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            categoryPanelList.get(selectedCategoryIndex).mouseReleased(click.x(), click.y(), click.button());
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // Solo la categoria selezionata gestisce lo scroll (per i moduli)
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            final int screenWidth = mc.getWindow().getScaledWidth();
            
            // Check se il mouse è sopra il pannello moduli
            if (mouseX >= MODULE_PANEL_X_OFFSET && mouseX <= screenWidth - PADDING) {
                categoryPanelList.get(selectedCategoryIndex).mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        super.keyPressed(keyInput);

        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            categoryPanelList.get(selectedCategoryIndex).keyPressed(keyInput);
        }

        return true;
    }

    @Override
    public boolean charTyped(CharInput charInput) {
        if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categoryPanelList.size()) {
            categoryPanelList.get(selectedCategoryIndex).charTyped((char) charInput.codepoint(), charInput.modifiers());
        }
        return true;
    }

    @Override
    protected void init() {
        categoryPanelList.forEach(CategoryPanel::init);
    }

    @Override
    public void close() {
        if (selectingBind) {
            return;
        }

        categoryPanelList.forEach(CategoryPanel::close);

        Multithreading.schedule(
                () -> OpalClient.getInstance().getModuleRepository().getModule(ClickGUIModule.class).setEnabled(false),
                100, TimeUnit.MILLISECONDS
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

}
