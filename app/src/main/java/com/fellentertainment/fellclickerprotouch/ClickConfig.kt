package com.fellentertainment.fellclickerprotouch

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ClickProfile(
    val name: String,
    val intervalMs: Long = 100L,
    val randomOffsetMs: Long = 20L,
    val holdDurationMs: Long = 15L,
    val doubleClick: Boolean = false,
    val maxClicks: Int = 0,
    val maxTimeSeconds: Int = 0,
    val targetX: Int = 500,
    val targetY: Int = 1000
)

object ConfigManager {
    private const val PREF_NAME = "fellclicker_prefs"
    private const val KEY_PROFILES = "saved_profiles"

    var currentConfig = ClickProfile(name = "Standard")
    var isClicking = false
    var isOverlayActive = false

    fun saveProfile(context: Context, profile: ClickProfile) {
        val profiles = getProfiles(context).toMutableList()
        profiles.removeAll { it.name.equals(profile.name, ignoreCase = true) }
        profiles.add(profile)
        saveProfileList(context, profiles)
    }

    fun deleteProfile(context: Context, name: String) {
        val profiles = getProfiles(context).filterNot { it.name == name }
        saveProfileList(context, profiles)
    }

    fun getProfiles(context: Context): List<ClickProfile> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_PROFILES, "[]") ?: "[]"
        val list = mutableListOf<ClickProfile>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ClickProfile(
                    name = obj.getString("name"),
                    intervalMs = obj.getLong("intervalMs"),
                    randomOffsetMs = obj.getLong("randomOffsetMs"),
                    holdDurationMs = obj.optLong("holdDurationMs", 15L),
                    doubleClick = obj.optBoolean("doubleClick", false),
                    maxClicks = obj.getInt("maxClicks"),
                    maxTimeSeconds = obj.getInt("maxTimeSeconds"),
                    targetX = obj.getInt("targetX"),
                    targetY = obj.getInt("targetY")
                )
            )
        }
        return list
    }

    private fun saveProfileList(context: Context, list: List<ClickProfile>) {
        val array = JSONArray()
        list.forEach { p ->
            val obj = JSONObject().apply {
                put("name", p.name)
                put("intervalMs", p.intervalMs)
                put("randomOffsetMs", p.randomOffsetMs)
                put("holdDurationMs", p.holdDurationMs)
                put("doubleClick", p.doubleClick)
                put("maxClicks", p.maxClicks)
                put("maxTimeSeconds", p.maxTimeSeconds)
                put("targetX", p.targetX)
                put("targetY", p.targetY)
            }
            array.put(obj)
        }
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_PROFILES, array.toString()).apply()
    }
}