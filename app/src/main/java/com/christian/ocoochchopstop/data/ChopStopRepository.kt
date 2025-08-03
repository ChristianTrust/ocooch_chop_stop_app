package com.christian.ocoochchopstop.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
//import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

//class ChopStopRepository(private val context: Context) {
//
//    companion object {
//        private val SPEED_KEY = intPreferencesKey("speed")
//        private val ACCEL_KEY = intPreferencesKey("accel")
//        private val MAX_DELAY_KEY = intPreferencesKey("max_delay")
//        private val MIN_DELAY_KEY = intPreferencesKey("min_delay")
//        private val EIGHT_FT_STOP_HEAD_KEY = doublePreferencesKey("8ft_stop_head")
//        private val TEN_FT_STOP_HEAD_KEY = doublePreferencesKey("10ft_stop_head")
//        private val TWELVE_FT_STOP_HEAD_KEY = doublePreferencesKey("12ft_stop_head")
//        private val STEPS_PER_INCH_KEY = doublePreferencesKey("steps_per_inch")
//        private val STEPS_PER_MM_KEY = doublePreferencesKey("steps_per_mm")
//        private val STEP_POSITION_KEY = intPreferencesKey("step_position")
//        private val MIN_STEP_POSITION = intPreferencesKey("min_step_position")
//        private val MAX_STEP_POSITION = intPreferencesKey("max_step_position")
//        private val STOP_HEAD_KEY = stringPreferencesKey("stop_head")
//    }
//
//    val speedFlow = context.dataStore.data.map { it[SPEED_KEY] ?: 20000 }
//    val accelFlow = context.dataStore.data.map { it[ACCEL_KEY] ?: 8000 }
//    val maxDelayFlow = context.dataStore.data.map { it[MAX_DELAY_KEY] ?: 320 }
//    val minDelayFlow = context.dataStore.data.map { it[MIN_DELAY_KEY] ?: 6 }
//    val eightFtStopHeadFlow = context.dataStore.data.map { it[EIGHT_FT_STOP_HEAD_KEY] ?: 2.6 }
//    val tenFtStopHeadFlow = context.dataStore.data.map { it[TEN_FT_STOP_HEAD_KEY] ?: 26.6 }
//    val twelveFtStopHeadFlow = context.dataStore.data.map { it[TWELVE_FT_STOP_HEAD_KEY] ?: 50.6 }
//    val stepsPerInchFlow = context.dataStore.data.map { it[STEPS_PER_INCH_KEY] ?: 1777.77777778 }
//    val stepsPerMmFlow = context.dataStore.data.map { it[STEPS_PER_MM_KEY] ?: 69.9912510935 }
//    val stepPositionFlow = context.dataStore.data.map { it[STEP_POSITION_KEY] ?: 0 }
//    val minStepPositionFlow = context.dataStore.data.map { it[MIN_STEP_POSITION] ?: 0 }
//    val maxStepPositionFlow = context.dataStore.data.map { it[MAX_STEP_POSITION] ?: 170666 }
//    val stopHeadFlow = context.dataStore.data.map { it[STOP_HEAD_KEY] ?: "8ft" }
//
//    suspend fun setSpeed(speed: Int) = context.dataStore.edit { it[SPEED_KEY] = speed }
//    suspend fun setAccel(accel: Int) = context.dataStore.edit { it[ACCEL_KEY] = accel }
//    suspend fun setMaxDelay(maxDelay: Int) = context.dataStore.edit { it[MAX_DELAY_KEY] = maxDelay }
//    suspend fun setMinDelay(minDelay: Int) = context.dataStore.edit { it[MIN_DELAY_KEY] = minDelay }
//    suspend fun setEightFtStopHead(value: Double) = context.dataStore.edit { it[EIGHT_FT_STOP_HEAD_KEY] = value }
//    suspend fun setTenFtStopHead(value: Double) = context.dataStore.edit { it[TEN_FT_STOP_HEAD_KEY] = value }
//    suspend fun setTwelveFtStopHead(value: Double) = context.dataStore.edit { it[TWELVE_FT_STOP_HEAD_KEY] = value }
//    suspend fun setStepsPerInch(value: Double) = context.dataStore.edit { it[STEPS_PER_INCH_KEY] = value }
//    suspend fun setStepsPerMm(value: Double) = context.dataStore.edit { it[STEPS_PER_MM_KEY] = value }
//    suspend fun setStepPosition(value: Int) = context.dataStore.edit { it[STEP_POSITION_KEY] = value }
//    suspend fun setMinStepPosition(value: Int) = context.dataStore.edit { it[MIN_STEP_POSITION] = value }
//    suspend fun setMaxStepPosition(value: Int) = context.dataStore.edit { it[MAX_STEP_POSITION] = value }
//    suspend fun setStopHead(value: String) = context.dataStore.edit { it[STOP_HEAD_KEY] = value }
//}