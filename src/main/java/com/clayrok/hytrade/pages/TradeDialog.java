package com.clayrok.hytrade.pages;

import com.clayrok.hytrade.Hytrade;
import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.data.PlayerConfigData;
import com.clayrok.hytrade.data.EventActionData;
import com.clayrok.hytrade.helpers.JsonStr;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.List;


public class TradeDialog extends InteractiveCustomUIPage<EventActionData>
{
    private PlayerConfigData playerConfigData = null;

    private PlayerRef senderPlayerRef = null;
    private PlayerRef playerRef = null;

    public TradeDialog(@NonNullDecl PlayerRef senderPlayerRef, @NonNullDecl PlayerRef playerRef)
    {
        super(playerRef, CustomPageLifetime.CanDismiss, EventActionData.CODEC);

        playerConfigData = PlayerConfigData.getConfigData(playerRef.getUuid());

        this.senderPlayerRef = senderPlayerRef;
        this.playerRef = playerRef;
    }

    @Override
    public void build(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl UICommandBuilder uiCommandBuilder, @NonNullDecl UIEventBuilder uiEventBuilder, @NonNullDecl Store<EntityStore> store)
    {
        uiCommandBuilder.append("Pages/TradeDialog.ui");
        uiCommandBuilder.set("#SenderUsername.Text", senderPlayerRef.getUsername());

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#YesButton",
                new EventData().append("ActionDataJson", new JsonStr().add("actionId", "YES").str())
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#NoButton",
                new EventData().append("ActionDataJson", new JsonStr().add("actionId", "NO").str())
        );

        uiEventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#IgnoreButton",
                new EventData().append("ActionDataJson", new JsonStr().add("actionId", "IGNORE").str())
        );

        playOpenSound();
    }

    @Override
    public void onDismiss(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store)
    {
        super.onDismiss(ref, store);
        playCloseSound();
    }

    private void playOpenSound()
    {
        int tradeAskOpenSoundIndex = SoundEvent.getAssetMap().getIndex(HytradeConfig.get().getTradeDialogOpenSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, tradeAskOpenSoundIndex, SoundCategory.UI);
    }

    private void playCloseSound()
    {
        int tradeAskOpenSoundIndex = SoundEvent.getAssetMap().getIndex(HytradeConfig.get().getTradeDialogCloseSoundId());
        SoundUtil.playSoundEvent2dToPlayer(playerRef, tradeAskOpenSoundIndex, SoundCategory.UI);
    }

    @Override
    public void handleDataEvent(@NonNullDecl Ref<EntityStore> ref, @NonNullDecl Store<EntityStore> store, @NonNullDecl EventActionData jsonData)
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
            case "YES" -> onYesClicked();
            case "NO" -> onNoClicked();
            case "IGNORE" -> onIgnoreClicked();
        }

        sendUpdate();
    }

    private void onYesClicked()
    {
        Hytrade.openTrade(senderPlayerRef, playerRef);
    }

    private void onNoClicked()
    {
        close();
    }

    private void onIgnoreClicked()
    {
        List<PlayerConfigData.IgnoredPlayer> ignoredPlayers = playerConfigData.vars.ignoredPlayers.getValue();
        ignoredPlayers.add(new PlayerConfigData.IgnoredPlayer(senderPlayerRef.getUsername(), senderPlayerRef.getUuid().toString()));
        playerConfigData.vars.ignoredPlayers.setValue(ignoredPlayers);

        close();
    }
}