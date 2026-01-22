package com.clayrok.plugin;

import com.clayrok.plugin.commands.TradeCommand;
import com.clayrok.plugin.data.TradeData;
import com.clayrok.plugin.data.NotificationParams;
import com.clayrok.plugin.helpers.NotificationHelper;
import com.clayrok.plugin.pages.TradePage;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.checkerframework.checker.nullness.compatqual.NonNullDecl;


public class Trade extends JavaPlugin
{
    public static String notificationIconId = "Utility_Leather_Backpack";
    public static String tradeOpenSoundId = "SFX_Axe_Special_Swing";
    public static String tradeCloseSoundId = "SFX_Drag_Items_Chest";
    public static float tradeMaxDistance = 5.0f;
    
    public static List<TradeData> onGoingTrades = new ArrayList<>();

    public Trade(@NonNullDecl JavaPluginInit init)
    {
        super(init);
    }

    public static void openTrade(PlayerRef player1Ref, PlayerRef player2Ref)
    {
        int tradeOpenSoundIndex = SoundEvent.getAssetMap().getIndex(Trade.tradeOpenSoundId);

        TradeData TradeData = new TradeData();

        UUID playerUuid = player1Ref.getUuid();
        TradeData.playerUUIDs.add(playerUuid);
        TradeData.playersInventory.put(playerUuid, new HashMap<>());
        TradeData.playersItemsPools.put(playerUuid, new HashMap<>());

        UUID player2Uuid = player2Ref.getUuid();
        TradeData.playerUUIDs.add(player2Uuid);
        TradeData.playersInventory.put(player2Uuid, new HashMap<>());
        TradeData.playersItemsPools.put(player2Uuid, new HashMap<>());

        Trade.onGoingTrades.add(TradeData);

        //Open trade for player 1
        Ref<EntityStore> player1StoreRef = player1Ref.getReference();
        Store<EntityStore> player1EntityStore = player1StoreRef.getStore();
        Player player1 = player1EntityStore.getComponent(player1StoreRef, Player.getComponentType());
        
        TradePage player1TradePage = new TradePage(player1Ref, TradeData); 
        player1.getPageManager().openCustomPage(player1StoreRef, player1EntityStore, player1TradePage);

        player1.getWorld().execute(() -> {
            SoundUtil.playSoundEvent2dToPlayer(player1Ref, tradeOpenSoundIndex, SoundCategory.UI);
        });

        //Open trade for player 2
        Ref<EntityStore> player2StoreRef = player2Ref.getReference();
        Store<EntityStore> player2EntityStore = player2StoreRef.getStore();
        Player player2 = player2EntityStore.getComponent(player2StoreRef, Player.getComponentType());
        
        TradePage player2TradePage = new TradePage(player2Ref, TradeData);
        player2.getPageManager().openCustomPage(player2StoreRef, player2EntityStore, player2TradePage);

        player2.getWorld().execute(() -> {
            SoundUtil.playSoundEvent2dToPlayer(player2Ref, tradeOpenSoundIndex, SoundCategory.UI);
        });
    }

    public static void closeTrade(TradeData transaction, NotificationParams notificationParams)
    {
        for (PlayerRef playerRef : Universe.get().getPlayers())
        {
            UUID playerUUID = playerRef.getUuid();

            if (transaction.playerUUIDs.contains(playerUUID))
            {
                notificationParams.playerRef = playerRef;
                NotificationHelper.send(notificationParams);

                Ref<EntityStore> storeRef = playerRef.getReference();
                Store<EntityStore> store = storeRef.getStore();
                Player player = store.getComponent(storeRef, Player.getComponentType());
                player.getPageManager().setPage(storeRef, store, Page.None);

                int tradeCloseSoundIndex = SoundEvent.getAssetMap().getIndex(Trade.tradeCloseSoundId);
                player.getWorld().execute(() -> {
                    SoundUtil.playSoundEvent2dToPlayer(playerRef, tradeCloseSoundIndex, SoundCategory.UI);
                });
            }
        }

        Trade.onGoingTrades.remove(transaction);
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

        NotificationParams notificationParams = new NotificationParams();
        notificationParams.title = "Trade";
        notificationParams.subtitle = "Trade succeed.";
        notificationParams.iconId = Trade.notificationIconId;
        notificationParams.titleColor = "#ffffff";
        notificationParams.subtitleColor = "#2ae917";

        closeTrade(tradeData, notificationParams);
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

    @Override
    protected void setup()
    {
        getCommandRegistry().registerCommand(new TradeCommand());
    }
}