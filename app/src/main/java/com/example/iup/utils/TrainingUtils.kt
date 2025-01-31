package com.example.iup.utils

import android.content.Context
import org.json.JSONObject

data class Training(val id: Int, val title: String, val description: String)

fun loadTrainings(context: Context): Map<Int, Training> {
    val assetManager = context.assets
    val inputStream = assetManager.open("trainings.json")
    val jsonString = inputStream.readBytes().toString(Charsets.UTF_8)

    val jsonObject = JSONObject(jsonString)
    val trainings = mutableMapOf<Int, Training>()

    jsonObject.keys().forEach { key ->
        val dayId = key.toInt()
        val trainingObj = jsonObject.getJSONObject(key)
        val title = trainingObj.getString("title")
        val description = trainingObj.getString("description")
        trainings[dayId] = Training(dayId, title, description)
    }
    return trainings
}