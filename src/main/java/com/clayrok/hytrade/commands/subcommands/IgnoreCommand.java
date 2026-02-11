package com.clayrok.hytrade.commands.subcommands;

import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.data.NotificationData;
import com.clayrok.hytrade.data.PlayerConfigData;
import com.clayrok.hytrade.helpers.NotificationHelper;
import com.clayrok.hytrade.helpers.TranslationHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.NameMatching;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class IgnoreCommand extends AbstractPlayerCommand
{
    private final RequiredArg<String> playerArg;

    public IgnoreCommand()
    {
        super("Ignore", "Toggles ignore.");
        playerArg = withRequiredArg("playerName", "Target player", ArgTypes.STRING);

        if (!HytradeConfig.get().arePermsEmpty())
        {
            String perm = HytradeConfig.get().getFullPermTrade();
            this.requirePermission(perm);
        }
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world)
    {
        UUID playerUUID = playerRef.getUuid();
        String targetPlayerName = playerArg.get(commandContext);
        PlayerRef targetPlayerRef = Universe.get().getPlayerByUsername(targetPlayerName, NameMatching.DEFAULT);

        if (targetPlayerRef == null) return;

        String targetPlayerStrUUID = targetPlayerRef.getUuid().toString();

        PlayerConfigData senderConfig = PlayerConfigData.getConfigData(playerUUID);
        String senderLanguage = senderConfig.vars.language.getValue();
        List<PlayerConfigData.IgnoredPlayer> ignoredPlayers = senderConfig.vars.ignoredPlayers.getValue();

        int playerIndex = -1;
        for (int i = 0; i < ignoredPlayers.size(); i++)
        {
            if (ignoredPlayers.get(i).uuid.equals(targetPlayerStrUUID)) playerIndex = i;
        }

        if (playerIndex != -1)
        {
            ignoredPlayers.remove(playerIndex);
            NotificationHelper.send(playerRef, new NotificationData(
                    TranslationHelper.getTranslation("notification.hytrade.title", senderLanguage),
                    TranslationHelper.getTranslation("notification.hytrade.ignore_removed", senderLanguage),
                    HytradeConfig.get().getNotificationIconId(),
                    "#ffffff", "#2ae917"
            ));
        }
        else
        {
            ignoredPlayers.add(new PlayerConfigData.IgnoredPlayer(targetPlayerName, targetPlayerStrUUID));
            NotificationHelper.send(playerRef, new NotificationData(
                    TranslationHelper.getTranslation("notification.hytrade.title", senderLanguage),
                    TranslationHelper.getTranslation("notification.hytrade.ignore_added", senderLanguage),
                    HytradeConfig.get().getNotificationIconId(),
                    "#ffffff", "#2ae917"
            ));
        }
    }
}
