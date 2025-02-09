package com.example.iup

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle

import androidx.appcompat.app.AppCompatActivity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import java.util.Calendar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.children
import androidx.core.view.forEach
import androidx.core.view.setMargins
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var calendarContainer: LinearLayout
    private var daysList = mutableListOf<Day>()
    var benchPress = 0
    var squat = 0
    var deadlift = 0
    var opa = 0 // Переменная для корректировки индексов тренировок
    private var trainingStartDate: Calendar = Calendar.getInstance()
    private var experienceLevel: Int = 0 // 0 - Новичок, 1 - Любитель, 2 - Профессионал
    private var loadPercentage: Int = 0

    data class Day(
        val date: Calendar,
        var isTrainingDay: Boolean = false,
        var workoutIndex: Int = -1, // Индекс тренировки из workouts
        var completed: Boolean = false,
        var notCompleted: Boolean = false,
        var isRestDay: Boolean = false // Флаг "день отдыха"
    )
    // Инициализируем workouts значением по умолчанию
    private var workouts: Array<String> = arrayOf("No workouts available")

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("TrainingPrefs", MODE_PRIVATE)
        calendarContainer = findViewById(R.id.calendarContainer)

        val settingsButton = findViewById<Button>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            openSettingsDialog()
        }
        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.setOnClickListener {
            showResetConfirmationDialog() // Показываем диалог подтверждения
        }

        if (isFirstRun()) {
            showSetupDialog()
        } else {
            loadSavedData()
            daysList = loadDaysFromSharedPreferences() // Загружаем список дней
            generateWorkouts()
            generateCalendar(trainingStartDate)
        }
        val loadFactor2 = 1 + (loadPercentage / 100.0)
        val benchPressNew = String.format("%.2f", benchPress * loadFactor2)
        val squatNew = String.format("%.2f", squat * loadFactor2)
        val deadliftPressNew = String.format("%.2f", deadlift * loadFactor2)

        //String.format("%.2f",deadlift * loadFactor2 )


        // Находим TextView в разметке
        val benchPressText = findViewById<TextView>(R.id.benchPressText)
        val squatText = findViewById<TextView>(R.id.squatText)
        val deadliftText = findViewById<TextView>(R.id.deadliftText)

        // Устанавливаем текст в TextView
        benchPressText.text = "Новый максимальный жим: $benchPressNew"
        squatText.text = "Новый максимальный присед: $squatNew"
        deadliftText.text = "Новый максимальный становая тяга: $deadliftPressNew"

    }


    private fun isFirstRun(): Boolean {
        return sharedPreferences.getBoolean("isFirstRun", true)
    }

    private fun openSettingsDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Настройки")
        builder.setMessage("Вы хотите сменить план тренировок?")

        builder.setPositiveButton("Сменить план") { _, _ ->
            showPlanInputDialog()
        }

        builder.setNegativeButton("Отмена") { dialog, _ ->
            dialog.dismiss()
        }

        builder.create().show()
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun showPlanDialog(isFirstSetup: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_plan, null)
        val benchPressInput = dialogView.findViewById<EditText>(R.id.benchPressInput)
        val squatInput = dialogView.findViewById<EditText>(R.id.squatInput)
        val deadliftInput = dialogView.findViewById<EditText>(R.id.deadliftInput)
        val startDateButton = dialogView.findViewById<Button>(R.id.startDateButton)
        val selectedDateTextView = dialogView.findViewById<TextView>(R.id.selectedDateTextView)
        val experienceLevelSpinner = dialogView.findViewById<Spinner>(R.id.experienceLevelSpinner)
        val loadPercentageSeekBar = dialogView.findViewById<SeekBar>(R.id.loadPercentageSeekBar)
        val loadPercentageTextView = dialogView.findViewById<TextView>(R.id.loadPercentageTextView)

        // Настройка Spinner
        val levels = resources.getStringArray(R.array.experience_levels)
        val levelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        experienceLevelSpinner.adapter = levelAdapter

        // Предзаполнение данных, если это изменение плана
        if (!isFirstSetup) {
            benchPressInput.setText(benchPress.toString())
            squatInput.setText(squat.toString())
            deadliftInput.setText(deadlift.toString())
            experienceLevelSpinner.setSelection(experienceLevel) // 0 - Новичок, 1 - Любитель, 2 - Профессионал
            loadPercentageSeekBar.progress =
                loadPercentage // Устанавливаем прогресс SeekBar на сохраненное значение
            loadPercentageTextView.text =
                "Процент нагрузки: $loadPercentage%" // Обновляем текст с процентом

            // Настройка SeekBar в зависимости от уровня
            when (experienceLevel) {
                0 -> { // Новичок
                    loadPercentageSeekBar.max = 15
                }

                1 -> { // Любитель
                    loadPercentageSeekBar.max = 7
                }

                2 -> { // Профессионал
                    loadPercentageSeekBar.max = 5
                }
            }
        }

        // Настройка SeekBar в зависимости от уровня
        experienceLevelSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    loadPercentageSeekBar.visibility = View.VISIBLE
                    loadPercentageTextView.visibility = View.VISIBLE

                    when (position) {
                        0 -> { // Новичок
                            loadPercentageSeekBar.max = 15
                        }

                        1 -> { // Любитель
                            loadPercentageSeekBar.max = 7
                        }

                        2 -> { // Профессионал
                            loadPercentageSeekBar.max = 5
                        }
                    }
                    loadPercentageTextView.text =
                        "Процент нагрузки: ${loadPercentageSeekBar.progress}%"
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        // Обновление текста при изменении SeekBar
        loadPercentageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                loadPercentageTextView.text =
                    "Процент нагрузки: $progress%" // Обновление текста при изменении ползунка
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Установка выбранной даты
        startDateButton.setOnClickListener {
            val today = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    trainingStartDate.set(year, month, dayOfMonth)
                    updateDateUI(selectedDateTextView)
                    saveData() // Сохраняем новые данные сразу после выбора
                },
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
            ).show()
        }


        // Обработчик сохранения данных
        AlertDialog.Builder(this)
            .setTitle(if (isFirstSetup) "Введите данные" else "Измените план тренировок")
            .setView(dialogView)
            .setCancelable(!isFirstSetup)
            .setPositiveButton("Сохранить") { _, _ ->
                benchPress = getIntFromInput(benchPressInput)
                squat = getIntFromInput(squatInput)
                deadlift = getIntFromInput(deadliftInput)
                experienceLevel = experienceLevelSpinner.selectedItemPosition
                loadPercentage = loadPercentageSeekBar.progress

                Log.d(
                    "PlanDialog",
                    "benchPress=$benchPress, squat=$squat, deadlift=$deadlift, level=$experienceLevel, load=$loadPercentage%"
                )

                saveData()
                generateWorkouts()
                val loadFactor2 = 1 + (loadPercentage / 100.0)
                val benchPressNew = String.format("%.2f", benchPress * loadFactor2)
                val squatNew = String.format("%.2f", squat * loadFactor2)
                val deadliftPressNew = String.format("%.2f", deadlift * loadFactor2)

                //String.format("%.2f",deadlift * loadFactor2 )


                // Находим TextView в разметке
                val benchPressText = findViewById<TextView>(R.id.benchPressText)
                val squatText = findViewById<TextView>(R.id.squatText)
                val deadliftText = findViewById<TextView>(R.id.deadliftText)

                // Устанавливаем текст в TextView
                benchPressText.text = "Новый максимальный жим: $benchPressNew"
                squatText.text = "Новый максимальный присед: $squatNew"
                deadliftText.text = "Новый максимальный становая тяга: $deadliftPressNew"
                generateCalendar(trainingStartDate)
            }
            .setNegativeButton("Отмена") { _, _ ->
                if (isFirstSetup) finish()
            }
            .show()

    }


    private fun saveData() {
        with(sharedPreferences.edit()) {
            putInt("benchPress", benchPress)
            putInt("squat", squat)
            putInt("deadlift", deadlift)
            putLong("startDate", trainingStartDate.timeInMillis) // Сохраняем дату в миллисекундах
            putBoolean("isFirstRun", false) // Устанавливаем isFirstRun в false
            putInt("loadPercentage", loadPercentage) // Сохраняем процент нагрузки
            putInt("experienceLevel", experienceLevel) // Сохраняем уровень подготовки
            putInt("opa", opa) // Сохраняем значение opa
            apply()
        }
        Log.d(
            "SaveData",
            "Data saved: benchPress=$benchPress, squat=$squat, deadlift=$deadlift, startDate=${trainingStartDate.time}, opa=$opa"
        )
    }

    private fun updateDateUI(selectedDateTextView: TextView) {
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(trainingStartDate.time)
        selectedDateTextView.text = formattedDate
    }


    private fun loadSavedData() {
        benchPress = sharedPreferences.getInt("benchPress", 0)
        squat = sharedPreferences.getInt("squat", 0)
        deadlift = sharedPreferences.getInt("deadlift", 0)
        loadPercentage = sharedPreferences.getInt("loadPercentage", 0)
        experienceLevel = sharedPreferences.getInt("experienceLevel", 0)
        val startDateMillis = sharedPreferences.getLong("startDate", 0)
        if (startDateMillis != 0L) {
            trainingStartDate.timeInMillis = startDateMillis
        }
        opa = sharedPreferences.getInt("opa", 0) // Загружаем значение opa
        Log.d(
            "LoadData",
            "Data loaded: benchPress=$benchPress, squat=$squat, deadlift=$deadlift, startDate=${trainingStartDate.time}, opa=$opa"
        )
    }

    private fun showSetupDialog() {
        showPlanDialog(isFirstSetup = true)

    }

    private fun showPlanInputDialog() {
        showPlanDialog(isFirstSetup = false)
    }

    private fun getIntFromInput(inputField: EditText): Int {
        val inputText = inputField.text.toString()
        return if (inputText.isNotEmpty()) inputText.toInt() else 0
    }


    @SuppressLint("DefaultLocale")
    private fun generateWorkouts() {
        // Вычисляем коэффициент нагрузки на основе выбранного процента
        val loadFactor = 1 + (loadPercentage / 100.0)


        // Обновляем тренировочный план на основе введенных данных и коэффициента нагрузки
        val benchPressNew = benchPress * loadFactor
        val squatNew = squat * loadFactor
        val deadliftPressNew = deadlift * loadFactor

        // Создаем тренировочный план
        workouts = arrayOf(
            "Тренировка 1: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 2: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 3: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 4: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 5: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 6: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 7: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 8: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 9: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 10: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 11: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 12: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 13: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 14: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 15: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 16: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 17: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 18: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 19: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 20: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 21: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 22: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 23: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 24: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 25: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 26: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 27: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 28: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 29: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 30: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 31: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 32: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 33: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 34: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 35: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 36: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 37: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 38: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 39: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 40: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 41: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 42: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 43: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 44: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 45: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 46: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 47: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
            "Тренировка 48: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPress * 0.6)} kg \n" +
                    "2. 5 x ${String.format("%.2f", benchPress * 0.65)} kg \n" +
                    "3. 5 x ${String.format("%.2f", benchPress * 0.7)} kg \n" +
                    "4. 4 x ${String.format("%.2f", benchPress * 0.75)} kg \n" +
                    "5. 3 x ${String.format("%.2f", benchPressNew * 0.8)} kg \n" +
                    "6. 3 подхода по 2 повторения x ${String.format("%.2f", benchPressNew * 0.85)} kg \n\n",
        )

        Log.d("GenerateWorkouts", "Workouts updated: ${workouts.joinToString()}")
    }

    private fun showResetConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Подтвердите действие")
        builder.setMessage("Вы уверены, что хотите сбросить все настройки?")
        builder.setPositiveButton("Да") { _, _ ->
            resetToFactorySettings() // Вызываем метод сброса
        }
        builder.setNegativeButton("Нет") { dialog, _ ->
            dialog.dismiss() // Закрываем диалог без изменений
        }
        builder.create().show()
    }
    private fun resetToFactorySettings() {
        val sharedPreferences = getSharedPreferences("TrainingPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Очищаем все настройки
        editor.clear()
        editor.putBoolean("isFirstRun", true) // Устанавливаем isFirstRun в true
        editor.apply()
        opa = 0

        // Очищаем состояние тренировок
        val workoutPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        val workoutEditor = workoutPrefs.edit()
        workoutEditor.clear()
        workoutEditor.apply()

        // Восстанавливаем начальный массив тренировок
        generateWorkouts()

        // Перегенерируем календарь
        generateCalendar(trainingStartDate)

        // Перезапускаем активность для применения изменений
        recreate()
    }

    private val WORKOUT_REQUEST_CODE = 100
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == WORKOUT_REQUEST_CODE && resultCode == RESULT_OK) {
            // Получаем индекс тренировки
            val workoutIndex = data?.getIntExtra("WORKOUT_INDEX", -1) ?: -1

            // Проверяем, что индекс корректный
            if (workoutIndex < 0 || workoutIndex >= workouts.size) {
                Log.e("MainActivity", "Invalid workout index: $workoutIndex")
                return
            }

            // Получаем состояние тренировки из SharedPreferences
            val sharedPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
            val isCompleted = sharedPrefs.getBoolean("WORKOUT_COMPLETED_$workoutIndex", false)
            val isNotCompleted = sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$workoutIndex", false)

            // Если тренировка помечена как "не выполненная", сдвигаем тренировки
            if (isNotCompleted) {
                shiftWorkouts(workoutIndex) // Вызываем метод сдвига тренировок
            }

            // Перегенерируем календарь для применения изменений
            generateCalendar(trainingStartDate)
        }
    }



    // Generate Calendar

    fun generateCalendar(startDate: Calendar) {
        val calendar = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Начинаем с понедельника
        }
        val endDate = (startDate.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 16) }

        calendarContainer.removeAllViews()

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val monthContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        daysList.clear() // Очищаем список дней

        while (calendar.before(endDate)) {
            val monthSection = createMonthSection(calendar) // Создаем секцию для каждого месяца
            monthContainer.addView(monthSection)

            val gridLayout = createDayGridLayout() // Создаем сетку для дней месяца
            val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfMonth - Calendar.MONDAY + 7) % 7

            repeat(emptyCells) {
                addEmptyDay(gridLayout)
            }

            while (calendar.get(Calendar.MONTH) == monthSection.tag as Int && calendar.before(endDate)) {
                val dayIndex = ((calendar.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                val isTrainingDay = dayIndex >= 0 && (dayIndex % 7 == 0 || dayIndex % 7 == 2 || dayIndex % 7 == 4)

                val workoutIndex = if (isTrainingDay) {
                    (dayIndex / 7) * 3 + listOf(0, 2, 4).indexOf(dayIndex % 7) + opa
                } else {
                    -1 // Для пустых дней
                }

                val day = Day(
                    date = calendar.clone() as Calendar,
                    isTrainingDay = isTrainingDay,
                    workoutIndex = workoutIndex,
                    completed = false,
                    notCompleted = false,
                    isRestDay = false
                )

                daysList.add(day) // Добавляем день в список
                addDayView(day, gridLayout, startDate) // Добавляем представление дня в UI

                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }

            val remainingCells = (7 - (gridLayout.childCount % 7)) % 7
            repeat(remainingCells) {
                addEmptyDay(gridLayout)
            }

            monthSection.addView(gridLayout)
        }

        scrollView.addView(monthContainer)
        rootLayout.addView(scrollView)
        calendarContainer.addView(rootLayout)

        saveDaysToSharedPreferences(daysList) // Сохраняем список дней
    }

    // Create a section for each month
    private fun createMonthSection(calendar: Calendar): LinearLayout {
        val monthIndex = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val monthName = DateFormatSymbols().months[monthIndex]

        val monthTitle = TextView(this).apply {
            text = "$monthName $year"
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(16, 16, 16, 16)
        }

        val monthLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(8, 16, 8, 16)
            tag = monthIndex
        }

        monthLayout.addView(monthTitle)
        return monthLayout
    }

    // Create grid layout for each month's days
    private fun createDayGridLayout(): GridLayout {
        return GridLayout(this).apply {
            columnCount = 7 // 7 days in a week
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 16, 0, 16)
        }
    }

    // Add empty day to the grid layout
    private fun addEmptyDay(gridLayout: GridLayout) {
        val emptyView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            }
        }
        gridLayout.addView(emptyView)
    }

    // Add day view to the grid layout
    private fun addDayView(day: Day, gridLayout: GridLayout, startDate: Calendar) {
        val displayMetrics = gridLayout.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val leftPadding = (24 * displayMetrics.density).toInt()
        val rightPadding = (22 * displayMetrics.density).toInt()
        val margin = (4 * displayMetrics.density).toInt()
        val totalMargins = margin * 6 + leftPadding + rightPadding
        val cellSize = ((screenWidth - totalMargins) / 8).toInt()

        val layoutParams = GridLayout.LayoutParams().apply {
            width = cellSize
            height = cellSize
            setMargins(margin)
        }

        if (day.isTrainingDay) {
            val sharedPrefs = gridLayout.context.getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
            val trainingIndex = daysList.indexOf(day)
            val isCompleted = sharedPrefs.getBoolean("WORKOUT_COMPLETED_$trainingIndex", false)
            val isNotCompleted = sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$trainingIndex", false)

            val dayView = TextView(this).apply {
                text = "${day.date.get(Calendar.DAY_OF_MONTH)}"
                textSize = 14f
                gravity = Gravity.CENTER
                this.layoutParams = layoutParams
                background = GradientDrawable().apply {
                    setColor(
                        when {
                            isCompleted -> Color.parseColor("#90BE6D") // Зеленый для выполненных
                            isNotCompleted -> Color.parseColor("#F94144") // Красный для не выполненных
                            else -> Color.parseColor("#4D908E") // Синий для обычных
                        }
                    )
                    cornerRadius = 10f * resources.displayMetrics.density
                }
                tag = trainingIndex // Устанавливаем тег как индекс тренировки
            }

            dayView.setOnClickListener {
                if (!isCompleted && !isNotCompleted && day.workoutIndex != -1) {
                    val intent = Intent(this@MainActivity, WorkoutDetailActivity::class.java)
                    intent.putExtra("WORKOUT_INDEX", it.tag as Int)
                    intent.putExtra("WORKOUT_TEXT", workouts[day.workoutIndex])
                    startActivityForResult(intent, WORKOUT_REQUEST_CODE)
                }
            }

            gridLayout.addView(dayView)
        } else {
            val dayView = TextView(this).apply {
                text = day.date.get(Calendar.DAY_OF_MONTH).toString()
                textSize = 14f
                gravity = Gravity.CENTER
                this.layoutParams = layoutParams
                background = GradientDrawable().apply {
                    setColor(
                        if (day.isRestDay) {
                            Color.GRAY // Серый цвет для дней отдыха
                        } else {
                            Color.LTGRAY // Более светлый серый для пустых дней
                        }
                    )
                    cornerRadius = 10f * resources.displayMetrics.density
                }
            }

            dayView.setOnClickListener {
                if (day.isRestDay || day.workoutIndex == -1) {
                    // Открываем экран "День отдыха"
                    val intent = Intent(this@MainActivity, RestDayActivity::class.java)
                    startActivity(intent)
                }
            }

            gridLayout.addView(dayView)
        }
    }

    private fun saveDaysToSharedPreferences(daysList: List<Day>) {
        val editor = sharedPreferences.edit()
        for ((index, day) in daysList.withIndex()) {
            editor.putLong("DAY_${index}_DATE", day.date.timeInMillis) // Сохраняем время в миллисекундах
            editor.putBoolean("DAY_${index}_IS_TRAINING_DAY", day.isTrainingDay)
            editor.putInt("DAY_${index}_WORKOUT_INDEX", day.workoutIndex)
            editor.putBoolean("DAY_${index}_COMPLETED", day.completed)
            editor.putBoolean("DAY_${index}_NOT_COMPLETED", day.notCompleted)
            editor.putBoolean("DAY_${index}_IS_REST_DAY", day.isRestDay) // Сохраняем флаг "день отдыха"
        }
        editor.apply()
    }

    private fun loadDaysFromSharedPreferences(): MutableList<Day> {
        val daysList = mutableListOf<Day>()
        var index = 0

        while (sharedPreferences.contains("DAY_${index}_DATE")) {
            val dateMillis = sharedPreferences.getLong("DAY_${index}_DATE", 0)
            val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val isTrainingDay = sharedPreferences.getBoolean("DAY_${index}_IS_TRAINING_DAY", false)
            val workoutIndex = sharedPreferences.getInt("DAY_${index}_WORKOUT_INDEX", -1)
            val completed = sharedPreferences.getBoolean("DAY_${index}_COMPLETED", false)
            val notCompleted = sharedPreferences.getBoolean("DAY_${index}_NOT_COMPLETED", false)
            val isRestDay = sharedPreferences.getBoolean("DAY_${index}_IS_REST_DAY", false)

            daysList.add(Day(date, isTrainingDay, workoutIndex, completed, notCompleted, isRestDay))
            index++
        }

        return daysList
    }


    private fun shiftWorkouts(failedWorkoutIndex: Int) {
        val failedDay = daysList.find { it.workoutIndex == failedWorkoutIndex } ?: return

        // Помечаем текущий день как "не выполненный"
        failedDay.notCompleted = true
        failedDay.completed = false

        // Находим индекс дня в списке
        val failedDayPosition = daysList.indexOf(failedDay)

        // Добавляем "День отдыха" после не выполненной тренировки
        val restDay = Day(
            date = failedDay.date.clone() as Calendar,
            isTrainingDay = false,
            workoutIndex = -1,
            completed = false,
            notCompleted = false,
            isRestDay = true
        )
        daysList.add(failedDayPosition + 1, restDay)

        // Находим следующий тренировочный день и делаем его днем отдыха
        for (i in failedDayPosition + 2 until daysList.size) {
            if (daysList[i].isTrainingDay) {
                // Преобразуем следующий тренировочный день в день отдыха
                daysList[i].isTrainingDay = false
                daysList[i].workoutIndex = -1
                daysList[i].isRestDay = true
                break // Останавливаемся после первого найденного тренировочного дня
            }
        }

        // Корректируем индексы всех последующих тренировочных дней
        for (i in failedDayPosition + 3 until daysList.size) {
            if (daysList[i].isTrainingDay) {
                daysList[i].workoutIndex -= 2 // Уменьшаем индекс на 2
            }
        }

        // Увеличиваем значение opa
        opa -= 2

        // Сохраняем обновленные данные
        saveData()
        saveDaysToSharedPreferences(daysList)

        // Перегенерируем календарь
        generateCalendar(trainingStartDate)
    }
}
