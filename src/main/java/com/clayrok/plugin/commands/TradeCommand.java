package com.clayrok.plugin.commands;

import com.clayrok.plugin.Trade;
import com.clayrok.plugin.helpers.NotificationHelper;
import com.clayrok.plugin.data.NotificationParams;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.GameMode;
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

public class TradeCommand extends AbstractPlayerCommand
{
    private final RequiredArg<String> playerArg;

    public TradeCommand()
    {
        super("Trade", "Opens the player trading panel.", false);
        playerArg = withRequiredArg("playerName", "Target player", ArgTypes.STRING);

        this.setPermissionGroup(GameMode.Adventure);
        this.requirePermission(PermissionsModule.get().getBasePermission());
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef playerRef, World world)
    {
        String playerName = playerArg.get(context);
        PlayerRef targetPlayerRef = Universe.get().getPlayerByUsername(playerName, NameMatching.DEFAULT);
        
        try
        {
            if (targetPlayerRef == null) throw new Exception("Player not found.");
            if (Trade.isPlayerTrading(playerRef)) throw new Exception("You're already in an trade.");
            
            Vector3d player1Pos = playerRef.getTransform().getPosition();
            Vector3d player2Pos = targetPlayerRef.getTransform().getPosition();
            if (player1Pos.distanceTo(player2Pos) > Trade.tradeMaxDistance) throw new Exception("Player too far away.");
            
            Trade.openTrade(playerRef, targetPlayerRef);
        }
        catch (Exception e)
        {
            NotificationParams notificationParams = new NotificationParams();
            notificationParams.playerRef = playerRef;
            notificationParams.title = "Trade";
            notificationParams.subtitle = e.getMessage();
            notificationParams.iconId = Trade.notificationIconId;
            notificationParams.titleColor = "#ffffff";
            notificationParams.subtitleColor = "#f57482";

            NotificationHelper.send(notificationParams);
        }
    }
}