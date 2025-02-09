package com.example.iup.utils

import java.util.Calendar

data class Day(
    val date: Calendar,
    var isTrainingDay: Boolean = false,
    var workoutIndex: Int = -1, // Индекс тренировки из workouts
    var completed: Boolean = false,
    var notCompleted: Boolean = false,
    var isRestDay: Boolean = false, // Флаг "день отдыха",
    var workoutDescription: String? = null
)