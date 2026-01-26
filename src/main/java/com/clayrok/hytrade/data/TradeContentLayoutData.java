package com.clayrok.hytrade.data;

import com.hypixel.hytale.protocol.Vector2i;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TradeContentLayoutData
{
    public String name;
    public Vector2i size;
    public String iconPath;
    public String layoutIconSelector;
    public String layoutButtonSelector;
    public Consumer<UICommandBuilder> buildFunction;

    public TradeContentLayoutData(String name, Vector2i size, String iconPath,
                                  String layoutIconSelector,
                                  String layoutButtonSelector,
                                  Consumer<UICommandBuilder> buildFunction)
    {
        this.name = name;
        this.size = size;
        this.iconPath = iconPath;
        this.layoutIconSelector = layoutIconSelector;
        this.layoutButtonSelector = layoutButtonSelector;
        this.buildFunction = buildFunction;
    }

    public static List<TradeContentLayoutData> getAllLayouts()
    {
        List<TradeContentLayoutData> layouts = new ArrayList<>();

        layouts.add(new TradeContentLayoutData(
                "Horizontal",
                new Vector2i(950, 650),
                "Pages/Icons/horizontalLayout.png",
                "#HorizontalLayoutIcon",
                "#HorizontalLayoutBtn",
                uiBuilder -> {
                    uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/ContentLayouts/HorizontalTradeContent.ui");
                    uiBuilder.insertBefore("#TradePools", "Pages/TradePanel/InventoryContainer.ui");
                    uiBuilder.insertBeforeInline("#TradePools", "Group { Anchor: (Width: 5); }");
                    uiBuilder.append("#TradePools", "Pages/TradePanel/TradePoolYours.ui");
                    uiBuilder.appendInline("#TradePools", "Group { Anchor: (Height: 5); }");
                    uiBuilder.append("#TradePools", "Pages/TradePanel/TradePoolTheirs.ui");
                }));

        layouts.add(new TradeContentLayoutData(
                "Vertical",
                new Vector2i(450, 950),
                "Pages/Icons/verticalLayout.png",
                "#VerticalLayoutIcon",
                "#VerticalLayoutBtn",
                uiBuilder -> {
                    uiBuilder.append("#TradePanel #Content", "Pages/TradePanel/ContentLayouts/VerticalTradeContent.ui");
                    uiBuilder.append("#ItemLists", "Pages/TradePanel/InventoryContainer.ui");
                    uiBuilder.append("#ItemLists", "Pages/TradePanel/TradePoolYours.ui");
                    uiBuilder.append("#ItemLists", "Pages/TradePanel/TradePoolTheirs.ui");
                }));

        return layouts;
    }
}