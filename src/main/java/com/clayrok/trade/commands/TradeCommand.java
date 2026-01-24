package com.clayrok.trade.commands;

import com.clayrok.trade.Trade;
import com.clayrok.trade.helpers.NotificationHelper;
import com.clayrok.trade.data.NotificationData;
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

import java.util.UUID;

public class TradeCommand extends AbstractPlayerCommand
{
    private static String permBase = "clayrok.trade.";

    private final RequiredArg<String> playerArg;

    public TradeCommand()
    {
        super("Trade", "Opens the player trading panel.", false);
        playerArg = withRequiredArg("playerName", "Target player", ArgTypes.STRING);

        this.requirePermission(permBase + "trade");
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world)
    {
        UUID playerUUID = playerRef.getUuid();
        boolean tradeFromFarPerm = PermissionsModule.get().hasPermission(playerUUID, permBase + "fromfar");

        String playerName = playerArg.get(context);
        PlayerRef targetPlayerRef = Universe.get().getPlayerByUsername(playerName, NameMatching.DEFAULT);
        
        try
        {
            if (targetPlayerRef == null) throw new Exception("Player not found.");
            if (Trade.isPlayerTrading(playerRef)) throw new Exception("You're already in an trade.");
            
            Vector3d player1Pos = playerRef.getTransform().getPosition();
            Vector3d player2Pos = targetPlayerRef.getTransform().getPosition();
            if (!tradeFromFarPerm && player1Pos.distanceTo(player2Pos) > Trade.tradeMaxDistance) throw new Exception("Player too far away.");
            
            Trade.openTrade(playerRef, targetPlayerRef);
        }
        catch (Exception e)
        {
            NotificationData notificationData = new NotificationData();
            notificationData.playerRef = playerRef;
            notificationData.title = "Trade";
            notificationData.subtitle = e.getMessage();
            notificationData.iconId = Trade.notificationIconId;
            notificationData.titleColor = "#ffffff";
            notificationData.subtitleColor = "#f57482";

            NotificationHelper.send(notificationData);
        }
    }
}