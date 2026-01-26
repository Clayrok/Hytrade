package com.clayrok.hytrade.pages;

import com.clayrok.hytrade.Hytrade;
import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.data.*;
import com.clayrok.hytrade.helpers.I18nHelper;
import com.clayrok.hytrade.helpers.JsonStr;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
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

import java.util.*;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class TradePanel extends InteractiveCustomUIPage<EventActionData>
{
    private final List<TradeContentLayoutData> contentLayouts = TradeContentLayoutData.getAllLayouts();
    private int currentLayoutIndex = 0;

    private PlayerConfigData playerConfigData = null;

    private TradeData tradeData = null;
    private UUID playerUUID = null;
    private UUID otherPlayerUUID = null;
    private Integer emptySlotsCount = 0;

    public TradePanel(PlayerRef playerRef, TradeData TradeData)
    {
        super(playerRef, CustomPageLifetime.CantClose, EventActionData.CODEC);

        TradeData.subscribeRefresh(this::refresh);

        this.tradeData = TradeData;
        playerUUID = playerRef.getUuid();
        otherPlayerUUID = TradeData.playerUUIDs.get(TradeData.playerUUIDs.indexOf(playerUUID) == 0 ? 1 : 0);

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
    }

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiBuilder,
                      @NonNullDecl UIEventBuilder eventBuilder,
                      @NonNullDecl Store<EntityStore> store)
    {
        uiBuilder.append("Pages/TradePanel/TradePanel.ui");

        loadContentLayout(uiBuilder, eventBuilder);
        buildInventoryList(uiBuilder, eventBuilder, ref, store);

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveLeftBtn",
                new EventData().append("ActionDataJson", new JsonStr()
                        .add("actionId", "POS_UPDATE")
                        .add("movePanelDirection", -1).str())
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#MoveRightBtn",
                new EventData().append("ActionDataJson", new JsonStr()
                        .add("actionId", "POS_UPDATE")
                        .add("movePanelDirection", 1).str())
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ChangeLayout",
                new EventData().append("ActionDataJson", new JsonStr()
                        .add("actionId", "CHANGE_LAYOUT").str())
        );

        refresh();

        playOpenSound();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store)
    {
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
        uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/TradeBottomButtons.ui");

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
                new EventData().append("ActionDataJson", new JsonStr()
                        .add("actionId", "CANCEL").str())
        );

        eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#ValidateBtn",
                new EventData().append("ActionDataJson", new JsonStr()
                        .add("actionId", "VALIDATE").str())
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
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        updateTradePanelLayout(uiBuilder);
        updateTradePanelPos(uiBuilder, eventBuilder);
        refreshLists(uiBuilder, eventBuilder);
        refreshValidation(uiBuilder, eventBuilder);

        sendUpdate(uiBuilder, eventBuilder, false);
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

    private  void refreshValidation(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder)
    {
        if (emptySlotsCount + tradeData.playersItemsPools.get(playerUUID).size() < tradeData.playersItemsPools.get(otherPlayerUUID).size())
        {
            updateValidationBtn(uiBuilder, true, "Trade", "Not enough space.");
        }
        else if (tradeData.playersItemsPools.get(playerUUID).isEmpty() && tradeData.playersItemsPools.get(otherPlayerUUID).isEmpty())
        {
            updateValidationBtn(uiBuilder, true, "Trade", "No item added to hytrade.");
        }
        else if (tradeData.hasPlayerValidated(playerUUID))
        {
            updateValidationBtn(uiBuilder, true, "Waiting...", "Waiting for other player validation...");
        }
        else
        {
            updateValidationBtn(uiBuilder, false, "Trade", null);
        }

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
        String groupId = "#Item%s".formatted(itemStackId);

        uiBuilder.appendInline(listSelector, """
            Group %s {
                LayoutMode: Top;
                Anchor: (Horizontal: 1);
                Padding: (Bottom: 2);
            }
        """.formatted(groupId));

        groupId = listSelector + " " + groupId;
        
        uiBuilder.append(groupId, "Pages/TradePanel/TradeItemButton.ui");

        uiBuilder.set(groupId + " #ItemButtonLine.Disabled", isDisabled);

        uiBuilder.set(groupId + " #ItemIcon.ItemId", itemStack.getItemId());
        uiBuilder.set(groupId + " #ItemName.Text", I18nHelper.getItemStackDisplayName(playerRef.getLanguage(), itemStack));
        uiBuilder.set(groupId + " #ItemQte.Text", "x" + itemStack.getQuantity());

        if (onClickActionId != null && !onClickActionId.isEmpty())
        {
            eventBuilder.addEventBinding(
                    CustomUIEventBindingType.Activating,
                    groupId + " #ItemButtonLine",
                    new EventData().append("ActionDataJson", new JsonStr()
                            .add("actionId", onClickActionId)
                            .add("stackId", itemStackId).str())
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventActionData jsonData)
    {
        JsonObject obj = null;
        if (jsonData.actionDataJson != null)
        {
            obj = JsonParser.parseString(jsonData.actionDataJson).getAsJsonObject();
        }
        else
        {
            sendUpdate();
            return;
        }

        String actionId = obj.get("actionId").getAsString();
        switch (actionId)
        {
            case "CHANGE_LAYOUT" -> onChangeLayoutClicked();
            case "POS_UPDATE" -> onChangePagePositionClicked(obj.get("movePanelDirection").getAsShort());
            case "ADD_ITEM" -> onAddItemClicked(obj.get("stackId").getAsInt());
            case "REMOVE_ITEM" -> onRemoveItemClicked(obj.get("stackId").getAsInt());
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