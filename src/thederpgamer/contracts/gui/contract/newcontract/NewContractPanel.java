package thederpgamer.contracts.gui.contract.newcontract;

import api.DebugFile;
import api.common.GameClient;
import api.utils.gui.SimpleGUIHorizontalButtonPane;
import org.schema.game.common.data.player.faction.Faction;
import org.schema.game.client.data.GameClientState;
import org.schema.game.client.view.gui.GUIBlockSprite;
import org.schema.game.client.view.gui.GUIInputPanel;
import org.schema.game.client.view.gui.advanced.tools.*;
import org.schema.game.common.controller.BlockTypeSearchRunnableManager;
import org.schema.game.common.data.element.ElementInformation;
import org.schema.game.common.data.element.ElementKeyMap;
import org.schema.schine.common.TextCallback;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.settings.PrefixNotFoundException;
import org.schema.schine.graphicsengine.forms.font.FontLibrary;
import org.schema.schine.graphicsengine.forms.gui.*;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIActivatableTextBar;
import org.schema.schine.input.InputState;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NewContractPanel extends GUIInputPanel implements BlockTypeSearchRunnableManager.BlockTypeSearchProgressCallback {

    private Faction contractor;
    private GUIAncor content;
    private GUIActivatableTextBar rewardInput;
    private GUIActivatableTextBar nameInput;
    private GUIActivatableTextBar itemInput;
    private GUIAdvTextBar textBar;
    private GUICallback guiCallback;
    private ElementInformation selectedBlockType;
    private GUIAdvDropdown dropdown;
    private GUIAdvBlockDisplay display;
    private boolean textChanged;
    private String curText;


    public NewContractPanel(InputState inputState, GUICallback guiCallback, Faction contractor) {
        super("NewContractPanel", inputState, guiCallback, "New Contract", "");
        this.contractor = contractor;
        this.content = new GUIAncor(getState(), 350, 300);
        this.guiCallback = guiCallback;
        this.curText = "";
    }

    public ElementInformation getSelectedBlockType() {
        if (selectedBlockType != null) {
            return selectedBlockType;
        } else {
            return ElementKeyMap.getInfo(ElementKeyMap.CORE_ID);
        }
    }

    public int getReward() {
        try {
            return Integer.parseInt(rewardInput.getText());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public int getCount() {
        try {
            return Integer.parseInt(itemInput.getText());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String getName() {
        return nameInput.getText();
    }

    public void drawMiningPanel() {
        curText = "";
        if (dropdown != null) {
            dropdown.cleanUp();
            content.detach(dropdown);
        }
        if (display != null) {
            display.cleanUp();
            content.detach(display);
        }
        if (textBar != null) {
            textBar.cleanUp();
            content.detach(textBar);
        }

        addDropdown(new DropdownResult() {
            private List<GUIElement> blockElements;

            @Override
            public DropdownCallback initCallback() {
                return new DropdownCallback() {
                    @Override
                    public void onChanged(Object value) {
                        if (value instanceof ElementInformation) {
                            selectedBlockType = (ElementInformation) value;
                        }
                    }
                };
            }

            @Override
            public String getToolTipText() {
                return "Select resource to request";
            }

            @Override
            public String getName() {
                return "Resource";
            }

            @Override
            public boolean needsListUpdate() {
                return textChanged;
            }

            @Override
            public Collection<? extends GUIElement> getDropdownElements(GUIElement guiElement) {
                blockElements = getResourceElements();
                return blockElements;
            }

            @Override
            public int getDropdownHeight() {
                return 26;
            }

            @Override
            public Object getDefault() {
                if (blockElements != null && blockElements.size() > 0) {
                    return blockElements.get(0);
                } else {
                    return null;
                }
            }

            @Override
            public void flagListNeedsUpdate(boolean flag) {
                textChanged = flag;
            }
        });

        addBlockDisplay(new BlockDisplayResult() {

            @Override
            public BlockSelectCallback initCallback() {
                return callback;
            }

            @Override
            public String getToolTipText() {
                return "Selected Block";
            }

            @Override
            public short getDefault() {
                return ElementKeyMap.RESS_CRYS_BASTYN;
            }

            @Override
            public short getCurrentValue() {
                if(selectedBlockType != null) {
                    return selectedBlockType.getId();
                } else {
                    return 0;
                }
            }

            @Override
            public float getIconScale() {
                return 0.5f;
            }

            @Override
            public float getWeight() {
                return 0.3f;
            }

        });


        addTextBar(new TextBarResult() {

            @Override
            public TextBarCallback initCallback() {
                return callback;
            }

            @Override
            public String getToolTipText() {
                return Lng.str("Enter blocks to search for in the drop down");
            }

            @Override
            public String getName() {
                return Lng.str("Search Blocks");
            }

            @Override
            public String onTextChanged(String text) {
                String t = text.trim();
                if (!t.equals(curText)) {
                    curText = t;
                    textChanged = true;
                }
                return text;
            }
        });

        if (itemInput == null) {
            itemInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "COUNT", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            itemInput.onInit();
        }
        itemInput.setPos(0, 30, 0);
        content.attach(itemInput);
        itemInput.draw();
        if (nameInput != null) {
            nameInput.setText("");
            nameInput.deactivateBar();
            nameInput.cleanUp();
        }

        if (rewardInput == null) {
            rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            rewardInput.onInit();
            rewardInput.setPos(0, 60, 0);
            content.attach(rewardInput);
        }

        rewardInput.setText("");
    }

    public void drawProductionPanel() {

        curText = "";
        if (dropdown != null) {
            dropdown.cleanUp();
            content.detach(dropdown);
        }
        if (display != null) {
            display.cleanUp();
            content.detach(display);
        }
        if (textBar != null) {
            textBar.cleanUp();
            content.detach(textBar);
        }

        addDropdown(new DropdownResult() {
            private List<GUIElement> blockElements;

            @Override
            public DropdownCallback initCallback() {
                return new DropdownCallback() {
                    @Override
                    public void onChanged(Object value) {
                        if (value instanceof ElementInformation) {
                            selectedBlockType = (ElementInformation) value;
                        }
                    }
                };
            }

            @Override
            public String getToolTipText() {
                return "Select resource to request";
            }

            @Override
            public String getName() {
                return "Resource";
            }

            @Override
            public boolean needsListUpdate() {
                return textChanged;
            }

            @Override
            public Collection<? extends GUIElement> getDropdownElements(GUIElement guiElement) {
                blockElements = getProductionElements();
                return blockElements;
            }

            @Override
            public int getDropdownHeight() {
                return 26;
            }

            @Override
            public Object getDefault() {
                if (blockElements != null && blockElements.size() > 0) {
                    return blockElements.get(0);
                } else {
                    return null;
                }
            }

            @Override
            public void flagListNeedsUpdate(boolean flag) {
                textChanged = flag;
            }
        });

        addBlockDisplay(new BlockDisplayResult() {

            @Override
            public BlockSelectCallback initCallback() {
                return callback;
            }

            @Override
            public String getToolTipText() {
                return "Selected Block";
            }

            @Override
            public short getDefault() {
                return ElementKeyMap.CORE_ID;
            }

            @Override
            public short getCurrentValue() {
                if(selectedBlockType != null) {
                    return selectedBlockType.getId();
                } else {
                    return 0;
                }
            }

            @Override
            public float getIconScale() {
                return 0.5f;
            }

            @Override
            public float getWeight() {
                return 0.3f;
            }

        });

        addTextBar(new TextBarResult() {

            @Override
            public TextBarCallback initCallback() {
                return callback;
            }

            @Override
            public String getToolTipText() {
                return Lng.str("Enter blocks to search for in the drop down");
            }

            @Override
            public String getName() {
                return Lng.str("Search Blocks");
            }

            @Override
            public String onTextChanged(String text) {
                String t = text.trim();
                if (!t.equals(curText)) {
                    curText = t;
                    textChanged = true;
                }
                return text;
            }
        });

        if (itemInput == null) {
            itemInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "COUNT", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            itemInput.onInit();
        }
        itemInput.setPos(0, 30, 0);
        content.attach(itemInput);
        itemInput.draw();
        if (nameInput != null) {
            nameInput.setText("");
            nameInput.deactivateBar();
            nameInput.cleanUp();
        }

        if (rewardInput == null) {
            rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) throws PrefixNotFoundException {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            rewardInput.onInit();
            rewardInput.setPos(0, 60, 0);
            content.attach(rewardInput);
        }

        rewardInput.setText("");
    }

    public void drawBountyPanel() {
        if (nameInput == null) {
            nameInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 30, 1, "NAME", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            nameInput.onInit();
        }
        nameInput.setPos(0, 30, 0);
        content.attach(nameInput);
        nameInput.draw();
        if (itemInput != null) {
            itemInput.setText("");
            itemInput.deactivateBar();
            itemInput.cleanUp();
        }

        if (dropdown != null) {
            dropdown.cleanUp();
            content.detach(dropdown);
        }
        if (display != null) {
            display.cleanUp();
            content.detach(display);
        }
        if (textBar != null) {
            textBar.cleanUp();
            content.detach(textBar);
        }

        if (rewardInput == null) {
            rewardInput = new GUIActivatableTextBar(getState(), FontLibrary.FontSize.MEDIUM, 10, 1, "REWARD", content, new TextCallback() {
                @Override
                public String[] getCommandPrefixes() {
                    return null;
                }

                @Override
                public String handleAutoComplete(String s, TextCallback textCallback, String s1) {
                    return null;
                }

                @Override
                public void onFailedTextCheck(String s) {

                }

                @Override
                public void onTextEnter(String s, boolean b, boolean b1) {

                }

                @Override
                public void newLine() {

                }
            }, null);
            rewardInput.onInit();
            rewardInput.setPos(0, 60, 0);
            content.attach(rewardInput);
        }

        rewardInput.setText("");
    }

    @Override
    public float getHeight() {
        return content.getHeight();
    }

    @Override
    public float getWidth() {
        return content.getWidth();
    }

    private void addDropdown(DropdownResult result) {
        dropdown = new GUIAdvDropdown(getState(), content, result);
        dropdown.setPos(0, 120, 0);
        content.attach(dropdown);
    }

    private void addBlockDisplay(BlockDisplayResult displayResult) {
        display = new GUIAdvBlockDisplay(getState(), content, displayResult);
        display.setPos(0, 120, 0);
        content.attach(display);
    }

    private void addTextBar(TextBarResult textBarResult) {
        textBar = new GUIAdvTextBar(getState(), content, textBarResult);
        textBar.setPos(0, 90, 0);
        content.attach(textBar);
    }

    @Override
    public void onDone() {

    }

    @Override
    public void onInit() {
        super.onInit();

        setPos(0, 0, 0);

        SimpleGUIHorizontalButtonPane buttonPane = new SimpleGUIHorizontalButtonPane(getState(), background.getInnerWidth() + 12, background.getInnerHeigth(), 2);

        //Todo: Make Cargo contracts automatically generate for trade network deals
        GUITextButton bountyButton = new GUITextButton(getState(), 100, 24, GUITextButton.ColorPalette.OK, "BOUNTY", guiCallback);
        bountyButton.setUserPointer("BOUNTY");
        buttonPane.addButton(bountyButton);
        GUITextButton miningButton = new GUITextButton(getState(), 100, 24, GUITextButton.ColorPalette.OK, "MINING", guiCallback);
        miningButton.setUserPointer("MINING");
        buttonPane.addButton(miningButton);
        GUITextButton productionButton = new GUITextButton(getState(), 100, 24, GUITextButton.ColorPalette.OK, "PRODUCTION", guiCallback);
        productionButton.setUserPointer("PRODUCTION");
        buttonPane.addButton(productionButton);

        buttonPane.setPos(0, 0, 0);
        content.attach(buttonPane);

        getContent().attach(content);

        float currentWidth = background.getWidth();
        background.setWidth(currentWidth + 50);
    }

    private ArrayList<ElementInformation> getResourcesFilter() {
        ArrayList<ElementInformation> filter = new ArrayList<>();
        for (ElementInformation info : ElementKeyMap.getInfoArray()) {
            try {
                if (info != null && !info.isDeprecated() && (info.name.toLowerCase().contains("raw") || info.name.toLowerCase().contains("capsule"))) {
                    if (curText.equals("")) {
                        filter.add(info);
                    } else {
                        if (info.name.toLowerCase().contains(curText.toLowerCase())) filter.add(info);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                DebugFile.log("fuck you suck my dicc and bal");
            }
        }
        return filter;
    }

    private ArrayList<ElementInformation> getProductionFilter() {
        ArrayList<ElementInformation> filter = new ArrayList<>();
        for (ElementInformation info : ElementKeyMap.getInfoArray()) {
            try {
                if (info != null && info.isInRecipe() && !info.name.toLowerCase().contains("capsule") && !info.name.toLowerCase().contains("raw") && !info.isDeprecated()) {
                    if (curText.equals("")) {
                        filter.add(info);
                    } else {
                        if (info.name.toLowerCase().contains(curText.toLowerCase())) filter.add(info);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                DebugFile.log("fuck you suck my dicc and bal");
            }
        }
        return filter;
    }

    private ArrayList<GUIElement> getResourceElements() {
        ArrayList<GUIElement> elementList = new ArrayList<>();
        GameClientState gameClientState = GameClient.getClientState();

        for (ElementInformation elementInfo : getResourcesFilter()) {

            GUIAncor anchor = new GUIAncor(gameClientState, 300.0F, 26.0F);
            elementList.add(anchor);
            GUITextOverlay textOverlay = new GUITextOverlay(100, 26, FontLibrary.getBoldArial12White(), gameClientState);

            textOverlay.setTextSimple(elementInfo.getName());
            anchor.setUserPointer(elementInfo);
            GUIBlockSprite blockSprite = new GUIBlockSprite(gameClientState, elementInfo.getId());
            blockSprite.getScale().set(0.4F, 0.4F, 0.0F);
            anchor.attach(blockSprite);
            textOverlay.getPos().x = 50.0F;
            textOverlay.getPos().y = 7.0F;
            anchor.attach(textOverlay);
        }
        return elementList;
    }

    private List<GUIElement> getProductionElements() {
        ArrayList<GUIElement> elementList = new ArrayList<>();
        GameClientState gameClientState = GameClient.getClientState();

        for (ElementInformation elementInfo : getProductionFilter()) {

            GUIAncor anchor = new GUIAncor(gameClientState, 300.0F, 26.0F);
            elementList.add(anchor);
            GUITextOverlay textOverlay = new GUITextOverlay(100, 26, FontLibrary.getBoldArial12White(), gameClientState);

            textOverlay.setTextSimple(elementInfo.getName());
            anchor.setUserPointer(elementInfo);
            GUIBlockSprite blockSprite = new GUIBlockSprite(gameClientState, elementInfo.getId());
            blockSprite.getScale().set(0.4F, 0.4F, 0.0F);
            anchor.attach(blockSprite);
            textOverlay.getPos().x = 50.0F;
            textOverlay.getPos().y = 7.0F;
            anchor.attach(textOverlay);
        }
        return elementList;
    }

    public Faction getContractor() {
        return contractor;
    }
}
