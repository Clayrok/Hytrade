package com.clayrok.plugin.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.clayrok.plugin.Trade;
import com.hypixel.hytale.server.core.inventory.ItemStack;


public class TradeData
{
    private List<Runnable> refreshFunctions = new ArrayList<>();

    public List<UUID> playerUUIDs = new ArrayList<>();
    private List<UUID> uuidsValidation = new ArrayList<>();

    public Map<UUID, Map<Integer, ItemStack>> playersInventory = new HashMap<>();
    public Map<UUID, Map<Integer, ItemStack>> playersItemsPools = new HashMap<>();

    public void addValidation(UUID playerUUID, TradeData TradeData)
    {
        uuidsValidation.add(playerUUID);
        
        if (TradeData.isValidated())
        {
            Trade.finalizeTrade(TradeData);
        }
        
        refresh();
    }

    public Boolean hasPlayerValidated(UUID playerUUID)
    {
        return uuidsValidation.contains(playerUUID);
    }

    public void resetValidation()
    {
        uuidsValidation.clear();
        refresh();
    }

    public Boolean isValidated()
    {
        return uuidsValidation.size() >= playerUUIDs.size();
    }

    public void subscribeRefresh(Runnable refreshFunction)
    {
        if (!refreshFunctions.contains(refreshFunction))
        {
            refreshFunctions.add(refreshFunction);
        }
    }

    public void refresh()
    {
        for (Runnable refreshFunction : refreshFunctions)
        {
            refreshFunction.run();
        }
    }
}