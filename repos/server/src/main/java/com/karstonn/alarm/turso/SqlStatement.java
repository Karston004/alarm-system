package com.karstonn.alarm.turso;

import com.google.gson.JsonObject;

import java.util.List;

public record SqlStatement(
        String sql,
        List<JsonObject> args
) {
}