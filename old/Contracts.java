package thederpgamer.contracts;

import api.DebugFile;
import api.common.GameClient;
import api.common.GameServer;
import api.listener.Listener;
import api.listener.events.fleet.FleetLoadSectorEvent;
import api.listener.events.gui.ControlManagerActivateEvent;
import api.listener.events.gui.GUITopBarCreateEvent;
import api.listener.events.gui.MainWindowTabAddEvent;
import api.listener.events.player.BuyTradeEvent;
import api.listener.events.player.PlayerDeathEvent;
import api.listener.events.player.PlayerSpawnEvent;
import api.listener.events.player.SellTradeEvent;
import api.listener.fastevents.FastListenerCommon;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.FileConfiguration;
import api.network.Packet;
import api.utils.StarRunnable;
import api.utils.game.PlayerUtils;
import org.schema.game.client.controller.manager.ingame.shop.ShopControllerManager;
import org.schema.game.client.view.gui.PlayerPanel;
import org.schema.game.client.view.gui.newgui.GUITopBar;
import org.schema.game.client.view.gui.shop.shopnew.ShopPanelNew;
import org.schema.game.common.controller.ElementCountMap;
import org.schema.game.common.controller.SegmentController;
import org.schema.game.common.data.fleet.FleetMember;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.SimpleTransformableSendableObject;
import org.schema.game.server.data.PlayerNotFountException;
import org.schema.game.server.data.simulation.npc.NPCFaction;
import org.schema.schine.common.language.Lng;
import org.schema.schine.graphicsengine.core.MouseEvent;
import org.schema.schine.graphicsengine.forms.gui.GUIActivationHighlightCallback;
import org.schema.schine.graphicsengine.forms.gui.GUICallback;
import org.schema.schine.graphicsengine.forms.gui.GUIElement;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIContentPane;
import org.schema.schine.graphicsengine.forms.gui.newgui.GUIMainWindow;
import org.schema.schine.input.InputState;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.UUID;

public class Contracts extends StarMod {

    //Instance
    static Contracts instance;
    public Contracts() { }
    public static void main(String[] args) { }

    //Config
    private String[] defaultConfig = {
            "debug-mode: false",
            "cargo-contracts-enabled: true",
            "cargo-escort-bonus: 1.3",
            "contract-timer-max: 30",
            "npc-contracts-enabled: true",
            "traders-faction-id: -10000000",
            "automatic-contract-generation-max: 5",
            "pirate-interception-chance: 7"
    };

    public boolean debugMode = false;
    public boolean cargoContractsEnabled = true;
    public double cargoEscortBonus = 1.3;
    public int contractTimerMax = 30;
    public boolean npcContractsEnabled = true;
    public int tradersFactionID = -10000000;
    public int automaticContractGenerationMax = 5;

    //Other
    public GameMapListener gameMapListener;

    @Override
    public void onEnable() {
        instance = this;
        initConfig();
        registerRunners();
        registerPackets();
        registerCommands();
        registerFastListeners();
        registerListeners();
    }

    @Override
    public void onDisable() {
        FastListenerCommon.gameMapListeners.remove(gameMapListener);
    }

    private void registerFastListeners() {
        FastListenerCommon.gameMapListeners.add(gameMapListener = new GameMapListener());
    }

    private void registerListeners() {
        StarLoader.registerListener(PlayerSpawnEvent.class, new Listener<PlayerSpawnEvent>() {
            @Override
            public void onEvent(PlayerSpawnEvent event) {
                StarPlayer player = new StarPlayer(event.getPlayer().getOwnerState());
                try {
                    if(DataUtils.getPlayerData(player.getName()) == null) {
                        PlayerData playerData = new PlayerData(player);
                        DataUtils.addPlayer(playerData);
                        DebugFile.log("[DEBUG]: Registered PlayerData for " + player.getName() + ".", Contracts.getInstance());
                    }
                } catch (PlayerNotFountException e) {
                    e.printStackTrace();
                }
            }
        }, this);

        StarLoader.registerListener(PlayerDeathEvent.class, new Listener<PlayerDeathEvent>() {
            @Override
            public void onEvent(PlayerDeathEvent event) {
                StarFaction killerFaction = null;
                StarFaction targetFaction = null;
                double pointAward = 15.0;
                double pointCost = 15.0;
                if (event.getPlayer().getFactionId() != 0) {
                    targetFaction = new StarFaction(StarLoader.getGameState().getFactionManager().getFaction(event.getPlayer().getFactionId()));
                    pointAward += 5.0;
                    pointCost -= 5.0;
                }
                if (event.getDamager().isSegmentController()) {
                    SegmentController controller = (SegmentController) event.getDamager();
                    if (controller.getFactionId() != 0) {
                        killerFaction = new StarFaction(StarLoader.getGameState().getFactionManager().getFaction(controller.getFactionId()));
                        pointAward += 5.0;
                        pointCost -= 5.0;
                    }

                    if (controller.getType().equals(SimpleTransformableSendableObject.EntityType.SHIP)) {
                        Ship ship = new Ship(controller);
                        if (ship.getDockedRoot().getPilot() != null) {
                            StarPlayer attacker = ship.getDockedRoot().getPilot();
                            StarPlayer target = new StarPlayer(event.getPlayer());
                            try {
                                for (Contract contract : DataUtils.getPlayerContracts(attacker.getName())) {
                                    PlayerTarget playerTarget = (PlayerTarget) contract.getTarget();
                                    if (contract.getContractType().equals(Contract.ContractType.BOUNTY) && playerTarget.getTargets()[0].equals(target.getName())) {
                                        Server.broadcastMessage("[CONTRACTS]: " + attacker.getName() + " has claimed the bounty on " + target.getName() + " for a reward of " + contract.getReward() + " credits!");
                                        attacker.setCredits(attacker.getCredits() + contract.getReward());
                                        DataUtils.removeContract(contract, false, attacker);
                                    }
                                }
                            } catch (PlayerNotFountException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } else if (event.getDamager().getOwnerState() instanceof PlayerState) {
                    StarPlayer attacker = new StarPlayer((PlayerState) event.getDamager().getOwnerState());
                    StarPlayer target = new StarPlayer(event.getPlayer());
                    if (attacker.getPlayerState().getFactionId() != 0) killerFaction = attacker.getFaction();
                    try {
                        for (Contract contract : DataUtils.getPlayerContracts(attacker.getName())) {
                            PlayerTarget playerTarget = (PlayerTarget) contract.getTarget();
                            if (contract.getContractType().equals(Contract.ContractType.BOUNTY) && playerTarget.getTargets()[0].equals(target.getName())) {
                                Server.broadcastMessage("[CONTRACTS]: " + attacker.getName() + " has claimed the bounty on " + target.getName() + " for a reward of " + contract.getReward() + " credits!");
                                attacker.setCredits(attacker.getCredits() + contract.getReward());
                                PlayerData attackerData = DataUtils.getPlayerData(attacker.getName());
                                attackerData.modOpinionScore(contract.getContractor().getID(), 10);
                                DataUtils.addPlayer(attackerData);
                                DataUtils.removeContract(contract, false, attacker);
                                break;
                            }
                        }
                    } catch (PlayerNotFountException e) {
                        e.printStackTrace();
                    }
                }

                if (killerFaction != null) {
                    FactionData killerData = FactionUtils.getFactionData(killerFaction);
                    killerData.setFactionPower(killerData.getFactionPower() + pointAward);
                    FactionUtils.updateFactionData(killerData);
                }

                if (targetFaction != null) {
                    FactionData targetData = FactionUtils.getFactionData(targetFaction);
                    targetData.setFactionPower(targetData.getFactionPower() + pointCost);
                    FactionUtils.updateFactionData(targetData);
                }
            }
        });

        StarLoader.registerListener(GUITopBarCreateEvent.class, new Listener<GUITopBarCreateEvent>() {
            @Override
            public void onEvent(final GUITopBarCreateEvent guiTopBarCreateEvent) {
                GUITopBar.ExpandedButton dropDownButton = guiTopBarCreateEvent.getDropdownButtons().get(guiTopBarCreateEvent.getDropdownButtons().size() - 1);
                dropDownButton.addExpandedButton("CONTRACTS", new GUICallback() {
                    @Override
                    public void callback(final GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse()) {
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            final GUIMainWindow guiWindow = new GUIMainWindow(GameClient.getClientState(), 850, 550, "CONTRACTS");
                            guiWindow.onInit();
                            guiWindow.setCloseCallback(new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent event) {
                                    if (event.pressedLeftMouse()) {
                                        GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return !guiWindow.getState().getController().getPlayerInputs().isEmpty();
                                }
                            });

                            GUIContentPane contractsPane = guiWindow.addTab("CONTRACTS");
                            contractsPane.setTextBoxHeightLast(300);

                            PlayerContractsScrollableList playerContractsList = new PlayerContractsScrollableList(GameClient.getClientState(), 500, 300, contractsPane.getContent(0));
                            playerContractsList.onInit();
                            contractsPane.getContent(0).attach(playerContractsList);

                            GUIUtils.activateCustomGUIWindow(guiWindow);
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false;
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });

                dropDownButton.addExpandedButton("STATS", new GUICallback() {
                    @Override
                    public void callback(GUIElement guiElement, MouseEvent mouseEvent) {
                        if (mouseEvent.pressedLeftMouse()) {
                            GameClient.getClientState().getController().queueUIAudio("0022_menu_ui - enter");
                            final GUIMainWindow guiWindow = new GUIMainWindow(GameClient.getClientState(), 850, 550, "STATS");
                            guiWindow.onInit();
                            guiWindow.setCloseCallback(new GUICallback() {
                                @Override
                                public void callback(GUIElement guiElement, MouseEvent event) {
                                    if (event.pressedLeftMouse()) {
                                        GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerPanel().deactivateAll();
                                    }
                                }

                                @Override
                                public boolean isOccluded() {
                                    return !guiWindow.getState().getController().getPlayerInputs().isEmpty();
                                }
                            });

                            GUIContentPane statsPane = guiWindow.addTab("STATS");
                            statsPane.setTextBoxHeightLast(300);

                            GUIUtils.activateCustomGUIWindow(guiWindow);
                        }
                    }

                    @Override
                    public boolean isOccluded() {
                        return false;
                    }
                }, new GUIActivationHighlightCallback() {
                    @Override
                    public boolean isHighlighted(InputState inputState) {
                        return false;
                    }

                    @Override
                    public boolean isVisible(InputState inputState) {
                        return true;
                    }

                    @Override
                    public boolean isActive(InputState inputState) {
                        return true;
                    }
                });
            }
        });

        StarLoader.registerListener(ControlManagerActivateEvent.class, new Listener<ControlManagerActivateEvent>() {
            @Override
            public void onEvent(ControlManagerActivateEvent event) {
                if (event.isActive() && event.getControlManager() instanceof ShopControllerManager) {
                    try {
                        PlayerPanel playerPanel = GameClient.getClientState().getWorldDrawer().getGuiDrawer().getPlayerPanel();
                        if (debugMode) DebugFile.log("[DEBUG]: ShopControllerManager activated", getMod());
                        Field shopPanelField = PlayerPanel.class.getDeclaredField("shopPanelNew");
                        shopPanelField.setAccessible(true);
                        ShopPanelNew shopPanelNew = (ShopPanelNew) shopPanelField.get(playerPanel);
                        if (shopPanelNew.isActive() && shopPanelNew.shopPanel != null && shopPanelNew.shopPanel.getTabs().size() > 0) {
                            Collection<GUIContentPane> tabs = shopPanelNew.shopPanel.getTabs();
                            SpecialDealsTab specialDealsTab = null;
                            StarPlayer player = new StarPlayer(GameClient.getClientPlayerState());
                            PlayerData playerData = DataUtils.getPlayerData(player.getName());
                            if (DataUtils.getSectorStationFactionID(player) == tradersFactionID) {
                                switch (playerData.getOpinionFromID(tradersFactionID).getOpinion()) {
                                    case Opinion.HATED:
                                        GameClient.getClientState().getGlobalGameControlManager().getIngameControlManager().getPlayerGameControlManager().getShopControlManager().setActive(false);
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Go away scum!");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "It appears the traders have a strong hatred of you, and are unwilling to even speak to you.");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        break;
                                    case Opinion.HOSTILE:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] You better have something worth our time...");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "It appears the traders have a strong distrust of you, and may be unwilling to sell you some items or services.");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        break;
                                    case Opinion.POOR:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Welcome to our shop space travel-oh... it's you again.");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "It appears the traders have a slight distrust of you, and may charge more for some items or services.");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        break;
                                    case Opinion.COOL:
                                    case Opinion.NEUTRAL:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Welcome to our shop space traveller!");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        break;
                                    case Opinion.CORDIAL:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Welcome back to our shop space traveller!");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        break;
                                    case Opinion.GOOD:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Welcome back to our shop space traveller! What can we do for you?");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "The trader recognizes you and greets you with some enthusiasm. Perhaps they may be willing to get you a Special Deal...");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        specialDealsTab = new SpecialDealsTab(shopPanelNew.shopPanel.getState(), shopPanelNew.shopPanel, Opinion.GOOD);
                                        specialDealsTab.onInit();
                                        shopPanelNew.shopPanel.getTabs().add(specialDealsTab);
                                        shopPanelField.set(playerPanel, shopPanelNew);
                                        break;
                                    case Opinion.EXCELLENT:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Welcome back to our shop friend! What can we do for you?");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "The trader recognizes you and greets you enthusiastically as if you were a good friend. Perhaps they may be willing to share some valuable information with you...");

                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }

                                        specialDealsTab = new SpecialDealsTab(shopPanelNew.shopPanel.getState(), shopPanelNew.shopPanel, Opinion.EXCELLENT);
                                        specialDealsTab.onInit();
                                        shopPanelNew.shopPanel.getTabs().add(specialDealsTab);
                                        shopPanelField.set(playerPanel, shopPanelNew);
                                        break;
                                    case Opinion.TRUSTED:
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "[TRADERS] Good to see you again! Welcome back to our shop!");
                                        PlayerUtils.sendMessage(GameClient.getClientPlayerState(), "The trader recognizes you and greets you enthusiastically as if you were a close friend. It is clear the Trading Guild sees you as a close and trusted ally...");
                                        for (GUIContentPane tab : tabs) {
                                            if (tab.getTabName().equals("SPECIAL DEALS")) {
                                                shopPanelNew.shopPanel.getTabs().remove(tab);
                                                break;
                                            }
                                        }
                                        specialDealsTab = new SpecialDealsTab(shopPanelNew.shopPanel.getState(), shopPanelNew.shopPanel, Opinion.TRUSTED);
                                        specialDealsTab.onInit();
                                        shopPanelNew.shopPanel.getTabs().add(specialDealsTab);
                                        shopPanelField.set(playerPanel, shopPanelNew);
                                        break;
                                }
                            }
                        }
                    } catch (PlayerNotFountException | IllegalAccessException | NoSuchFieldException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        StarLoader.registerListener(MainWindowTabAddEvent.class, new Listener<MainWindowTabAddEvent>() {
            @Override
            public void onEvent(MainWindowTabAddEvent event) {
                if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_SHOP_SHOPNEW_SHOPPANELNEW_2)) {
                    ContractsTab contractsTab = new ContractsTab(event.getWindow().getState(), event.getWindow());
                    contractsTab.onInit();
                    event.getWindow().getTabs().add(contractsTab);
                } else if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_2)) {
                    GUIContentPane oldDiplomacyTab = event.getPane();
                    event.getWindow().getTabs().remove(oldDiplomacyTab);
                    DiplomacyTab diplomacyTab = new DiplomacyTab(event.getWindow().getState(), event.getWindow());
                    diplomacyTab.onInit();
                    event.getWindow().getTabs().add(diplomacyTab);
                } else if (event.getTitle().equals(Lng.ORG_SCHEMA_GAME_CLIENT_VIEW_GUI_FACTION_NEWFACTION_FACTIONPANELNEW_9)) {
                    GUIContentPane oldListTab = event.getPane();
                    event.getWindow().getTabs().remove(oldListTab);
                    if (GameClient.getClientPlayerState().getFactionId() != 0) {
                        FederationsTab federationsTab = new FederationsTab(event.getWindow().getState(), event.getWindow());
                        federationsTab.onInit();
                        event.getWindow().getTabs().add(federationsTab);
                    }
                }
            }
        });

        if (cargoContractsEnabled) {
            StarLoader.registerListener(BuyTradeEvent.class, new Listener<BuyTradeEvent>() {
                @Override
                public void onEvent(BuyTradeEvent event) {
                    if (event.getBuyer().getFactionId() != 0 && event.getSeller().getFactionId() != 0) {
                        StarFaction buyerFaction = StarFaction.fromId(event.getBuyer().getFactionId());
                        StarFaction sellerFaction = StarFaction.fromId(event.getSeller().getFactionId());

                        if (buyerFaction.getInternalFaction().isNPC() && sellerFaction.getInternalFaction().isNPC())
                            return;
                        int totalCost = event.getTotalCost();
                        ItemStack[] items = new ItemStack[event.getItems().size()];
                        for (int i = 0; i < items.length; i++) {
                            ItemStack itemStack = new ItemStack(event.getItems().get(i).getType());
                            itemStack.setAmount(event.getItems().get(i).amount);
                            items[i] = itemStack;
                        }
                        String contractName = "Escort cargo to " + event.getTo().toString();
                        CargoTarget cargoTarget = new CargoTarget();
                        cargoTarget.setTargets(items);
                        Contract cargoContract = new Contract(event.getBuyer().getFactionId(), contractName, Contract.ContractType.CARGO_ESCORT, (int) (totalCost * cargoEscortBonus), UUID.randomUUID().toString(), cargoTarget);
                        cargoTarget.setLocation(StarUniverse.getUniverse().getSector(event.getTo()));
                        cargoContract.setTarget(cargoTarget);
                        StarFaction traders = new StarFaction(GameServer.getServerState().getFactionManager().getFaction(tradersFactionID));
                        NPCFaction npcFaction = (NPCFaction) traders.getInternalFaction();
                        ElementCountMap elementCountMap = new ElementCountMap();
                        for (ItemStack item : items) {
                            elementCountMap.inc(item.getId(), item.getAmount());
                        }
                        //Fleet tradeFleet = new Fleet(npcFaction.getFleetManager().spawnTradingFleet(elementCountMap, event.getFrom(), event.getTo()));
                        Fleet tradeFleet = new Fleet(Fleet.getServerFleetManager().getByFleetDbId(event.getOrder().assignFleetBuy(event.getBuyer(), event.getSeller(), event.getFrom(), event.getTo())));
                        tradeFleet.idle();
                        ContractUtils.tradeFleets.put(cargoContract, tradeFleet.getInternalFleet().dbid);
                        ContractUtils.startCargoClaimTimer(cargoContract);

                        DataUtils.addContract(cargoContract);
                        if (ContractsScrollableList.getInst() != null) {
                            ContractsScrollableList.getInst().clear();
                            ContractsScrollableList.getInst().handleDirty();
                        }
                        //event.setCanceled(true);
                    }
                }
            });

            StarLoader.registerListener(SellTradeEvent.class, new Listener<SellTradeEvent>() {
                @Override
                public void onEvent(SellTradeEvent event) {
                    if (event.getBuyer().getFactionId() != 0 && event.getSeller().getFactionId() != 0) {
                        StarFaction buyerFaction = StarFaction.fromId(event.getBuyer().getFactionId());
                        StarFaction sellerFaction = StarFaction.fromId(event.getSeller().getFactionId());

                        if (buyerFaction.getInternalFaction().isNPC() && sellerFaction.getInternalFaction().isNPC())
                            return;
                        int totalCost = event.getTotalCost();
                        ItemStack[] items = new ItemStack[event.getItems().size()];
                        for (int i = 0; i < items.length; i++) {
                            ItemStack itemStack = new ItemStack(event.getItems().get(i).getType());
                            itemStack.setAmount(event.getItems().get(i).amount);
                            items[i] = itemStack;
                        }
                        String contractName = "Escort cargo to " + event.getTo().toString();
                        CargoTarget cargoTarget = new CargoTarget();
                        cargoTarget.setTargets(items);
                        Contract cargoContract = new Contract(event.getBuyer().getFactionId(), contractName, Contract.ContractType.CARGO_ESCORT, (int) (totalCost * cargoEscortBonus), UUID.randomUUID().toString(), cargoTarget);
                        cargoTarget.setLocation(StarUniverse.getUniverse().getSector(event.getTo()));
                        cargoContract.setTarget(cargoTarget);
                        StarFaction traders = new StarFaction(GameServer.getServerState().getFactionManager().getFaction(tradersFactionID));
                        NPCFaction npcFaction = (NPCFaction) traders.getInternalFaction();
                        ElementCountMap elementCountMap = new ElementCountMap();
                        for (ItemStack item : items) {
                            elementCountMap.inc(item.getId(), item.getAmount());
                        }
                        //Fleet tradeFleet = new Fleet(npcFaction.getFleetManager().spawnTradingFleet(elementCountMap, event.getFrom(), event.getTo()));
                        Fleet tradeFleet = new Fleet(Fleet.getServerFleetManager().getByFleetDbId(event.getOrder().assignFleetSell(event.getBuyer(), event.getSeller(), event.getFrom(), event.getTo())));
                        tradeFleet.idle();
                        ContractUtils.tradeFleets.put(cargoContract, tradeFleet.getInternalFleet().dbid);
                        ContractUtils.startCargoClaimTimer(cargoContract);

                        DataUtils.addContract(cargoContract);
                        if (ContractsScrollableList.getInst() != null) {
                            ContractsScrollableList.getInst().clear();
                            ContractsScrollableList.getInst().handleDirty();
                        }
                        //event.setCanceled(true);
                    }
                }
            });

            StarLoader.registerListener(FleetLoadSectorEvent.class, new Listener<FleetLoadSectorEvent>() {
                @Override
                public void onEvent(FleetLoadSectorEvent event) {
                    StarSector newSector = StarUniverse.getUniverse().getSector(event.getNewPosition());
                    if (ContractUtils.cargoSectors.containsValue(newSector)) {
                        for (FleetMember member : event.getFleet().getMembers()) {
                            Ship ship = new Ship(member.getLoaded());
                            try {
                                PlayerData playerData = DataUtils.getPlayerData((ship.getDockedRoot().getPilot().getName()));
                                for (Contract contract : DataUtils.getPlayerContracts(playerData.getName())) {
                                    if (contract.getContractType().equals(Contract.ContractType.CARGO_ESCORT)) {
                                        Fleet tradeFleet = new Fleet(Fleet.getServerFleetManager().getByFleetDbId(ContractUtils.tradeFleets.get(contract)));
                                        if (tradeFleet.getFlagshipSector().equals(contract.getTarget().getLocation())) {
                                            DebugFile.log("[DEBUG]: Trade fleet for contract " + contract.getName() + " has arrived at their target destination.");
                                            try {
                                                StarPlayer player = new StarPlayer(GameServer.getServerState().getPlayerFromName(playerData.getName()));
                                                StarFaction traders = new StarFaction(GameServer.getServerState().getFactionManager().getFaction(tradersFactionID));
                                                StarFaction contractor = contract.getContractor();
                                                contract.setFinished(true);
                                                DataUtils.removeContract(contract, false, player);
                                                PlayerData pData = DataUtils.getPlayerData((player.getName()));
                                                pData.modOpinionScore(traders.getID(), 5);
                                                pData.modOpinionScore(contractor.getID(), 10);
                                                DataUtils.addPlayer(pData);
                                            } catch (PlayerNotFountException e) {
                                                e.printStackTrace();
                                            }
                                            //Todo: Pause the trade progress
                                            return;
                                        }
                                    }
                                }
                            } catch (PlayerNotFountException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
        DebugFile.log("Registered Listeners", this);
    }

    private void registerCommands() {

        StarLoader.registerCommand(new EndContractsCommand());
        StarLoader.registerCommand(new SpawnTradeFleetCommand());
        StarLoader.registerCommand(new GiveOpinionCommand());
        StarLoader.registerCommand(new RandomContractCommand());

        DebugFile.log("Registered Commands", this);
    }

    private void registerPackets() {
        Packet.registerPacket(GetAllContractsPacket.class);
        Packet.registerPacket(GetClientContractsPacket.class);
        Packet.registerPacket(GetPlayerDataPacket.class);
        Packet.registerPacket(ReturnPlayerDataPacket.class);
        Packet.registerPacket(ReturnClientContractsPacket.class);
        Packet.registerPacket(ReturnAllContractsPacket.class);
        Packet.registerPacket(AddContractPacket.class);
        Packet.registerPacket(RemoveContractPacket.class);
        Packet.registerPacket(GetFactionAlliesPacket.class);
        Packet.registerPacket(ReturnFactionAlliesPacket.class);
        Packet.registerPacket(GetClientSectorStationFactionPacket.class);
        Packet.registerPacket(ReturnClientSectorStationFactionPacket.class);
        Packet.registerPacket(GetFederationsPacket.class);
        Packet.registerPacket(ReturnFederationsPacket.class);

        DebugFile.log("Registered Packets", this);
    }

    private void registerRunners() {
        new StarRunnable() {
            @Override
            public void run() {
                if (automaticContractGenerationMax > 0 && DataUtils.getAllContracts().size() <= automaticContractGenerationMax) {
                    ContractUtils.generateRandomContract();
                }
            }
        }.runTimer(5000);

        DebugFile.log("Registered Runners", this);
    }

    private void initConfig() {
        FileConfiguration config = getConfig("config");
        config.saveDefault(defaultConfig);

        this.debugMode = Boolean.parseBoolean(config.getString("debug-mode"));
        this.modCompatibility = Boolean.parseBoolean(config.getString("mod-compatibility-enabled"));
        this.cargoContractsEnabled = Boolean.parseBoolean(config.getString("cargo-contracts-enabled"));
        this.cargoEscortBonus = config.getDouble("cargo-escort-bonus");
        this.contractTimerMax = config.getInt("contract-timer-max");
        this.npcContractsEnabled = Boolean.parseBoolean(config.getString("npc-contracts-enabled"));
        this.tradersFactionID = config.getInt("traders-faction-id");
        this.automaticContractGenerationMax = config.getInt("automatic-contract-generation-max");

        DebugFile.log("Loaded Config", this);
    }

    public static Contracts getInstance() {
        return instance;
    }
}
