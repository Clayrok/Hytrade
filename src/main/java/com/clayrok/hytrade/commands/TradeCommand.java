package com.clayrok.hytrade.commands;

import com.clayrok.hytrade.Hytrade;
import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.data.PlayerConfigData;
import com.clayrok.hytrade.helpers.NotificationHelper;
import com.clayrok.hytrade.data.NotificationData;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class TradeCommand extends AbstractPlayerCommand
{
    private final RequiredArg<String> playerArg;

    public TradeCommand()
    {
        super("Trade", "Sends a trade request.", false);
        playerArg = withRequiredArg("playerName", "Target player", ArgTypes.STRING);

        if (!HytradeConfig.get().arePermsEmpty()) this.requirePermission(HytradeConfig.get().getFullPermTrade());
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world)
    {
        UUID playerUUID = playerRef.getUuid();

        String targetPlayerName = playerArg.get(context);
        PlayerRef targetPlayerRef = Universe.get().getPlayerByUsername(targetPlayerName, NameMatching.DEFAULT);

        try
        {
            if (targetPlayerRef == null) throw new Exception("Player not found.");

            UUID targetPlayerUUID = targetPlayerRef.getUuid();
            if (playerUUID.equals(targetPlayerUUID)) throw new Exception("You cannot trade with yourself.");

            if (Hytrade.isPlayerTrading(playerRef)) throw new Exception("You are already trading.");
            if (isOnCooldown(playerUUID)) throw new Exception("Please wait before trading again.");

            PlayerConfigData targetConfig = PlayerConfigData.getConfigData(targetPlayerUUID);
            if (targetConfig.vars.tradeIgnore.getValue()) return;

            if (isTooFar(playerRef, targetPlayerRef)) throw new Exception("Player too far away.");

            if (Hytrade.isPlayerTrading(targetPlayerRef)) return;
            if (isIgnored(playerUUID, targetPlayerUUID)) return;

            Hytrade.askForTrade(playerRef, targetPlayerRef);
        }
        catch (Exception e)
        {
            NotificationData notificationData = new NotificationData();
            notificationData.playerRef = playerRef;
            notificationData.title = "Trade";
            notificationData.subtitle = e.getMessage();
            notificationData.iconId = HytradeConfig.get().getNotificationIconId();
            notificationData.titleColor = "#ffffff";
            notificationData.subtitleColor = "#f57482";

            NotificationHelper.send(notificationData);
        }
    }

    private Boolean isIgnored(UUID senderUUID, UUID targetUUID)
    {
        PlayerConfigData targetPlayerConfigData = PlayerConfigData.getConfigData(targetUUID);

        List<PlayerConfigData.IgnoredPlayer> ignoredList = targetPlayerConfigData.vars.ignoredPlayers.getValue();
        for (PlayerConfigData.IgnoredPlayer ignoredPlayer : ignoredList)
        {
            if (ignoredPlayer.uuid.equals(senderUUID.toString())) return true;
        }

        return false;
    }

    private boolean isOnCooldown(UUID playerUUID)
    {
        PlayerConfigData senderConfig = PlayerConfigData.getConfigData(playerUUID);
        long lastTradeRequestEpochTime = senderConfig.vars.lastTradeRequestEpochTime.getValue().unixTime;
        return Instant.now().getEpochSecond() - lastTradeRequestEpochTime < HytradeConfig.get().getTradeRequestCooldownSeconds();
    }

    private boolean isTooFar(PlayerRef sender, PlayerRef target)
    {
        boolean tradeFromFarPerm =
                HytradeConfig.get().arePermsEmpty() ||
                PermissionsModule.get().hasPermission(sender.getUuid(), HytradeConfig.get().getFullPermFromFar());

        Vector3d player1Pos = sender.getTransform().getPosition();
        Vector3d player2Pos = target.getTransform().getPosition();

        return !tradeFromFarPerm && player1Pos.distanceTo(player2Pos) > HytradeConfig.get().getTradeMaxDistance();
    }
}