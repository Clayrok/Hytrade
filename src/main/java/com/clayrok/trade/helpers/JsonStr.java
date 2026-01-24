package com.clayrok.trade.helpers;

import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Map;

public class JsonStr
{
    Map<String, String> properties = new HashMap<>();

    public JsonStr add(String name, Object value)
    {
        properties.put(name, String.valueOf(value));
        return this;
    }

    public String str()
    {
        JsonObject jsonObject = new JsonObject();
        properties.forEach(jsonObject::addProperty);

        return jsonObject.toString();
    }
}