package com.clayrok.hytrade;

import com.clayrok.hytrade.commands.HytradeCommand;
import com.clayrok.hytrade.commands.TradeCommand;
import com.clayrok.hytrade.data.PlayerConfigData;
import com.clayrok.hytrade.data.TradeData;
import com.clayrok.hytrade.data.NotificationData;
import com.clayrok.hytrade.helpers.NotificationHelper;
import com.clayrok.hytrade.pages.Settings;
import com.clayrok.hytrade.pages.TradeDialog;
import com.clayrok.hytrade.pages.TradePanel;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class Hytrade extends JavaPlugin
{
    public final static List<TradeData> onGoingTrades = new ArrayList<>();

    private final EventRegistry events = getEventRegistry();


    public Hytrade(@NonNullDecl JavaPluginInit init)
    {
        super(init);
    }

    @Override
    protected void setup()
    {
        getCommandRegistry().registerCommand(new TradeCommand());
        getCommandRegistry().registerCommand(new HytradeCommand());

        events.register(PlayerDisconnectEvent.class, event -> {
            PlayerRef playerRef = event.getPlayerRef();

            for (TradeData tradeData : onGoingTrades)
            {
                if (tradeData.playerUUIDs.contains(playerRef.getUuid()))
                {
                    Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
                        cancelTrade(tradeData);
                    });
                }
            }
        });
    }

    public static void askForTrade(PlayerRef senderPlayerRef, PlayerRef targetPlayerRef)
    {
        Universe.get().getWorld(targetPlayerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = targetPlayerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            PlayerConfigData senderPlayerConfigData = PlayerConfigData.getConfigData(senderPlayerRef.getUuid());
            senderPlayerConfigData.vars.lastTradeRequestEpochTime.setValue(new PlayerConfigData.LastTradeRequestSent(
                    Instant.now().getEpochSecond(),
                    targetPlayerRef.getUuid().toString()
            ));

            TradeDialog tradeDialog = new TradeDialog(senderPlayerRef, targetPlayerRef);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, tradeDialog);
        });
    }

    public static void openTrade(PlayerRef senderPlayerRef, PlayerRef targetPlayerRef)
    {
        TradeData TradeData = new TradeData();

        UUID playerUuid = senderPlayerRef.getUuid();
        TradeData.playerUUIDs.add(playerUuid);
        TradeData.playersInventory.put(playerUuid, new HashMap<>());
        TradeData.playersItemsPools.put(playerUuid, new HashMap<>());

        UUID player2Uuid = targetPlayerRef.getUuid();
        TradeData.playerUUIDs.add(player2Uuid);
        TradeData.playersInventory.put(player2Uuid, new HashMap<>());
        TradeData.playersItemsPools.put(player2Uuid, new HashMap<>());

        Hytrade.onGoingTrades.add(TradeData);

        openPlayerTrade(senderPlayerRef, TradeData);
        if(!playerUuid.equals(player2Uuid)) openPlayerTrade(targetPlayerRef, TradeData);
    }

    private static void openPlayerTrade(PlayerRef playerRef, TradeData tradeData)
    {
        Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            TradePanel playerTradePanel = new TradePanel(playerRef, tradeData);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, playerTradePanel);
        });
    }

    public static void cancelTrade(TradeData tradeData)
    {
        NotificationData notificationData = new NotificationData();
        notificationData.title = "Trade";
        notificationData.subtitle = "Trade cancelled.";
        notificationData.iconId = HytradeConfig.get().getNotificationIconId();
        notificationData.titleColor = "#ffffff";
        notificationData.subtitleColor = "#f57482";

        closeTrade(tradeData, notificationData);
    }

    public static void closeTrade(TradeData tradeData, NotificationData notificationData)
    {
        if (tradeData != null)
        {
            for (UUID playerUUID : tradeData.playerUUIDs)
            {
                PlayerRef playerRef = Universe.get().getPlayer(playerUUID);
                if (playerRef == null) continue;

                Ref<EntityStore> storeRef = playerRef.getReference();
                Store<EntityStore> store = storeRef.getStore();
                Player player = store.getComponent(storeRef, Player.getComponentType());

                notificationData.playerRef = playerRef;
                NotificationHelper.send(notificationData);

                player.getPageManager().setPage(storeRef, store, Page.None);
            }

            Hytrade.onGoingTrades.remove(tradeData);
        }
    }

    public static void finalizeTrade(TradeData tradeData)
    {
        //Player 1 - Put stacks in a temporary container
        UUID playerUuid1 = tradeData.playerUUIDs.get(0);
        PlayerRef playerRef1 = Universe.get().getPlayer(playerUuid1);
        Ref<EntityStore> ref1 = playerRef1.getReference();
        Store<EntityStore> store1 = ref1.getStore();
        Player player1 = store1.getComponent(ref1, Player.getComponentType());
        ItemContainer itemContainer1 = player1.getInventory().getCombinedStorageFirst().getContainerForSlot((short)0);

        Map<Integer, ItemStack> playerAddedStacks1 = tradeData.playersItemsPools.get(playerUuid1);
        ItemContainer transitionContainer1 = SimpleItemContainer.getNewContainer((short)playerAddedStacks1.size());
        playerAddedStacks1.forEach((stackSelectorId, itemStack) ->  {
            ItemStackTransaction transaction = itemContainer1.removeItemStack(itemStack);
            transitionContainer1.addItemStack(transaction.getQuery());
        });

        //Player 2 - Put stacks in a temporary container
        UUID playerUuid2 = tradeData.playerUUIDs.get(1);
        PlayerRef playerRef2 = Universe.get().getPlayer(playerUuid2);
        Ref<EntityStore> ref2 = playerRef2.getReference();
        Store<EntityStore> store2 = ref2.getStore();
        Player player2 = store2.getComponent(ref2, Player.getComponentType());
        ItemContainer itemContainer2 = player2.getInventory().getCombinedStorageFirst().getContainerForSlot((short)0);

        Map<Integer, ItemStack> playerAddedStacks2 = tradeData.playersItemsPools.get(playerUuid2);
        ItemContainer transitionContainer2 = SimpleItemContainer.getNewContainer((short)playerAddedStacks2.size());
        playerAddedStacks2.forEach((stackSelectorId, itemStack) ->  {
            ItemStackTransaction transaction = itemContainer2.removeItemStack(itemStack);
            transitionContainer2.addItemStack(transaction.getQuery());
        });

        //Both - Distribute temporary containers stacks
        transitionContainer1.moveAllItemStacksTo(itemContainer2);
        transitionContainer2.moveAllItemStacksTo(itemContainer1);

        NotificationData notificationData = new NotificationData();
        notificationData.title = "Trade";
        notificationData.subtitle = "Trade succeed.";
        notificationData.iconId = HytradeConfig.get().getNotificationIconId();
        notificationData.titleColor = "#ffffff";
        notificationData.subtitleColor = "#2ae917";

        closeTrade(tradeData, notificationData);
    }
    
    public static Boolean isPlayerTrading(PlayerRef playerRef)
    {
        UUID playerUuid = playerRef.getUuid();
        for (TradeData tradeData : onGoingTrades)
        {
            if (tradeData.playerUUIDs.contains(playerUuid)) return true;
        }

        return false;
    }

    public static void openSettings(PlayerRef playerRef)
    {
        Universe.get().getWorld(playerRef.getWorldUuid()).execute(() -> {
            Ref<EntityStore> playerStoreRef = playerRef.getReference();
            Store<EntityStore> playerEntityStore = playerStoreRef.getStore();
            Player player = playerEntityStore.getComponent(playerStoreRef, Player.getComponentType());

            Settings playerTradePanel = new Settings(playerRef);
            player.getPageManager().openCustomPage(playerStoreRef, playerEntityStore, playerTradePanel);
        });
    }
}