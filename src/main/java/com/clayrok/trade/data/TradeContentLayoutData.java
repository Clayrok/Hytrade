package com.clayrok.trade.data;

import com.hypixel.hytale.protocol.Vector2i;

import java.util.ArrayList;
import java.util.List;

public class TradeContentLayoutData
{
    public String name;
    public Vector2i size;
    public String uiPath;
    public String layoutIconSelector;

    public TradeContentLayoutData(String name, Vector2i size, String uiPath, String layoutIconSelector)
    {
        this.name = name;
        this.size = size;
        this.uiPath = uiPath;
        this.layoutIconSelector = layoutIconSelector;
    }

    public static List<TradeContentLayoutData> getAllLayouts()
    {
        List<TradeContentLayoutData> layouts = new ArrayList<>();

        layouts.add(new TradeContentLayoutData(
                "Horizontal",
                new Vector2i(950, 650),
                "Pages/HorizontalTradeContent.ui",
                "#HorizontalLayoutIcon"));

        layouts.add(new TradeContentLayoutData(
                "Vertical",
                new Vector2i(450, 950),
                "Pages/VerticalTradeContent.ui",
                "#VerticalLayoutIcon"));

        return layouts;
    }
}