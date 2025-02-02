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
import androidx.core.view.setMargins
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var calendarContainer: LinearLayout
    private var benchPress = 0
    private var squat = 0
    private var deadlift = 0
    private var trainingStartDate: Calendar = Calendar.getInstance()
    private var experienceLevel: Int = 0 // 0 - Новичок, 1 - Любитель, 2 - Профессионал
    private var loadPercentage: Int = 0

    // Инициализируем workouts значением по умолчанию
    private var workouts: Array<String> = arrayOf("No workouts available")

    @SuppressLint("DefaultLocale")
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

        if (isFirstRun()) {
            showSetupDialog()
        } else {
            loadSavedData()
            generateWorkouts() // Генерируем план тренировок сразу после загрузки данных
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
            putBoolean("isFirstRun", false) // Устанавливаем, что первое открытие завершено
            putInt("loadPercentage", loadPercentage) // Сохраняем процент нагрузки
            putInt("experienceLevel", experienceLevel) // Сохраняем уровень
            apply()
        }
        Log.d(
            "SaveData",
            "Data saved: benchPress=$benchPress, squat=$squat, deadlift=$deadlift, startDate=${trainingStartDate.time}"
        )
    }

    private fun updateDateUI(selectedDateTextView: TextView) {
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            .format(trainingStartDate.time)
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
        Log.d(
            "LoadData",
            "Data loaded: benchPress=$benchPress, squat=$squat, deadlift=$deadlift, startDate=${trainingStartDate.time}"
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n",
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
                    "6. 3 подхода по 2 повторения x ${
                        String.format(
                            "%.2f",
                            benchPressNew * 0.85
                        )
                    } kg \n\n"


        )

        Log.d("GenerateWorkouts", "Workouts updated: ${workouts.joinToString()}")
    }

    private val WORKOUT_REQUEST_CODE = 100
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WORKOUT_REQUEST_CODE && resultCode == RESULT_OK) {
            // Получаем индекс тренировки, чтобы обновить день в календаре
            val workoutIndex = data?.getIntExtra("WORKOUT_INDEX", -1) ?: return

            // Получаем состояние тренировки из SharedPreferences
            val sharedPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
            val isCompleted = sharedPrefs.getBoolean("WORKOUT_COMPLETED_$workoutIndex", false)
            val isNotCompleted =
                sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$workoutIndex", false)

            // Если тренировка помечена как "не выполнена", сдвигаем тренировки
            if (isNotCompleted) {
                shiftWorkouts(workoutIndex) // Сдвигаем тренировки
            }

            // Найдем день в календаре и обновим его цвет
            val dayView = findViewById<ViewGroup>(calendarContainer.id)
                ?.findViewWithTag<TextView>(workoutIndex)

            // Обновляем фон дня в календаре в зависимости от выполнения тренировки
            dayView?.background = GradientDrawable().apply {
                setColor(
                    when {
                        isCompleted -> Color.parseColor("#90BE6D") // Зеленый для выполненных
                        isNotCompleted -> Color.parseColor("#F94144") // Красный для не выполненных
                        else -> Color.parseColor("#4D908E") // Синий для обычных
                    }
                )
                cornerRadius = 10f * resources.displayMetrics.density
            }

            // Обновляем календарь, если тренировка сдвинута
            generateCalendar(trainingStartDate)
        }
    }


    // Generate Calendar
    fun generateCalendar(startDate: Calendar) {
        val calendar = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Start the calendar from Monday
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
                0, // Use weight to fill space
                1f
            )
        }

        val monthContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        while (calendar.before(endDate)) {
            val monthSection = createMonthSection(calendar) // Create section for each month
            monthContainer.addView(monthSection)

            val gridLayout = createDayGridLayout() // Create grid layout for the month

            val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfMonth - Calendar.MONDAY + 7) % 7
            repeat(emptyCells) {
                addEmptyDay(gridLayout)
            }

            while (calendar.get(Calendar.MONTH) == monthSection.tag as Int && calendar.before(
                    endDate
                )
            ) {
                addDayView(calendar.clone() as Calendar, gridLayout, startDate)
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
    private fun addDayView(calendar: Calendar, gridLayout: GridLayout, startDate: Calendar) {
        val dayIndex =
            ((calendar.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
        val isTrainingDay =
            dayIndex >= 0 && (dayIndex % 7 == 0 || dayIndex % 7 == 2 || dayIndex % 7 == 4)

        val displayMetrics = gridLayout.context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels

        // Учитываем отступы
        val leftPadding = (24 * displayMetrics.density).toInt()
        val rightPadding = (22 * displayMetrics.density).toInt()
        val margin = (4 * displayMetrics.density).toInt()
        val totalMargins = margin * 6 + leftPadding + rightPadding

        // Вычисляем размер ячейки
        val cellSize = ((screenWidth - totalMargins) / 8).toInt()

        val layoutParams = GridLayout.LayoutParams().apply {
            width = cellSize
            height = cellSize
            setMargins(margin)
        }

        val sharedPrefs = gridLayout.context.getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)

        if (isTrainingDay) {
            val trainingIndex = (dayIndex / 7) * 3 + listOf(0, 2, 4).indexOf(dayIndex % 7)
            val isCompleted = sharedPrefs.getBoolean("WORKOUT_COMPLETED_$trainingIndex", false)
            val isNotCompleted =
                sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_$trainingIndex", false)

            val dayView = TextView(this).apply {
                text = "${calendar.get(Calendar.DAY_OF_MONTH)}"
                textSize = 14f
                gravity = Gravity.CENTER
                this.layoutParams = layoutParams

                background = GradientDrawable().apply {
                    setColor(
                        when {
                            isCompleted -> Color.parseColor("#90BE6D")
                            isNotCompleted -> Color.parseColor("#F94144")
                            else -> Color.parseColor("#4D908E")
                        }
                    )
                    cornerRadius = 10f * displayMetrics.density
                }

                tag = trainingIndex
            }

            dayView.setOnClickListener {
                val isNotCompleted =
                    sharedPrefs.getBoolean("WORKOUT_NOT_COMPLETED_${dayView.tag}", false)

                if (isNotCompleted) {
                    shiftWorkouts(dayView.tag as Int) // Обновляем тренировки и календарь
                } else {
                    val intent = Intent(this@MainActivity, WorkoutDetailActivity::class.java)
                    intent.putExtra("WORKOUT_INDEX", dayView.tag as Int)
                    intent.putExtra("WORKOUT_TEXT", workouts[dayView.tag as Int])
                    startActivityForResult(intent, WORKOUT_REQUEST_CODE)
                }

            }

            gridLayout.addView(dayView)
        } else {
            val dayView = TextView(this).apply {
                text = calendar.get(Calendar.DAY_OF_MONTH).toString()
                textSize = 14f
                gravity = Gravity.CENTER
                this.layoutParams = layoutParams

                background = GradientDrawable().apply {
                    setColor(Color.LTGRAY)
                    cornerRadius = 10f * displayMetrics.density
                }
            }

            gridLayout.addView(dayView)
        }
    }

    private fun shiftWorkouts(failedWorkoutIndex: Int) {
        val workoutsList = workouts.toMutableList()

        // Добавляем "День отдыха" после не выполненной тренировки
        workoutsList.add(failedWorkoutIndex + 1, "День отдыха")

        // Сдвигаем все тренировки вперед начиная с позиции +2
        for (i in workoutsList.size - 1 downTo failedWorkoutIndex + 2) {
            workoutsList[i] = workoutsList[i - 1]
        }

        // Повторяем текущую не выполненную тренировку на следующей позиции)))--
        workoutsList[failedWorkoutIndex + 2] = workoutsList[failedWorkoutIndex]

        // Обновляем массив тренировок
        workouts = workoutsList.toTypedArray()

        // Пересоздаем календарь
        generateCalendar(trainingStartDate)
    }
}
