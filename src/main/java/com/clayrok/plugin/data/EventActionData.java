package com.clayrok.plugin.data;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class EventActionData
{
    public String actionId;
    public String actionDataJson;
    
    public static final BuilderCodec<EventActionData> CODEC = 
        BuilderCodec.builder(EventActionData.class, EventActionData::new)
        .append(
            new KeyedCodec<>("ActionId", Codec.STRING), 
            (obj, val) -> obj.actionId = val,
            obj -> obj.actionId
        )
        .add()
        .append(
            new KeyedCodec<>("ActionDataJson", Codec.STRING), 
            (obj, val) -> obj.actionDataJson = val,
            obj -> obj.actionDataJson
        )
        .add()
        .build();
}