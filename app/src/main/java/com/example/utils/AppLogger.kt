package com.example.utils

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    fun d(tag: String, message: String) {
        val time = dateFormat.format(Date())
        val entry = "[$time] [DEBUG] [$tag] $message"
        Log.d(tag, message)
        appendLog(entry)
    }

    fun i(tag: String, message: String) {
        val time = dateFormat.format(Date())
        val entry = "[$time] [INFO] [$tag] $message"
        Log.i(tag, message)
        appendLog(entry)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = dateFormat.format(Date())
        val errDetails = throwable?.let { "\n${it.stackTraceToString()}" } ?: ""
        val entry = "[$time] [ERROR] [$tag] $message$errDetails"
        Log.e(tag, message, throwable)
        appendLog(entry)
    }

    private fun appendLog(entry: String) {
        synchronized(_logs) {
            val current = _logs.value.toMutableList()
            if (current.size >= 1000) {
                current.removeAt(0)
            }
            current.add(entry)
            _logs.value = current
        }
    }

    fun clear() {
        synchronized(_logs) {
            _logs.value = emptyList()
        }
    }

    fun getAllLogsText(): String {
        return _logs.value.joinToString("\n")
    }
}
