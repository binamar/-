package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.model.Priority
import com.example.domain.model.RoutineCategory
import com.example.domain.model.RoutineDayVariation
import com.example.domain.model.TimeBlock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RoutineDayVariationDto(
    val dayOfWeek: Int,
    val note: String,
    val durationMinutes: Int
)

object RoomTypeConverters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromPriority(value: Priority): String = value.name

    @TypeConverter
    fun toPriority(value: String): Priority =
        runCatching { Priority.valueOf(value) }.getOrDefault(Priority.MEDIUM)

    @TypeConverter
    fun fromTimeBlock(value: TimeBlock): String = value.name

    @TypeConverter
    fun toTimeBlock(value: String): TimeBlock =
        runCatching { TimeBlock.valueOf(value) }.getOrDefault(TimeBlock.ANYTIME)

    @TypeConverter
    fun fromRoutineCategory(value: RoutineCategory): String = value.name

    @TypeConverter
    fun toRoutineCategory(value: String): RoutineCategory =
        runCatching { RoutineCategory.valueOf(value) }.getOrDefault(RoutineCategory.CUSTOM)

    fun encodeStringList(list: List<String>): String = json.encodeToString(list)

    fun decodeStringList(raw: String): List<String> =
        runCatching { json.decodeFromString<List<String>>(raw) }.getOrDefault(emptyList())

    fun encodeIntSet(set: Set<Int>): String = json.encodeToString(set.toList())

    fun decodeIntSet(raw: String): Set<Int> =
        runCatching { json.decodeFromString<List<Int>>(raw).toSet() }.getOrDefault(setOf(1, 2, 3, 4, 5, 6, 7))

    fun encodeVariations(map: Map<Int, RoutineDayVariation>): String {
        val dtoMap = map.mapKeys { it.key.toString() }.mapValues {
            RoutineDayVariationDto(it.value.dayOfWeek, it.value.note, it.value.durationMinutes)
        }
        return json.encodeToString(dtoMap)
    }

    fun decodeVariations(raw: String): Map<Int, RoutineDayVariation> =
        runCatching {
            val dtoMap = json.decodeFromString<Map<String, RoutineDayVariationDto>>(raw)
            dtoMap.mapNotNull { (k, v) ->
                k.toIntOrNull()?.let { day ->
                    day to RoutineDayVariation(v.dayOfWeek, v.note, v.durationMinutes)
                }
            }.toMap()
        }.getOrDefault(emptyMap())
}
