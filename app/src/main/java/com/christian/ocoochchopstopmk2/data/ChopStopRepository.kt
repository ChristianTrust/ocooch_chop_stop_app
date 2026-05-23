package com.christian.ocoochchopstopmk2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ChopStopRepository(private val context: Context) {

    companion object {
        private val SPEED_KEY = intPreferencesKey("speed")
        private val ACCEL_KEY = intPreferencesKey("accel")
        private val MAX_DELAY_KEY = intPreferencesKey("max_delay")
        private val MIN_DELAY_KEY = intPreferencesKey("min_delay")

        private val DIRECTION_KEY = stringPreferencesKey("direction")
        private val STEP_POSITION_KEY = intPreferencesKey("step_position")
        private val MIN_STEP_POSITION_KEY = intPreferencesKey("min_step_position")
        private val MAX_STEP_POSITION_KEY = intPreferencesKey("max_step_position")

        private val EIGHT_FT_STOP_HEAD_KEY = doublePreferencesKey("8ft_stop_head")
        private val TEN_FT_STOP_HEAD_KEY = doublePreferencesKey("10ft_stop_head")
        private val SIX_FT_STOP_HEAD_KEY = doublePreferencesKey("6ft_stop_head")

        private val STEPS_PER_INCH_KEY = doublePreferencesKey("steps_per_inch")

        private val STOP_HEAD_KEY = stringPreferencesKey("stop_head")
        private val TABLE_LENGTH_KEY = stringPreferencesKey("table_length")
    }

    // Settings Flows
    val speedFlow: Flow<Int> = context.dataStore.data.map { it[SPEED_KEY] ?: 20000 }
    val accelFlow: Flow<Int> = context.dataStore.data.map { it[ACCEL_KEY] ?: 8000 }
    val maxDelayFlow: Flow<Int> = context.dataStore.data.map { it[MAX_DELAY_KEY] ?: 320 }
    val minDelayFlow: Flow<Int> = context.dataStore.data.map { it[MIN_DELAY_KEY] ?: 6 }

    val directionFlow: Flow<String> = context.dataStore.data.map { it[DIRECTION_KEY] ?: "RIGHT" }
    val stepPositionFlow: Flow<Int> = context.dataStore.data.map { it[STEP_POSITION_KEY] ?: 0 }
    val minStepPositionFlow: Flow<Int> = context.dataStore.data.map { it[MIN_STEP_POSITION_KEY] ?: 0 }
    val maxStepPositionFlow: Flow<Int> = context.dataStore.data.map { it[MAX_STEP_POSITION_KEY] ?: 166044 }

    val eightFtStopHeadFlow: Flow<Double> = context.dataStore.data.map { it[EIGHT_FT_STOP_HEAD_KEY] ?: 2.6 }
    val tenFtStopHeadFlow: Flow<Double> = context.dataStore.data.map { it[TEN_FT_STOP_HEAD_KEY] ?: 26.6 }
    val sixFtStopHeadFlow: Flow<Double> = context.dataStore.data.map { it[SIX_FT_STOP_HEAD_KEY] ?: 2.6 }

    val stepsPerInchFlow: Flow<Double> = context.dataStore.data.map { it[STEPS_PER_INCH_KEY] ?: 1775.36 }

    val stopHeadFlow: Flow<String> = context.dataStore.data.map { it[STOP_HEAD_KEY] ?: "8ft" }
    val tableLengthFlow: Flow<String> = context.dataStore.data.map { it[TABLE_LENGTH_KEY] ?: "8ft" }

    // Save helpers
    suspend fun saveSpeed(value: Int) = context.dataStore.edit { it[SPEED_KEY] = value }
    suspend fun saveAccel(value: Int) = context.dataStore.edit { it[ACCEL_KEY] = value }
    suspend fun saveMaxDelay(value: Int) = context.dataStore.edit { it[MAX_DELAY_KEY] = value }
    suspend fun saveMinDelay(value: Int) = context.dataStore.edit { it[MIN_DELAY_KEY] = value }
    suspend fun saveDirection(value: String) = context.dataStore.edit { it[DIRECTION_KEY] = value }
    suspend fun saveStepPosition(value: Int) = context.dataStore.edit { it[STEP_POSITION_KEY] = value }
    suspend fun saveMinStepPosition(value: Int) = context.dataStore.edit { it[MIN_STEP_POSITION_KEY] = value }
    suspend fun saveMaxStepPosition(value: Int) = context.dataStore.edit { it[MAX_STEP_POSITION_KEY] = value }
    suspend fun saveEightFtStopHead(value: Double) = context.dataStore.edit { it[EIGHT_FT_STOP_HEAD_KEY] = value }
    suspend fun saveTenFtStopHead(value: Double) = context.dataStore.edit { it[TEN_FT_STOP_HEAD_KEY] = value }
    suspend fun saveSixFtStopHead(value: Double) = context.dataStore.edit { it[SIX_FT_STOP_HEAD_KEY] = value }
    suspend fun saveStepsPerInch(value: Double) = context.dataStore.edit { it[STEPS_PER_INCH_KEY] = value }
    suspend fun saveStopHead(value: String) = context.dataStore.edit { it[STOP_HEAD_KEY] = value }
    suspend fun saveTableLength(value: String) = context.dataStore.edit { it[TABLE_LENGTH_KEY] = value }

    // Reset helpers
    suspend fun resetSpeed() = context.dataStore.edit { it.remove(SPEED_KEY) }
    suspend fun resetAccel() = context.dataStore.edit { it.remove(ACCEL_KEY) }
    suspend fun resetMaxDelay() = context.dataStore.edit { it.remove(MAX_DELAY_KEY) }
    suspend fun resetMinDelay() = context.dataStore.edit { it.remove(MIN_DELAY_KEY) }
    suspend fun resetDirection() = context.dataStore.edit { it.remove(DIRECTION_KEY) }
    suspend fun resetStepPosition() = context.dataStore.edit { it.remove(STEP_POSITION_KEY) }
    suspend fun resetMinStepPosition() = context.dataStore.edit { it.remove(MIN_STEP_POSITION_KEY) }
    suspend fun resetMaxStepPosition() = context.dataStore.edit { it.remove(MAX_STEP_POSITION_KEY) }
    suspend fun resetEightFtStopHead() = context.dataStore.edit { it.remove(EIGHT_FT_STOP_HEAD_KEY) }
    suspend fun resetTenFtStopHead() = context.dataStore.edit { it.remove(TEN_FT_STOP_HEAD_KEY) }
    suspend fun resetSixFtStopHead() = context.dataStore.edit { it.remove(SIX_FT_STOP_HEAD_KEY) }
    suspend fun resetStepsPerInch() = context.dataStore.edit { it.remove(STEPS_PER_INCH_KEY) }
    suspend fun resetTableLength() = context.dataStore.edit { it.remove(TABLE_LENGTH_KEY) }
}
