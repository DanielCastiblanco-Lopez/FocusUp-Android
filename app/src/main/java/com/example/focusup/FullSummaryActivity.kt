package com.example.focusup

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.example.focusup.data.PomodoroStorage
import com.example.focusup.data.StreakStorage
import com.example.focusup.data.TaskStorage
import com.example.focusup.data.UserStorage

class FullSummaryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_summary)

        val toolbar = findViewById<Toolbar>(R.id.toolbarFullSummary)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        cargarResumenTotal()
    }

    private fun cargarResumenTotal() {
        findViewById<TextView>(R.id.tvStartDate).text =
            "Usando FocusUp desde el ${UserStorage.getFirstUseDateFormatted(this)}"

        findViewById<TextView>(R.id.tvTotalMinutesAllTime).text =
            PomodoroStorage.getTotalMinutes(this).toString()

        findViewById<TextView>(R.id.tvTotalSessionsAllTime).text =
            PomodoroStorage.getSessionCount(this).toString()

        findViewById<TextView>(R.id.tvDaysStudied).text =
            PomodoroStorage.getTotalDaysStudied(this).toString()

        findViewById<TextView>(R.id.tvMaxStreak).text =
            StreakStorage.getMaxStreak(this).toString()

        findViewById<TextView>(R.id.tvTotalTasksCompleted).text =
            TaskStorage.getCompletedCount(this).toString()

        findViewById<TextView>(R.id.tvAverageDaily).text =
            "${PomodoroStorage.getAverageMinutesPerDay(this)} min"

        findViewById<TextView>(R.id.tvBestWeek).text =
            "${PomodoroStorage.getBestWeekMinutes(this)} min"

        findViewById<TextView>(R.id.tvRecordDay).text =
            "${PomodoroStorage.getRecordMinutesInADay(this)} min"
    }
}
