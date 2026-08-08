package com.christian.ocoochchopstopmk2

import org.json.JSONObject

fun main() {
    val responseBody = """{"device_id":"test-1","settings":{"10ft_stop_head":26.6,"6ft_stop_head":2.6,"8ft_stop_head":2.6,"accel":8000,"direction":"RIGHT","max_delay":320,"max_step_position":166044,"min_delay":6,"min_step_position":0,"speed":20000,"step_position":0,"steps_per_inch":1775.36,"stop_head":"8ft","table_length":"8ft"}}"""
    val rootJson = JSONObject(responseBody)
    
    val settingsJson = if (rootJson.has("settings")) {
        val settingsValue = rootJson.get("settings")
        settingsValue as? JSONObject
            ?: if (settingsValue is String) {
                try {
                    JSONObject(settingsValue)
                } catch (_: Exception) {
                    rootJson
                }
            } else {
                rootJson
            }
    } else {
        rootJson
    }
    
    println("has speed: " + settingsJson.has("speed"))
    if (settingsJson.has("speed")) println("speed: " + settingsJson.getInt("speed"))
    println("has accel: " + settingsJson.has("accel"))
    if (settingsJson.has("accel")) println("accel: " + settingsJson.getInt("accel"))
}
