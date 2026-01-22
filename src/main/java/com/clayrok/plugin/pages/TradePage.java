package com.clayrok.plugin.pages;

import com.clayrok.plugin.Trade;
import com.clayrok.plugin.data.NotificationParams;
import com.clayrok.plugin.data.EventActionData;
import com.clayrok.plugin.data.TradeData;
import com.clayrok.plugin.helpers.I18nHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class TradePage extends InteractiveCustomUIPage<EventActionData>
{
    private TradeData TradeData = null;
    private UUID playerUUID = null;
    private UUID otherPlayerUUID = null;
    private Integer emptySlotsCount = 0;

    public TradePage(PlayerRef playerRef, TradeData TradeData)
    {
        super(playerRef, CustomPageLifetime.CantClose, EventActionData.CODEC);

        TradeData.subscribeRefresh(() -> this.refresh());

        this.TradeData = TradeData;
        playerUUID = playerRef.getUuid();
        otherPlayerUUID = TradeData.playerUUIDs.get(TradeData.playerUUIDs.indexOf(playerUUID) == 0 ? 1 : 0);
    }
    
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref,
                      @NonNullDecl UICommandBuilder uiCommandBuilder,
                      @NonNullDecl UIEventBuilder uiEventBuilder,
                      @NonNullDecl Store<EntityStore> store)
    {
        uiCommandBuilder.append("Pages/TradePanel.ui");

        String playerUsername = playerRef.getUsername();
        String otherPlayerUsername = Universe.get().getPlayer(otherPlayerUUID).getUsername();

        uiCommandBuilder.set("#YourUsername.Text", playerUsername);
        uiCommandBuilder.set("#TheirUsername.Text", otherPlayerUsername);

        buildInventoryList(ref, uiCommandBuilder, uiEventBuilder, store);
        refresh();
    }

    private void buildInventoryList(Ref<EntityStore> ref, UICommandBuilder uiCommandBuilder, UIEventBuilder uiEventBuilder, Store<EntityStore> store)
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

        TradeData.playersInventory.put(playerUUID, playerStacks);

        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#CancelBtn",
            new EventData()
                .append("ActionId", "CANCEL")
        );

        uiEventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#ValidateBtn",
            new EventData()
                .append("ActionId", "VALIDATE")
        );
    }

    private void refresh()
    {
        UICommandBuilder uiBuilder = new UICommandBuilder();
        UIEventBuilder eventBuilder = new UIEventBuilder();

        uiBuilder.clear("#Inventory");
        uiBuilder.clear("#YoursList");
        uiBuilder.clear("#TheirsList");

        TradeData.playersInventory.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#Inventory", stackData, stackSelectorId, "ADD_ITEM", false);
        });

        TradeData.playersItemsPools.get(playerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#YoursList", stackData, stackSelectorId, "REMOVE_ITEM", false);
        });

        TradeData.playersItemsPools.get(otherPlayerUUID).forEach((stackSelectorId, stackData)  -> {
            addItemToList(uiBuilder, eventBuilder, "#TheirsList", stackData, stackSelectorId, "", true);
        });

        if (emptySlotsCount + TradeData.playersItemsPools.get(playerUUID).size() < TradeData.playersItemsPools.get(otherPlayerUUID).size())
        {
            uiBuilder.set("#ValidateBtn.Disabled", true);
            uiBuilder.set("#ValidateBtn.TooltipText", "Not enough space.");
        }
        else if (TradeData.playersItemsPools.get(playerUUID).size() == 0 && TradeData.playersItemsPools.get(otherPlayerUUID).size() == 0)
        {
            uiBuilder.set("#ValidateBtn.Disabled", true);
            uiBuilder.set("#ValidateBtn.TooltipText", "No item added to trade.");
        }
        else if (TradeData.hasPlayerValidated(playerUUID))
        {
            uiBuilder.set("#ValidateBtn.Disabled", true);
            uiBuilder.set("#ValidateBtn.TooltipText", "Waiting for other player validation...");
        }
        else
        {
            uiBuilder.set("#ValidateBtn.Disabled", false);
            uiBuilder.setNull("#ValidateBtn.TooltipText");
        }

        sendUpdate(uiBuilder, eventBuilder, false);
    }

    private void addItemToList(UICommandBuilder uiBuilder, UIEventBuilder eventBuilder, String listSelector, ItemStack itemStack, int itemStackId, String onClickActionId, Boolean isDisabled)
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
        
        uiBuilder.append(groupId, "Pages/TradeItemButton.ui");

        uiBuilder.set(groupId + " #ItemButton.Disabled", isDisabled);

        uiBuilder.set(groupId + " #ItemIcon.ItemId", itemStack.getItemId());
        uiBuilder.set(groupId + " #ItemName.Text", I18nHelper.getItemStackDisplayName(playerRef.getLanguage(), itemStack));
        uiBuilder.set(groupId + " #ItemQte.Text", "x" + itemStack.getQuantity());

        if (onClickActionId != null && onClickActionId.length() > 0)
        {
            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                groupId + " #ItemButton",
                new EventData()
                    .append("ActionId", onClickActionId)
                    .append("ActionDataJson", "{\"stackId\" : \"%s\"}".formatted(itemStackId))
            );
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, EventActionData data)
    {
        JsonObject obj = null;
        if (data.actionDataJson != null)
        {
            obj = JsonParser.parseString(data.actionDataJson).getAsJsonObject();
        }
        
        switch (data.actionId)
        {
            case "ADD_ITEM":
                onItemAdded(obj.get("stackId").getAsInt());
                break;
        
            case "REMOVE_ITEM":
                onItemRemoved(obj.get("stackId").getAsInt());
                break;

            case "CANCEL":
                onCancel();
                break;

            case "VALIDATE":
                onValidate();
                break;

            default:
                break;
        }
        
        sendUpdate();
    }

    private void onItemAdded(int slotId)
    {
        Map<Integer, ItemStack> playerInventory = TradeData.playersInventory.get(playerUUID);
        ItemStack stackData = playerInventory.get(slotId);
        
        playerInventory.remove(slotId);
        TradeData.playersItemsPools.get(playerUUID).put(slotId, stackData);

        TradeData.resetValidation();
    }

    private void onItemRemoved(int slotId)
    {
        ItemStack stackData = TradeData.playersItemsPools.get(playerUUID).get(slotId);
        
        TradeData.playersItemsPools.get(playerUUID).remove(slotId);
        TradeData.playersInventory.get(playerUUID).put(slotId, stackData);

        TradeData.resetValidation();
    }

    private void onCancel()
    {
        NotificationParams notificationParams = new NotificationParams();
        notificationParams.playerRef = playerRef;
        notificationParams.title = "Trade";
        notificationParams.subtitle = "Trade cancelled.";
        notificationParams.iconId = Trade.notificationIconId;
        notificationParams.titleColor = "#ffffff";
        notificationParams.subtitleColor = "#f57482";

        Trade.closeTrade(TradeData, notificationParams);
    }

    private void onValidate()
    {
        UICommandBuilder uiCommandBuilder = new UICommandBuilder();
        uiCommandBuilder.set("#ValidateBtn.Disabled", true);

        TradeData.addValidation(playerUUID, TradeData);
    }
}