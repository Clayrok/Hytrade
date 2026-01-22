package com.clayrok.plugin.helpers;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.util.NotificationUtil;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.clayrok.plugin.data.NotificationParams;
import com.hypixel.hytale.protocol.ItemWithAllMetadata;


public class NotificationHelper
{
    public static void send(NotificationParams params)
    {
        var packetHandler = params.playerRef.getPacketHandler();

        var primaryMessage = Message.raw(params.title).color(params.titleColor);
        var secondaryMessage = Message.raw(params.subtitle).color(params.subtitleColor);
        
        var icon = new ItemStack(params.iconId, 1).toPacket();

        NotificationUtil.sendNotification(
            packetHandler,
            primaryMessage,
            secondaryMessage,
            (ItemWithAllMetadata) icon
        );
    }
}