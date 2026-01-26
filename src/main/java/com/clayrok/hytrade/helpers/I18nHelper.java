package com.clayrok.hytrade.helpers;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.i18n.I18nModule;

public class I18nHelper
{
    public static String getItemStackDisplayName(String language, ItemStack itemStack)
    {
        return I18nModule.get().getMessage(language, itemStack.getItem().getTranslationKey());
    }
}