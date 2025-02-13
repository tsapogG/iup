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
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import java.util.Calendar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.setMargins
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.iup.utils.Day
import kotlin.math.abs


class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var calendarContainer: LinearLayout
    var trainingDaysList = mutableListOf<Day>()
    var benchPress = 0
    var squat = 0
    var deadlift = 0
    var workoutCounter = 0
    var ddas = 0
    var opa = 0 // Переменная для корректировки индексов тренировок
    var fdays = 0
    private var trainingStartDate: Calendar = Calendar.getInstance()
    private var experienceLevel: Int = 0
    private var loadPercentage: Int = 0


    // Инициализируем workouts значением по умолчанию
    private var workouts: Array<String> = arrayOf("No workouts available")

    @SuppressLint("DefaultLocale", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        // Инициализация SharedPreferences и других переменных
        sharedPreferences = getSharedPreferences("TrainingPrefs", MODE_PRIVATE)
        calendarContainer = findViewById(R.id.calendarContainer)


        loadSavedData()

        if (benchPress == 0) {
            // Если это первый запуск, показываем диалог настройки плана тренировок
            showPlanDialog(isFirstSetup = true)
            generateWorkouts()
        } else {
            // Если это не первый запуск, загружаем сохраненные данные и генерируем тренировки
            loadSavedData()
            generateWorkouts()
            Log.e("MainActivity", "список $trainingDaysList")
            generateCalendar(trainingStartDate)
        }



        // Рассчитываем новые значения нагрузки
        val loadFactor2 = 1 + (loadPercentage / 100.0)
        val benchPressNew = String.format("%.2f", benchPress * loadFactor2)
        val squatNew = String.format("%.2f", squat * loadFactor2)
        val deadliftPressNew = String.format("%.2f", deadlift * loadFactor2)

        // Находим TextView в разметке и устанавливаем текст
        val benchPressText = findViewById<TextView>(R.id.benchPressText)
        val squatText = findViewById<TextView>(R.id.squatText)
        val deadliftText = findViewById<TextView>(R.id.deadliftText)

        benchPressText.text = "Новый максимальный жим: $benchPressNew"
        squatText.text = "Новый максимальный присед: $squatNew"
        deadliftText.text = "Новый максимальный становая тяга: $deadliftPressNew"

        // Настройка кнопки "Настройки"
        val settingsButton = findViewById<Button>(R.id.settingsButton)
        settingsButton.setOnClickListener {
            openSettingsDialog()
        }


        // Настройка кнопки "Сброс"
        val resetButton = findViewById<Button>(R.id.resetButton)
        resetButton.setOnClickListener {
            showResetConfirmationDialog() // Показываем диалог подтверждения сброса
        }
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
        val hah = dialogView.findViewById<ImageButton>(R.id.experienceLevelInfoButton)

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
        hah.setOnClickListener(){
            haha()}


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
                saveData()

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
                val benchPressDifference = abs(benchPress - (benchPress * loadFactor2).toInt())
                val squatDifference = abs(squat - (squat * loadFactor2).toInt())
                val deadliftDifference = abs(deadlift - (deadlift * loadFactor2).toInt())

// Проверяем разницу для каждого упражнения
                if (benchPressDifference <= 5 || squatDifference <= 5 || deadliftDifference <= 5) {
                    haha2()
                }


                // Находим TextView в разметке
                val benchPressText = findViewById<TextView>(R.id.benchPressText)
                val squatText = findViewById<TextView>(R.id.squatText)
                val deadliftText = findViewById<TextView>(R.id.deadliftText)

                // Устанавливаем текст в TextView
                benchPressText.text = "Новый максимальный жим: $benchPressNew"
                squatText.text = "Новый максимальный присед: $squatNew"
                deadliftText.text = "Новый максимальный становая тяга: $deadliftPressNew"
                generateWorkouts()
                generateCalendar(trainingStartDate)
            }
            .setNegativeButton("Отмена") { _, _ ->
                if (isFirstSetup) finish()
            }
            .show()

    }
    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun showPlanDialog2() {
        val dialogView = layoutInflater.inflate(R.layout.changerur, null)

        val experienceLevelSpinner = dialogView.findViewById<Spinner>(R.id.experienceLevelSpinner)
        val loadPercentageSeekBar = dialogView.findViewById<SeekBar>(R.id.loadPercentageSeekBar)
        val loadPercentageTextView = dialogView.findViewById<TextView>(R.id.loadPercentageTextView)

        // Настройка Spinner
        val levels = resources.getStringArray(R.array.experience_levels)
        val levelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levels)
        experienceLevelSpinner.adapter = levelAdapter
        experienceLevelSpinner.setSelection(experienceLevel) // 0 - Новичок, 1 - Любитель, 2 - Профессионал

        // Установка максимального значения SeekBar в зависимости от уровня
        when (experienceLevel) {
            0 -> loadPercentageSeekBar.max = 15 // Новичок
            1 -> loadPercentageSeekBar.max = 7  // Любитель
            2 -> loadPercentageSeekBar.max = 5  // Профессионал
        }

        // Обновление текста при изменении SeekBar
        loadPercentageSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                loadPercentageTextView.text = "Процент нагрузки: $progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Обработчик выбора уровня
        experienceLevelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                when (position) {
                    0 -> loadPercentageSeekBar.max = 15 // Новичок
                    1 -> loadPercentageSeekBar.max = 7  // Любитель
                    2 -> loadPercentageSeekBar.max = 5  // Профессионал
                }
                loadPercentageTextView.text = "Процент нагрузки: ${loadPercentageSeekBar.progress}%"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Создание диалогового окна
        AlertDialog.Builder(this)
            .setTitle("Рекомендация")
            .setMessage("Похоже, это слишком большая нагрузка для вас. Рекомендую снизить её в настройках, и тогда тренировки станут эффективнее.")
            .setView(dialogView) // Добавляем нашу разметку
            .setPositiveButton("Сохранить") { _, _ ->
                experienceLevel = experienceLevelSpinner.selectedItemPosition
                loadPercentage = loadPercentageSeekBar.progress
                saveData()
                generateWorkouts()
                generateCalendar(trainingStartDate)
                val loadFactor2 = 1 + (loadPercentage / 100.0)
                val benchPressNew = String.format("%.2f", benchPress * loadFactor2)
                val squatNew = String.format("%.2f", squat * loadFactor2)
                val deadliftPressNew = String.format("%.2f", deadlift * loadFactor2)
                // Находим TextView в разметке
                val benchPressText = findViewById<TextView>(R.id.benchPressText)
                val squatText = findViewById<TextView>(R.id.squatText)
                val deadliftText = findViewById<TextView>(R.id.deadliftText)

                // Устанавливаем текст в TextView
                benchPressText.text = "Новый максимальный жим: $benchPressNew"
                squatText.text = "Новый максимальный присед: $squatNew"
                deadliftText.text = "Новый максимальный становая тяга: $deadliftPressNew"

            }
            .setNegativeButton("Отмена") { _, _ -> }
            .setCancelable(false) // Диалог нельзя закрыть, нажав вне области
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
            putInt("fdays", fdays)
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

        // Пересоздаем список тренировочных дней при изменении даты
        generateCalendar(trainingStartDate)
    }


    private fun loadSavedData() {
        benchPress = sharedPreferences.getInt("benchPress", 0)
        squat = sharedPreferences.getInt("squat", 0)
        deadlift = sharedPreferences.getInt("deadlift", 0)
        loadPercentage = sharedPreferences.getInt("loadPercentage", 0)
        experienceLevel = sharedPreferences.getInt("experienceLevel", 0)
        fdays = sharedPreferences.getInt("fdays", 0)
        trainingDaysList = loadTrainingDaysFromSharedPreferences()
        opa = sharedPreferences.getInt("opa",0)
        val startDateMillis = sharedPreferences.getLong("startDate", 0)
        if (startDateMillis != 0L) {
            trainingStartDate.timeInMillis = startDateMillis
        }
        workoutCounter = sharedPreferences.getInt("workoutCounter", 0) // Загружаем значение workoutCounter
        Log.d(
            "LoadData",
            "Data loaded: benchPress=$benchPress, squat=$squat, deadlift=$deadlift, startDate=${trainingStartDate.time}, workoutCounter=$workoutCounter"
        )
    }

    private fun showSetupDialog() {
        showPlanDialog(isFirstSetup = false)
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
        Log.e("МегаМозг", "жим $benchPress присед $squat тяга $deadlift")

        // Создаем тренировочный план
        workouts = arrayOf(
            "Тренировка 1: \n" +
                    "Присед со штангой на плечах: \n" +
                    "1. 4 подхода по 3 повторения x ${String.format("%.2f", squat * 0.7)} kg \n" +
                    "2. 3 подхода по 2 повторения x ${String.format("%.2f", squat * 0.75)} kg \n" +
                    "3. 2 x ${String.format("%.2f", squat * 0.8)} kg \n" +
                    "4. 1 x ${String.format("%.2f", squat * 0.85)} kg \n" +
                    "Жим лежа: \n" +
                    "1. 5 x ${String.format("%.2f", benchPressNew * 0.6)} kg \n" +
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

        // Очищаем состояние тренировок
        val workoutPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        val workoutEditor = workoutPrefs.edit()
        workoutEditor.clear()
        workoutEditor.apply()

        // Сбрасываем переменные
        opa = 0
        fdays = 0
        workoutCounter = 0
        trainingDaysList.clear() // Очищаем текущий список тренировочных дней
        benchPress = 0
        squat = 0
        deadlift = 0

        // Очищаем сохраненные тренировочные дни
        val allKeys = sharedPreferences.all.keys
        for (key in allKeys) {
            if (key.startsWith("TRAINING_DAY_")) {
                editor.remove(key)
            }
        }
        editor.apply()

        // Восстанавливаем начальный массив тренировок
        generateWorkouts()


        // Перегенерируем календарь
        trainingStartDate = Calendar.getInstance() // Устанавливаем текущую дату как новую стартовую
        saveData() // Сохраняем сброшенные данные
        generateCalendar(trainingStartDate)


        showPlanDialog(true)
    }

    private val WORKOUT_REQUEST_CODE = 100
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WORKOUT_REQUEST_CODE && resultCode == RESULT_OK) {
            val dayIndex = data?.getIntExtra("DAY_INDEX", -1) ?: -1

            // Проверяем корректность индекса тренировки


            // Загружаем список тренировочных дней
            val trainingDaysList = loadTrainingDaysFromSharedPreferences()



            // Получаем SharedPreferences
            val sharedPrefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)


            if (trainingDaysList[dayIndex].notCompleted) {
                // Если тренировка не выполнена, сдвигаем тренировки
                shiftWorkouts(dayIndex)
            } else if (trainingDaysList[dayIndex].completed) {
                // Если тренировка выполнена, выполняем соответствующие действия
                handleCompletedWorkout(dayIndex)
            }

            // Перегенерируем календарь для отображения изменений
            generateCalendar(trainingStartDate)
        }
    }
    private fun generateTrainingDaysList(startDate: Calendar): List<Day> {
        val trainingDaysList = mutableListOf<Day>()
        val calendar = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Начинаем с понедельника
        }
        val endDate = (startDate.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, 16) } // 16 недель
        var workoutCounter = 0 // Счетчик для индексов тренировок

        while (calendar.before(endDate)) {
            val dayIndex = ((calendar.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()

            if (dayIndex >= 0 && (dayIndex % 7 == 0 || dayIndex % 7 == 2 || dayIndex % 7 == 4)) {
                // Это тренировочный день
                val workoutIndex = workoutCounter.coerceIn(0, workouts.size - 1)

                // Определяем количество выходных дней после тренировки
                val restDaysAfter = when (dayIndex % 7) {
                    0 -> 0 // После понедельника — 1 выходной день
                    2 -> 2 // После среды — 1 выходной день
                    4 -> 4 // После пятницы — 2 выходных дня
                    else -> 0 // Для других дней (на всякий случай)
                }

                val day = Day(
                    date = calendar.clone() as Calendar,
                    isTrainingDay = true,
                    workoutIndex = workoutIndex,
                    completed = false,
                    notCompleted = false,
                    isRestDay = false,
                    workoutDescription = workouts.getOrNull(workoutIndex),
                    restDaysAfter = restDaysAfter // Устанавливаем количество выходных дней
                )

                trainingDaysList.add(day)
                workoutCounter++
            }

            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d("GenerateTrainingDays", "Generated ${trainingDaysList.size} training days")
        return trainingDaysList
    }




    // Generate Calendar

    fun generateCalendar(startDate: Calendar) {
        val calendar = (startDate.clone() as Calendar).apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY) // Начинаем с понедельника
        }
        val weekss = 16 + opa
        val endDate = (startDate.clone() as Calendar).apply { add(Calendar.WEEK_OF_YEAR, weekss) } // 16 недель
        calendarContainer.removeAllViews()

        // Проверяем, изменилась ли дата старта тренировок
        val savedStartDateMillis = sharedPreferences.getLong("startDate", 0)
        val isStartDateChanged = savedStartDateMillis != startDate.timeInMillis

        // Загружаем сохраненный список тренировочных дней
        var trainingDaysList: List<Day>
        if (!isStartDateChanged && loadTrainingDaysFromSharedPreferences().isNotEmpty()) {
            // Если дата не изменилась и список существует — используем его
            trainingDaysList = loadTrainingDaysFromSharedPreferences()
        } else {
            // Иначе генерируем новый список тренировочных дней
            generateWorkouts()
            trainingDaysList = generateTrainingDaysList(startDate)
            saveTrainingDaysToSharedPreferences(trainingDaysList) // Сохраняем новый список
        }

        // Создаем UI для календаря
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

        // Реальный индекс дня в списке trainingDaysList
        var dayIndex = 0

        while (calendar.before(endDate) && dayIndex < trainingDaysList.size) {
            val monthSection = createMonthSection(calendar) // Создаем секцию для каждого месяца
            monthContainer.addView(monthSection)
            val gridLayout = createDayGridLayout() // Создаем сетку для дней месяца
            val firstDayOfMonth = calendar.get(Calendar.DAY_OF_WEEK)
            val emptyCells = (firstDayOfMonth - Calendar.MONDAY + 7) % 7
            repeat(emptyCells) {
                addEmptyDay(gridLayout) // Добавляем пустые ячейки перед началом месяца
            }

            while (calendar.get(Calendar.MONTH) == monthSection.tag as Int && calendar.before(endDate) && dayIndex < trainingDaysList.size) {
                val dayOffset = ((calendar.timeInMillis - startDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                val isTrainingDayByIndex = dayOffset >= 0 && (dayOffset % 7 == 0 || dayOffset % 7 == 2 || dayOffset % 7 == 4)

                if (isTrainingDayByIndex) {
                    if (dayIndex < trainingDaysList.size) {
                        val trainingDay = trainingDaysList[dayIndex]
                        addDayView(trainingDay, gridLayout, dayIndex)
                        dayIndex++
                    } else {
                        break // Прерываем цикл, если все тренировочные дни обработаны
                    }
                } else {
                    addEmptyDayForNonTrainingDays(calendar.clone() as Calendar, gridLayout)
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
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
            tag = monthIndex // Устанавливаем метку месяца
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
    private fun addEmptyDayForNonTrainingDays(date: Calendar, gridLayout: GridLayout) {
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

        val dayView = TextView(this).apply {
            text = date.get(Calendar.DAY_OF_MONTH).toString() // Отображаем номер дня
            textSize = 14f
            gravity = Gravity.CENTER
            this.layoutParams = layoutParams
            background = GradientDrawable().apply {
                setColor(Color.LTGRAY) // Серый цвет для пустых дней
                cornerRadius = 10f * resources.displayMetrics.density
            }
        }

        dayView.setOnClickListener {
            // Открываем экран "День отдыха" для пустых дней
            val intent = Intent(this@MainActivity, RestDayActivity::class.java)
            startActivity(intent)
        }

        gridLayout.addView(dayView)
    }

    // Add day view to the grid layout
    private fun addDayView(day: Day, gridLayout: GridLayout, dayIndex: Int) {
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

        // Определяем цвет фона на основе состояния дня
        val backgroundColor = when {
            day.completed -> Color.parseColor("#90BE6D") // Зеленый для выполненных тренировок
            day.notCompleted -> Color.parseColor("#F94144") // Красный для не выполненных тренировок
            day.isRestDay -> Color.LTGRAY // Серый для дней отдыха
            else -> Color.parseColor("#4D908E") // Синий для обычных тренировочных дней
        }

        // Создаем TextView для дня
        val dayView = TextView(this).apply {
            text = "${day.date.get(Calendar.DAY_OF_MONTH)}"
            textSize = 14f
            gravity = Gravity.CENTER
            this.layoutParams = layoutParams
            background = GradientDrawable().apply {
                setColor(backgroundColor)
                cornerRadius = 10f * resources.displayMetrics.density
            }
            tag = dayIndex // Устанавливаем тег как реальный индекс дня
        }

        // Устанавливаем обработчик кликов
        dayView.setOnClickListener {
            when {
                day.completed -> {
                    // Если тренировка выполнена, показываем информацию о выполненном дне
                    showCompletedWorkoutInfo(day)
                }
                day.notCompleted || day.isRestDay -> {
                    // Если тренировка не выполнена или это день отдыха, открываем экран отдыха
                    val intent = Intent(this@MainActivity, RestDayActivity::class.java)
                    startActivity(intent)
                }
                else -> {
                    // Если тренировка еще не трогалась, открываем детали тренировки
                    if (day.workoutDescription != null) {
                        val intent = Intent(this@MainActivity, WorkoutDetailActivity::class.java)
                        intent.putExtra("WORKOUT_TEXT", workouts[day.workoutIndex])
                        intent.putExtra("DAY_INDEX", it.tag as Int) // Передаем реальный индекс дня
                        startActivityForResult(intent, WORKOUT_REQUEST_CODE)
                    }
                }
            }
        }

        // Добавляем ячейку в GridLayout
        gridLayout.addView(dayView)
    }
    private fun addEmptyDay(gridLayout: GridLayout) {
        val emptyView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED)
            }
            background = GradientDrawable().apply {
                setColor(Color.TRANSPARENT) // Прозрачный фон для пустых ячеек
                cornerRadius = 10f * resources.displayMetrics.density
            }
        }

        gridLayout.addView(emptyView)
    }

    private fun saveTrainingDaysToSharedPreferences(trainingDaysList: List<Day>) {
        val editor = sharedPreferences.edit()
        for ((index, day) in trainingDaysList.withIndex()) {
            editor.putBoolean("TRAINING_DAY_${index}_IS_TRAINING_DAY", day.isTrainingDay)
            editor.putLong("TRAINING_DAY_${index}_DATE", day.date.timeInMillis)
            editor.putInt("TRAINING_DAY_${index}_WORKOUT_INDEX", day.workoutIndex)
            editor.putBoolean("TRAINING_DAY_${index}_COMPLETED", day.completed)
            editor.putBoolean("TRAINING_DAY_${index}_NOT_COMPLETED", day.notCompleted)
            editor.putString("TRAINING_DAY_${index}_WORKOUT_DESCRIPTION", day.workoutDescription)
            editor.putBoolean("TRAINING_DAY_${index}_REST_DAY", day.isRestDay)
            editor.putInt("TRAINING_DAY_${index}_REST_DAYS_AFTER", day.restDaysAfter) // Сохраняем restDaysAfter
        }
        editor.apply()
    }

    private fun loadTrainingDaysFromSharedPreferences(): MutableList<Day> {
        val trainingDaysList = mutableListOf<Day>()
        val sharedPreferences = getSharedPreferences("TrainingPrefs", MODE_PRIVATE)
        var index = 0

        while (sharedPreferences.contains("TRAINING_DAY_${index}_DATE")) {
            val dateMillis = sharedPreferences.getLong("TRAINING_DAY_${index}_DATE", 0)
            val date = Calendar.getInstance().apply { timeInMillis = dateMillis }
            val workoutIndex = sharedPreferences.getInt("TRAINING_DAY_${index}_WORKOUT_INDEX", -1)
            val completed = sharedPreferences.getBoolean("TRAINING_DAY_${index}_COMPLETED", false)
            val notCompleted = sharedPreferences.getBoolean("TRAINING_DAY_${index}_NOT_COMPLETED", false)
            val workoutDescription = sharedPreferences.getString("TRAINING_DAY_${index}_WORKOUT_DESCRIPTION", null)
            val isRestDay = sharedPreferences.getBoolean("TRAINING_DAY_${index}_REST_DAY", false)
            val isTrainingDay = sharedPreferences.getBoolean("TRAINING_DAY_${index}_IS_TRAINING_DAY", true)
            val restDaysAfter = sharedPreferences.getInt("TRAINING_DAY_${index}_REST_DAYS_AFTER", 0) // Загружаем restDaysAfter

            trainingDaysList.add(
                Day(
                    date = date,
                    isTrainingDay = isTrainingDay,
                    workoutIndex = workoutIndex,
                    completed = completed,
                    notCompleted = notCompleted,
                    isRestDay = isRestDay,
                    workoutDescription = workoutDescription,
                    restDaysAfter = restDaysAfter // Инициализируем новое поле
                )
            )

            index++
        }

        return trainingDaysList
    }

    private fun handleCompletedWorkout(dayIndex: Int) {
        fdays = 0

        // Загружаем список тренировочных дней
        val trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()

        // Находим реальный индекс дня в списке по workoutIndex
        val dayIndex = dayIndex

        Log.e("mainS333", "LISTIND NOW = $dayIndex")
        Log.e("mainS333", "LISTIND  = ${trainingDaysList[dayIndex]}")

        // Отмечаем день как выполненый и устанавливаем workoutIndex = -100
        trainingDaysList[dayIndex].completed = true
        trainingDaysList[dayIndex].workoutDescription = workouts[trainingDaysList[dayIndex].workoutIndex]
        trainingDaysList[dayIndex].notCompleted = false
        trainingDaysList[dayIndex].workoutIndex = -100 // День больше не является тренировочным

        Log.d("m", "mmcompl ${trainingDaysList[dayIndex]}")

        // Сохраняем изменения
        saveData()
        saveTrainingDaysToSharedPreferences(trainingDaysList)

        // Перегенерируем календарь
        generateCalendar(trainingStartDate)
    }


    private fun shiftWorkouts(failedDayIndex: Int) {
        var trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()
        fdays += 1

        when (fdays) {
            1 -> {
                opa += 1 // Обновляем opa
                ddas += 2 // Обновляем ddas
                generate22(2)
                trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()
                Log.d("ПИЗДАААААААууу","OGOOOOOO11 $trainingDaysList")

                // Преобразуем следующий день в день отдыха
                if (failedDayIndex + 1 < trainingDaysList.size) {
                    trainingDaysList[failedDayIndex + 1].isRestDay = true
                    trainingDaysList[failedDayIndex + 1].isTrainingDay = false
                    trainingDaysList[failedDayIndex + 1].workoutDescription = null
                    trainingDaysList[failedDayIndex + 1].workoutIndex = -100 // День больше не является тренировочным
                }
                saveTrainingDaysToSharedPreferences(trainingDaysList)

                Log.d("ПИЗДАААААААууу","OGOOOOOO11 $trainingDaysList")

                // Сдвигаем индексы для последующих тренировок, включая новые дни
                for (i in failedDayIndex + 2 until trainingDaysList.size) {
                    if (trainingDaysList[i].isTrainingDay && trainingDaysList[i].workoutIndex != -100) {
                        trainingDaysList[i].workoutIndex -= 2
                        trainingDaysList[i].workoutDescription = workouts.getOrNull(trainingDaysList[i].workoutIndex)
                    }
                }
                saveTrainingDaysToSharedPreferences(trainingDaysList)
                Log.d("ПИЗДАААААААууу","OGOOOOOO $trainingDaysList")


            }

            2 -> {
                opa += 2 // Обновляем opa
                ddas += 3 // Обновляем ddas
                generate22(3)
                trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()
                // Преобразуем текущий день и два следующих в дни отдыха
                for (i in failedDayIndex until failedDayIndex + 3) {
                    if (i < trainingDaysList.size) {
                        trainingDaysList[i].isRestDay = true
                        trainingDaysList[i].isTrainingDay = false
                        trainingDaysList[i].workoutDescription = null
                        trainingDaysList[i].workoutIndex = -100 // День больше не является тренировочным
                    }
                }

                saveTrainingDaysToSharedPreferences(trainingDaysList)



                // Сдвигаем индексы для последующих тренировок, включая новые дни
                for (i in failedDayIndex + 3 until trainingDaysList.size) {
                    if (trainingDaysList[i].isTrainingDay && trainingDaysList[i].workoutIndex != -100) {
                        trainingDaysList[i].workoutIndex -= 3 // Сдвигаем на 3, так как пропускается 3 дня
                        trainingDaysList[i].workoutDescription = workouts.getOrNull(trainingDaysList[i].workoutIndex)
                    }
                }

            }

            3 -> {
                opa += 1 // Обновляем opa
                ddas += 2 // Обновляем ddas
                generate22(2)
                trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()

                // Преобразуем следующий день в день отдыха
                if (failedDayIndex + 1 < trainingDaysList.size) {
                    trainingDaysList[failedDayIndex + 1].isRestDay = true
                    trainingDaysList[failedDayIndex + 1].isTrainingDay = false
                    trainingDaysList[failedDayIndex + 1].workoutDescription = null
                    trainingDaysList[failedDayIndex + 1].workoutIndex = -100 // День больше не является тренировочным
                }

                saveTrainingDaysToSharedPreferences(trainingDaysList)



                // Сдвигаем индексы для последующих тренировок, включая новые дни
                for (i in failedDayIndex + 2 until trainingDaysList.size) {
                    if (trainingDaysList[i].isTrainingDay && trainingDaysList[i].workoutIndex != -100) {
                        trainingDaysList[i].workoutIndex -= 2
                        trainingDaysList[i].workoutDescription = workouts.getOrNull(trainingDaysList[i].workoutIndex)
                    }
                }

                // Показываем диалог с советом
                showLoadReductionAdvice()
                fdays = 0

            }
        }

        // Сохраняем изменения
        saveData()
        saveTrainingDaysToSharedPreferences(trainingDaysList)

        // Перегенерируем календарь
        generateCalendar(trainingStartDate)
    }
    private fun showCompletedWorkoutInfo(day: Day) {
        val workoutDescription = day.workoutDescription ?: "Тренировка без описания"
        AlertDialog.Builder(this)
            .setTitle("Выполненная тренировка")
            .setMessage("Тренировка: $workoutDescription\nДата: ${formatDate(day.date)}")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun formatDate(calendar: Calendar): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
    }

    private fun showRestDayMessage() {
        Toast.makeText(this, "Это день отдыха!", Toast.LENGTH_SHORT).show()
    }

    private fun showLoadReductionAdvice() {
        AlertDialog.Builder(this)
            .setTitle("Рекомендация")
            .setMessage("Похоже, это слишком большая нагрузка для вас. Рекомендую снизить её в настройках, и тогда тренировки станут эффективнее.")
            .setPositiveButton("Сменить") { _, _ ->
                showPlanDialog2()
            }.setNegativeButton("Отмена"){ _, _ ->

            }
            .setCancelable(false) // Диалог нельзя закрыть, нажав вне области
            .show()
    }
    private fun generate22(count: Int){
        val trainingDaysList = loadTrainingDaysFromSharedPreferences().toMutableList()
        for (i in 0 until count){
            if(trainingDaysList[trainingDaysList.size - 1].restDaysAfter==0){
                var lastDate = trainingDaysList[trainingDaysList.size - 1].date
                val newStartDate = (lastDate.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, 2) // Добавляем 2 дня после последнего дня
                }
                val day = Day(
                    date = newStartDate,
                    isTrainingDay = true,
                    workoutIndex = workouts.size + i  ,
                    completed = false,
                    notCompleted = false,
                    isRestDay = false,
                    workoutDescription = null,
                    restDaysAfter = 2 // Устанавливаем количество выходных дней
                )

                trainingDaysList.add(day)
            }
            else if(trainingDaysList[trainingDaysList.size - 1].restDaysAfter==2){
                var lastDate = trainingDaysList[trainingDaysList.size - 1].date
                val newStartDate = (lastDate.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, 2) // Добавляем 2 дня после последнего дня
                }
                val day = Day(
                    date = newStartDate,
                    isTrainingDay = true,
                    workoutIndex = workouts.size + i  ,
                    completed = false,
                    notCompleted = false,
                    isRestDay = false,
                    workoutDescription = null,
                    restDaysAfter = 4 // Устанавливаем количество выходных дней
                )

                trainingDaysList.add(day)
            }
            else if(trainingDaysList[trainingDaysList.size - 1].restDaysAfter==4){
                var lastDate = trainingDaysList[trainingDaysList.size - 1].date
                val newStartDate = (lastDate.clone() as Calendar).apply {
                    add(Calendar.DAY_OF_MONTH, 3) // Добавляем 2 дня после последнего дня
                }
                val day = Day(
                    date = newStartDate,
                    isTrainingDay = true,
                    workoutIndex = workouts.size + i  ,
                    completed = false,
                    notCompleted = false,
                    isRestDay = false,
                    workoutDescription = null,
                    restDaysAfter = 0 // Устанавливаем количество выходных дней
                )

                trainingDaysList.add(day)
            }
        }
        saveTrainingDaysToSharedPreferences(trainingDaysList)
        Log.d("GenerateTrainingDays", "Generated $trainingDaysList training days")

    }
    private fun haha() {
        AlertDialog.Builder(this)
            .setTitle("Справка")
            .setMessage("Новичок\n" +
                    "Занимаюсь менее года.\n" +
                    "Тренируюсь 2–3 раза в неделю.\n" +
                    "Основная цель — улучшить здоровье или внешний вид.\n" +
                    "Часто ошибаюсь в технике, быстро устаю.\n" +
                    "Любитель\n" +
                    "Занимаюсь от 1 до 5 лет.\n" +
                    "Тренируюсь 3–5 раз в неделю.\n" +
                    "Имею базовые знания и правильную технику.\n" +
                    "Ставлю конкретные цели (например, пробежать марафон или увеличить силу).\n" +
                    "Профессионал\n" +
                    "Занимаюсь более 5 лет.\n" +
                    "Тренируюсь почти ежедневно по строгому плану.\n" +
                    "Достиг высоких результатов (участие в соревнованиях, титулы).\n" +
                    "Спорт — основная часть жизни, возможно, профессия.")
            .setPositiveButton("OK") { _, _ ->
                // Можно добавить логику перехода в настройки, если нужно
            }
            .setCancelable(false) // Диалог нельзя закрыть, нажав вне области
            .show()
    }
    private fun haha2() {
        AlertDialog.Builder(this)
            .setTitle("Рекомендация")
            .setMessage("Разница рабочих весов между подходами может быть слишком маленькой. Рекомендуем Вам докупить уменьшенные блины, чтобы более точно регулировать вес")
            .setPositiveButton("OK") { _, _ ->

            }



            .setCancelable(false) // Диалог нельзя закрыть, нажав вне области
            .show()
    }


}
