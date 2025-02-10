package com.example.iup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences

import android.os.Bundle

import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import android.graphics.Color
import android.util.Log
import com.example.iup.MainActivity
class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var workoutTextView: TextView
    private lateinit var completeButton: Button
    private lateinit var notCompletedButton: Button // Кнопка для "Не выполнил"
    private lateinit var sharedPrefs: SharedPreferences
    private var dayIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)

        workoutTextView = findViewById(R.id.workoutTextView)
        completeButton = findViewById(R.id.completeButton)
        notCompletedButton = findViewById(R.id.notCompletedButton)
        sharedPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            onBackPressed() // Возвращаемся на предыдущий экран
        }

        // Получаем индекс тренировки и описание из Intent
        dayIndex = intent.getIntExtra("DAY_INDEX", -1)
        val workoutDescription = intent.getStringExtra("WORKOUT_TEXT")
        Log.d("IIIIIIIIIIIIIIIIIIIIIIIIIIII", "IIIIIIIIII = $dayIndex")
        if (dayIndex < 0 || dayIndex >= 48) { // Проверяем корректность индекса
            Log.e("WorkoutDetailActivity", "Invalid workout index: $dayIndex")
            finish() // Закрываем активность, если индекс некорректный
            return
        }

        workoutTextView.text = workoutDescription

        completeButton.setOnClickListener {
            val editor = sharedPrefs.edit()
            if (!sharedPrefs.getBoolean("WORKOUT_COMPLETED_$dayIndex", false)) {
                editor.putBoolean("WORKOUT_COMPLETED_$dayIndex", true).apply()
                completeButton.text = "Отменить"
                completeButton.setBackgroundColor(Color.parseColor("#F94144"))

                // Обновляем состояние тренировочного дня в SharedPreferences
                saveTrainingDayState(dayIndex, completed = true, notCompleted = false)
            } else {
                editor.putBoolean("WORKOUT_COMPLETED_$dayIndex", false).apply()
                completeButton.text = "Выполнить"
                completeButton.setBackgroundColor(Color.parseColor("#4D908E"))

                // Сбрасываем состояние тренировочного дня
                saveTrainingDayState(dayIndex, completed = false, notCompleted = false)
            }

            // Отправляем результат обратно в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("DAY_INDEX", dayIndex) // Передаем индекс тренировки
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

// Логика для кнопки "Не выполнил"
        notCompletedButton.setOnClickListener {
            val editor = sharedPrefs.edit()
            if (!sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$dayIndex", false)) {
                editor.putBoolean("WORKOUT_NOT_COMPLETED_$dayIndex", true).apply()
                notCompletedButton.text = "Отменить"
                notCompletedButton.setBackgroundColor(Color.parseColor("#F94144"))

                // Обновляем состояние тренировочного дня в SharedPreferences
                saveTrainingDayState(dayIndex, completed = false, notCompleted = true)
            } else {
                editor.putBoolean("WORKOUT_NOT_COMPLETED_$dayIndex", false).apply()
                notCompletedButton.text = "Не выполнено"
                notCompletedButton.setBackgroundColor(Color.parseColor("#4D908E"))

                // Сбрасываем состояние тренировочного дня
                saveTrainingDayState(dayIndex, completed = false, notCompleted = false)
            }

            // Отправляем результат обратно в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("DAY_INDEX", dayIndex) // Передаем индекс тренировки
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

    }
    private fun saveTrainingDayState(index: Int, completed: Boolean, notCompleted: Boolean) {
        val trainingPrefs = getSharedPreferences("TrainingPrefs", MODE_PRIVATE)
        val editor = trainingPrefs.edit()

        // Обновляем состояние "выполнено" и "не выполнено"
        editor.putBoolean("TRAINING_DAY_${index}_COMPLETED", completed)
        editor.putBoolean("TRAINING_DAY_${index}_NOT_COMPLETED", notCompleted)

        editor.apply()
    }

    override fun onBackPressed() {
        super.onBackPressed()  // Это возвращает на предыдущий экран
    }
}