package com.clayrok.hytrade.commands;

import com.clayrok.hytrade.commands.subcommands.IgnoreCommand;
import com.clayrok.hytrade.commands.subcommands.ReloadCommand;
import com.clayrok.hytrade.commands.subcommands.SettingsCommand;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractCommandCollection;

public class HytradeCommand extends AbstractCommandCollection
{
    public HytradeCommand()
    {
        super("Hytrade", "Base command.");

        addSubCommand(new SettingsCommand());
        addSubCommand(new ReloadCommand());
        addSubCommand(new IgnoreCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }
}