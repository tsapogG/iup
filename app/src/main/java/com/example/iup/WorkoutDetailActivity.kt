package com.example.iup

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

import android.graphics.Color

class WorkoutDetailActivity : AppCompatActivity() {

    private lateinit var workoutTextView: TextView
    private lateinit var completeButton: Button
    private lateinit var notCompletedButton: Button // Кнопка для "Не выполнил"
    private lateinit var sharedPrefs: SharedPreferences
    private var workoutIndex: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout_detail)


        // Инициализируем элементы UI
        workoutTextView = findViewById(R.id.workoutTextView)  // Убедитесь, что у вас есть TextView для отображения описания
        completeButton = findViewById(R.id.completeButton)
        notCompletedButton = findViewById(R.id.notCompletedButton) // Добавляем кнопку для "Не выполнил"
        sharedPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        backButton.setOnClickListener {
            // Возвращаемся на предыдущий экран
            onBackPressed()
        }

        // Получаем индекс тренировки и описание из Intent
        workoutIndex = intent.getIntExtra("WORKOUT_INDEX", -1)
        val workoutDescription = intent.getStringExtra("WORKOUT_TEXT")

        // Устанавливаем описание тренировки в TextView
        workoutTextView.text = workoutDescription

        // Получаем состояние тренировки
        val isCompleted = sharedPrefs.getBoolean("WORKOUT_COMPLETED_$workoutIndex", false)
        val isNotCompleted = sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$workoutIndex", false)

        // Устанавливаем состояние кнопок
        if (isCompleted) {
            completeButton.text = "Отменить"
            completeButton.setBackgroundColor(Color.parseColor("#F94144"))
        } else {
            completeButton.text = "Выполнить"
            completeButton.setBackgroundColor(Color.parseColor("#4D908E"))
        }

        if (isNotCompleted) {
            notCompletedButton.text = "Отменить"
            notCompletedButton.setBackgroundColor(Color.parseColor("#F94144"))
        } else {
            notCompletedButton.text = "Не выполнено"
            notCompletedButton.setBackgroundColor(Color.parseColor("#4D908E"))
        }

        // Логика для кнопки "Выполнил"
        completeButton.setOnClickListener {
            val editor = sharedPrefs.edit()

            if (!isCompleted) {
                editor.putBoolean("WORKOUT_COMPLETED_$workoutIndex", true).apply()
                completeButton.text = "Отменить"
                completeButton.setBackgroundColor(Color.parseColor("#F94144"))
            } else {
                editor.putBoolean("WORKOUT_COMPLETED_$workoutIndex", false).apply()
                completeButton.text = "Выполнить"
                completeButton.setBackgroundColor(Color.parseColor("#4D908E"))
            }

            // Отправляем результат обратно в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("WORKOUT_INDEX", workoutIndex)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Закрываем текущую активность
        }

        // Логика для кнопки "Не выполнил"
        notCompletedButton.setOnClickListener {
            val editor = sharedPrefs.edit()

            if (!isNotCompleted) {
                // Если тренировка не была помечена как "не выполнена"
                editor.putBoolean("WORKOUT_NOT_COMPLETED_$workoutIndex", true).apply()
                notCompletedButton.text = "Отменить"
                notCompletedButton.setBackgroundColor(Color.parseColor("#F94144")) // Красный цвет
            } else {
                // Если тренировка уже была помечена как "не выполнена"
                editor.putBoolean("WORKOUT_NOT_COMPLETED_$workoutIndex", false).apply()
                notCompletedButton.text = "Не выполнено"
                notCompletedButton.setBackgroundColor(Color.parseColor("#4D908E")) // Цвет по умолчанию
            }

            // Отправляем результат обратно в MainActivity
            val resultIntent = Intent()
            resultIntent.putExtra("WORKOUT_INDEX", workoutIndex)
            setResult(Activity.RESULT_OK, resultIntent)
            finish() // Закрываем текущую активность
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()  // Это возвращает на предыдущий экран
    }
}