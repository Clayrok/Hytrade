package com.clayrok.hytrade.pages;

import com.clayrok.hytrade.DynamicImageService;
import com.clayrok.hytrade.Hytrade;
import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.HytradeVaultUnlocked;
import com.clayrok.hytrade.data.*;
import com.clayrok.hytrade.helpers.TranslationHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.milkbowl.vault2.economy.Economy;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class TradePanel extends InteractiveCustomUIPage<EventActionData>
{
    private final List<TradeContentLayoutData> contentLayouts;
    private int currentLayoutIndex = 0;

    private ScheduledFuture<?> securityChecksHandle = null;

    private PlayerConfigData playerConfigData = null;
    private String language = "en-US";

    private TradeData tradeData = null;
    private UUID playerUUID = null;
    private UUID otherPlayerUUID = null;
    private Integer emptySlotsCount = 0;

    public TradePanel(PlayerRef playerRef, TradeData TradeData)
    {
        super(playerRef, CustomPageLifetime.CantClose, EventActionData.CODEC);

        TradeData.subscribeRefresh(this::refresh);
        TradeData.subscribeRefreshMoney(this::refreshAllMoneyValues);

        this.tradeData = TradeData;
        playerUUID = playerRef.getUuid();
        otherPlayerUUID = TradeData.playerUUIDs.get(TradeData.playerUUIDs.indexOf(playerUUID) == 0 ? 1 : 0);

        contentLayouts = TradeContentLayoutData.getAllLayouts();
        loadConfig();
    }

    private void loadConfig()
    {
        playerConfigData = PlayerConfigData.getConfigData(playerUUID);

        for (int i = 0; i < contentLayouts.size(); i++)
        {
            if (contentLayouts.get(i).name.equals(playerConfigData.vars.tradePanelLayoutName.getValue()))
            {
                currentLayoutIndex = i;
                break;
            }
        }

        language = playerConfigData.vars.language.getValue();
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiBuilder,
                      @NonNullDecl UIEventBuilder eventBuilder,
                      @NonNullDecl Store<EntityStore> store)
    {
        //Send currency icon to player
        String currencyIconPath = HytradeConfig.get().getFullCurrencyIconPath();
        if (currencyIconPath.length() > 0)
        {
            DynamicImageService.sendLocalToInterfaceSlot(playerRef, currencyIconPath, 0);
        }

        uiBuilder.append("Pages/TradePanel/TradePanel.ui");

        loadContentLayout(uiBuilder, eventBuilder);
        buildInventoryList(uiBuilder, eventBuilder, ref, store);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveLeftBtn",
                new EventData().append("ActionId", "POS_UPDATE")
                        .append("movePanelDirection", String.valueOf(-1))
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveRightBtn",
                new EventData().append("ActionId", "POS_UPDATE")
                        .append("movePanelDirection", String.valueOf(1))
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ChangeLayout",
                new EventData().append("ActionId", "CHANGE_LAYOUT")
        );

        refresh();

        playOpenSound();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store)
    {
        securityChecksHandle.cancel(true);
        playCloseSound();
    }

    private void playOpenSound()
    {
        int soundIndex = SoundEvent.getAssetMap().getIndex(HytradeConfig.get().getTradePanelOpenSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI);
    }

    public void playCloseSound()
    {
        int soundIndex = SoundEvent.getAssetMap().getIndex(HytradeConfig.get().getTradePanelCloseSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, soundIndex, SoundCategory.UI);
    }

    private void loadContentLayout(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        //Clears all the content
        uiBuilder.clear("#TradePanel #Content");
        contentLayouts.get(currentLayoutIndex).buildFunction.accept(uiBuilder);
        uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/Elements/BottomButtons.ui");

        //Inits money
        initMoney(uiBuilder, eventBuilder);

        //Updates layout button icon
        int nextLayoutIndex = currentLayoutIndex + 1;
        if (nextLayoutIndex > contentLayouts.size() - 1) nextLayoutIndex = 0;
        uiBuilder.set(contentLayouts.get(currentLayoutIndex).layoutIconSelector + ".Visible", false);
        uiBuilder.set(contentLayouts.get(nextLayoutIndex).layoutIconSelector + ".Visible", true);

        String playerUsername = playerRef.getUsername();
        String otherPlayerUsername = Universe.get().getPlayer(otherPlayerUUID).getUsername();

        uiBuilder.set("#YourUsername.Text", playerUsername);
        uiBuilder.set("#TheirUsername.Text", otherPlayerUsername);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CancelBtn",
                EventData.of("ActionId", "CANCEL")
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ValidateBtn",
                new EventData().append("ActionId", "VALIDATE")
        );
    }

    private void initMoney(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        Economy economyObj = HytradeVaultUnlocked.getEconomyObj();
        HytradeConfig config = HytradeConfig.get();
        if (economyObj == null || !config.getIsMoneyTradable()) return;

        uiBuilder.set("#TradePoolYours #MoneyGroup.Visible", true);
        uiBuilder.set("#TradePoolTheirs #MoneyGroup.Visible", true);

        //Apply downloaded currency icons
        if (HytradeConfig.get().getFullCurrencyIconPath().length() > 0)
        {
            uiBuilder.set("#TradePoolYours #CurrencyIcon.Background", "Pages/Dynamic/DynamicImage1.png");
            uiBuilder.set("#TradePoolTheirs #CurrencyIcon.Background", "Pages/Dynamic/DynamicImage1.png");
        }

        if (!HytradeConfig.get().getIsMoneyDecimal())
        {
            uiBuilder.set("#TradePoolYours #YourMoney.Format.MaxDecimalPlaces", 0);
            uiBuilder.set("#TradePoolYours #YourMoney.Format.Step", 1);
        }

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged, "#TradePoolYours #YourMoney",
                new EventData().append("ActionId", "MONEY_UPDATED").append("@Amount", "#YourMoney.Value")
        );
    }

    private void buildInventoryList(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder, Ref<EntityStore> ref, Store<EntityStore> store)
    {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        ItemContainer playerStorage = playerComponent.getInventory().getCombinedStorageFirst().getContainerForSlot((short)0);
        
        Map<Integer, ItemStack> playerStacks = new HashMap<>();

        for (short i = 0; i < playerStorage.getCapacity(); i++)
        {
            ItemStack itemStack = playerStorage.getItemStack(i);

            if (itemStack != null)
            {
                playerStacks.put((int)i, itemStack);
            }
            else
            {
                emptySlotsCount++;
            }
        }

        tradeData.playersInventory.put(playerUUID, playerStacks);
    }

    private void refresh()
    {
        if (securityChecksHandle != null) securityChecksHandle.cancel(true);

        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        updateTradePanelLayout(uiBuilder);
        updateTradePanelPos(uiBuilder, eventBuilder);
        refreshLists(uiBuilder, eventBuilder);
        refreshValidation(uiBuilder, eventBuilder);

        translate(uiBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);

        securityChecksHandle = Hytrade.SCHEDULER.scheduleAtFixedRate(this::doSecurityPass, 500, 500, TimeUnit.MILLISECONDS);
    }

    private void doSecurityPass()
    {
        if (tradeData == null) return;

        UICommandBuilder uiBuilder = new UICommandBuilder();
        boolean isDirty = false;

        for (UUID uuid : tradeData.playerUUIDs)
        {
            Economy economyObj = HytradeVaultUnlocked.getEconomyObj();
            if (economyObj != null)
            {
                Float moneyTradeAmount = tradeData.playersMoneyInTrade.get(uuid);
                Float balanceAmount = economyObj.balance("Hytrade", uuid).floatValue();
                Float clampedMoneyTradeAmount = Math.clamp(moneyTradeAmount, 0, balanceAmount);

                if (!moneyTradeAmount.equals(clampedMoneyTradeAmount))
                {
                    tradeData.updateMoney(uuid, clampedMoneyTradeAmount);
                    isDirty = true;
                }
            }
        }

        if (isDirty) sendUpdate(uiBuilder);
    }

    private void refreshAllMoneyValues()
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();

        Float theirMoneyAmount = tradeData.playersMoneyInTrade.get(otherPlayerUUID);
        if (theirMoneyAmount != null)
        {
            String otherPlayerAmountStr = HytradeConfig.get().getIsMoneyDecimal() ?
                                          theirMoneyAmount.toString() :
                                          String.valueOf(((int)(float) theirMoneyAmount));

            uiBuilder.set("#TheirMoney.Text", otherPlayerAmountStr);
        }

        Float yourMoneyAmount = tradeData.playersMoneyInTrade.get(playerUUID);
        uiBuilder.set("#YourMoney.Value", yourMoneyAmount);

        sendUpdate(uiBuilder);
    }

    private void translate(UICommandBuilder uiBuilder)
    {
        String language = playerConfigData.vars.language.getValue();

        uiBuilder.set("#PanelTitle.Text", TranslationHelper.getTranslation("ui.trade.title", language));
        uiBuilder.set("#ChangeLayout.TooltipText", TranslationHelper.getTranslation("ui.trade.change_layout", language));
        uiBuilder.set("#MoveLeftBtn.TooltipText", TranslationHelper.getTranslation("ui.trade.move_left", language));
        uiBuilder.set("#MoveRightBtn.TooltipText", TranslationHelper.getTranslation("ui.trade.move_right", language));
        uiBuilder.set("#CancelBtn #Label.Text", TranslationHelper.getTranslation("ui.trade.cancel", language));
        uiBuilder.set("#InventoryTitle.Text", TranslationHelper.getTranslation("ui.trade.inventory", language));
    }

    private void updateTradePanelLayout(UICommandBuilder uiBuilder)
    {
        //uiBuilder.remove("#TradePools");
    }

    private void updateTradePanelPos(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        Anchor anchor = new Anchor();
        anchor.setWidth(Value.of(contentLayouts.get(currentLayoutIndex).size.x));
        anchor.setHeight(Value.of(contentLayouts.get(currentLayoutIndex).size.y));

        uiBuilder.set("#MoveLeftBtn.Disabled", false);
        uiBuilder.set("#MoveRightBtn.Disabled", false);

        switch (playerConfigData.vars.tradePanelPosition.getValue())
        {
            case -1 -> {
                anchor.setLeft(Value.of(20));
                uiBuilder.set("#MoveLeftBtn.Disabled", true);
            }

            case 1 -> {
                anchor.setRight(Value.of(20));
                uiBuilder.set("#MoveRightBtn.Disabled", true);
            }
        }

        uiBuilder.setObject("#TradePanel.Anchor", anchor);
    }

    private void refreshLists(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        uiBuilder.clear("#Inventory");
        uiBuilder.clear("#YoursList");
        uiBuilder.clear("#TheirsList");

        tradeData.playersInventory.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#Inventory", stackData, stackSelectorId, "ADD_ITEM", false);
        });

        tradeData.playersItemsPools.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#YoursList", stackData, stackSelectorId, "REMOVE_ITEM", false);
        });

        tradeData.playersItemsPools.get(otherPlayerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#TheirsList", stackData, stackSelectorId, "", true);
        });
    }

    private void refreshValidation(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        String errorMessage = null;
        boolean isWaiting = tradeData.hasPlayerValidated(playerUUID);

        if (emptySlotsCount + tradeData.playersItemsPools.get(playerUUID).size() < tradeData.playersItemsPools.get(otherPlayerUUID).size())
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.no_space", language);
        }
        else if (tradeData.isTradeEmpty())
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.empty", language);
        }
        else if (isWaiting)
        {
            errorMessage = TranslationHelper.getTranslation("ui.trade.error.waiting_other", language);
        }

        String label = isWaiting ?
                       TranslationHelper.getTranslation("ui.trade.waiting", language) :
                       TranslationHelper.getTranslation("ui.trade.button", language);
        boolean isDisabled = (errorMessage != null);

        updateValidationBtn(uiBuilder, isDisabled, label, errorMessage);
        updateValidationCheckmarks(uiBuilder);
    }

    private void updateValidationBtn(UICommandBuilder uiBuilder, boolean isDisabled, String text, String tooltip)
    {
        uiBuilder.set("#ValidateBtn.Disabled", isDisabled);
        uiBuilder.set("#ValidateBtn #Label.Text", text);

        if (tooltip != null && !tooltip.isEmpty())
        {
            uiBuilder.set("#ValidateBtn.TooltipText", tooltip);
        }
        else
        {
            uiBuilder.setNull("#ValidateBtn.TooltipText");
        }
    }

    private void updateValidationCheckmarks(UICommandBuilder uiBuilder)
    {
        uiBuilder.set("#YourCheckmark.Visible", tradeData.hasPlayerValidated(playerUUID));
        uiBuilder.set("#TheirCheckmark.Visible", tradeData.hasPlayerValidated(otherPlayerUUID));
    }

    private void addItemToList(UICommandBuilder uiBuilder,
                               UIEventBuilder eventBuilder,
                               String listSelector,
                               ItemStack itemStack,
                               int itemStackId,
                               String onClickActionId,
                               Boolean isDisabled)
    {
        if (listSelector == null || listSelector.length() == 0 || itemStack == null || itemStackId < 0) return;

        String groupId = "#Item%s".formatted(itemStackId);

        uiBuilder.appendInline(listSelector, """
            Group %s {
                LayoutMode: Top;
                Anchor: (Horizontal: 1);
                Padding: (Bottom: 2);
            }
        """.formatted(groupId));

        groupId = listSelector + " " + groupId;

        String itemName = TranslationHelper.getItemStackDisplayName(playerRef.getLanguage(), itemStack);
        itemName = itemName != null && itemName.length() > 0 ? itemName : TranslationHelper.getItemStackDisplayName("en-US", itemStack);
        itemName = itemName != null && itemName.length() > 0 ? itemName : "";

        uiBuilder.append(groupId, "Pages/TradePanel/Elements/ItemButton.ui");

        uiBuilder.set(groupId + " #ItemButtonLine.Disabled", isDisabled);

        uiBuilder.set(groupId + " #ItemIcon.ItemId", itemStack.getItemId());
        uiBuilder.set(groupId + " #ItemName.Text", itemName);
        uiBuilder.set(groupId + " #ItemQte.Text", "x" + itemStack.getQuantity());

        if (onClickActionId != null && !onClickActionId.isEmpty())
        {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    groupId + " #ItemButtonLine",
                    new EventData().append("ActionId", onClickActionId)
                            .append("stackId", String.valueOf(itemStackId))
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, String rawData)
    {
        JsonObject jsonObj = JsonParser.parseString(rawData).getAsJsonObject();

        String actionId = jsonObj.get("ActionId").getAsString();
        switch (actionId)
        {
            case "CHANGE_LAYOUT" -> onChangeLayoutClicked();
            case "POS_UPDATE" -> onChangePagePositionClicked(jsonObj.get("movePanelDirection").getAsShort());
            case "ADD_ITEM" -> onAddItemClicked(jsonObj.get("stackId").getAsInt());
            case "REMOVE_ITEM" -> onRemoveItemClicked(jsonObj.get("stackId").getAsInt());
            case "MONEY_UPDATED" -> onMoneyAmountUpdated(jsonObj.get("@Amount").getAsString());
            case "CANCEL" -> onCancelClicked();
            case "VALIDATE" -> onValidateClicked();
        }
        
        sendUpdate();
    }

    private void onChangeLayoutClicked()
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        currentLayoutIndex++;
        if (currentLayoutIndex > contentLayouts.size() - 1) currentLayoutIndex = 0;
        playerConfigData.vars.tradePanelLayoutName.setValue(contentLayouts.get(currentLayoutIndex).name);

        loadContentLayout(uiBuilder, eventBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);
        refresh();
    }

    private void onChangePagePositionClicked(int movePanelDirection)
    {
        int currentPos = playerConfigData.vars.tradePanelPosition.getValue();
        playerConfigData.vars.tradePanelPosition.setValue(currentPos + movePanelDirection);
        refresh();
    }

    private void onAddItemClicked(int slotId)
    {
        Map<Integer, ItemStack> playerInventory = tradeData.playersInventory.get(playerUUID);
        ItemStack stackData = playerInventory.get(slotId);
        
        playerInventory.remove(slotId);
        tradeData.playersItemsPools.get(playerUUID).put(slotId, stackData);

        tradeData.resetValidation();
    }

    private void onRemoveItemClicked(int slotId)
    {
        ItemStack stackData = tradeData.playersItemsPools.get(playerUUID).get(slotId);
        
        tradeData.playersItemsPools.get(playerUUID).remove(slotId);
        tradeData.playersInventory.get(playerUUID).put(slotId, stackData);

        tradeData.resetValidation();
    }

    private void onMoneyAmountUpdated(String strAmount)
    {
        Float amount = null;
        try
        {
            amount = Float.parseFloat(strAmount);
        }
        catch (Exception e) {}


        if (amount != null)
        {
            tradeData.updateMoney(playerUUID, amount);
            doSecurityPass();
        }
    }

    private void onCancelClicked()
    {
        cancelTrade();
    }

    private void onValidateClicked()
    {
        tradeData.addValidation(playerUUID, tradeData);
    }

    private void cancelTrade()
    {
        Hytrade.cancelTrade(tradeData);
    }
}