package com.example.focusup.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object TaskStorage {

    private const val PREFS_NAME = "focusup_prefs"
    private const val KEY_TASKS = "tasks_json"

    fun getTasks(context: Context): MutableList<Task> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_TASKS, null) ?: return mutableListOf()

        val list = mutableListOf<Task>()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                Task(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    deadline = obj.getString("deadline"),
                    deadlineMillis = obj.optLong("deadlineMillis", 0L),
                    priority = obj.getString("priority"),
                    isDone = obj.getBoolean("isDone"),
                    completedAtMillis = obj.optLong("completedAtMillis", 0L),
                    note = obj.optString("note", "")
                )
            )
        }
        return list
    }

    fun saveTasks(context: Context, tasks: List<Task>) {
        val array = JSONArray()
        for (task in tasks) {
            val obj = JSONObject()
            obj.put("id", task.id)
            obj.put("title", task.title)
            obj.put("deadline", task.deadline)
            obj.put("deadlineMillis", task.deadlineMillis)
            obj.put("priority", task.priority)
            obj.put("isDone", task.isDone)
            obj.put("completedAtMillis", task.completedAtMillis)
            obj.put("note", task.note)
            array.put(obj)
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_TASKS, array.toString()).apply()
    }

    fun addTask(context: Context, task: Task) {
        val tasks = getTasks(context)
        tasks.add(0, task)
        saveTasks(context, tasks)
    }

    fun updateTask(context: Context, updated: Task) {
        val tasks = getTasks(context)
        val index = tasks.indexOfFirst { it.id == updated.id }
        if (index != -1) {
            val anterior = tasks[index]
            val tareaFinal = if (updated.isDone && !anterior.isDone) {
                updated.copy(completedAtMillis = System.currentTimeMillis())
            } else if (!updated.isDone) {
                updated.copy(completedAtMillis = 0L)
            } else {
                updated
            }
            tasks[index] = tareaFinal
            saveTasks(context, tasks)
        }
    }

    fun deleteTask(context: Context, task: Task) {
        val tasks = getTasks(context)
        tasks.removeAll { it.id == task.id }
        saveTasks(context, tasks)
    }

    fun getPendingCount(context: Context): Int =
        getTasks(context).count { !it.isDone }

    fun getCompletedCount(context: Context): Int =
        getTasks(context).count { it.isDone }

    fun getTasksForDay(context: Context, dayMillis: Long): List<Task> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = dayMillis
        val targetDay = cal.get(Calendar.DAY_OF_YEAR)
        val targetYear = cal.get(Calendar.YEAR)

        return getTasks(context).filter { task ->
            if (task.deadlineMillis == 0L) return@filter false
            val taskCal = Calendar.getInstance()
            taskCal.timeInMillis = task.deadlineMillis
            taskCal.get(Calendar.DAY_OF_YEAR) == targetDay &&
                taskCal.get(Calendar.YEAR) == targetYear
        }
    }

    private fun inicioSemanaActual(): Long {
        val cal = Calendar.getInstance()
        val diaSemana = cal.get(Calendar.DAY_OF_WEEK)
        val diasARetroceder = if (diaSemana == Calendar.SUNDAY) 6 else diaSemana - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -diasARetroceder)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun inicioMesActual(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getCompletedCountForPeriod(context: Context, period: PomodoroStorage.Period): Int {
        val completadas = getTasks(context).filter { it.isDone && it.completedAtMillis > 0 }
        return when (period) {
            PomodoroStorage.Period.WEEK -> completadas.count { it.completedAtMillis >= inicioSemanaActual() }
            PomodoroStorage.Period.MONTH -> completadas.count { it.completedAtMillis >= inicioMesActual() }
            PomodoroStorage.Period.ALL -> completadas.size
        }
    }
}
