package com.clayrok.hytrade.commands.subcommands;

import com.clayrok.hytrade.Hytrade;
import com.clayrok.hytrade.HytradeConfig;
import com.clayrok.hytrade.data.NotificationData;
import com.clayrok.hytrade.data.PlayerConfigData;
import com.clayrok.hytrade.helpers.NotificationHelper;
import com.clayrok.hytrade.helpers.TranslationHelper;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class ReloadCommand extends AbstractPlayerCommand
{
    public ReloadCommand()
    {
        super("reload", "Reloads the mod configuration.");
        this.requirePermission("clayrok.hytrade.reload");
    }

    @Override
    protected void execute(@NonNullDecl CommandContext commandContext, @NonNullDecl Store<EntityStore> store, @NonNullDecl Ref<EntityStore> ref, @NonNullDecl PlayerRef playerRef, @NonNullDecl World world)
    {
        String language = PlayerConfigData.getConfigData(playerRef.getUuid()).vars.language.getValue();

        HytradeConfig.get().reload();
        TranslationHelper.loadAllTranslations();

        NotificationData notificationData = new NotificationData(
                TranslationHelper.getTranslation("notification.hytrade.title", language),
                TranslationHelper.getTranslation("notification.config_reloaded", language),
                HytradeConfig.get().getNotificationIconId(),
                "#ffffff", "#2ae917"
        );

        NotificationHelper.send(playerRef, notificationData);
    }
}