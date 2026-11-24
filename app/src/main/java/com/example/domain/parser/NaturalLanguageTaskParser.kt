package com.example.domain.parser

import com.example.domain.model.Priority
import com.example.domain.model.TimeBlock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

data class ParsedTaskInput(
    val cleanTitle: String,
    val dueDate: LocalDate? = null,
    val dueTime: LocalTime? = null,
    val timeBlock: TimeBlock = TimeBlock.ANYTIME,
    val priority: Priority = Priority.MEDIUM,
    val tags: List<String> = emptyList()
)

@Singleton
class NaturalLanguageTaskParser @Inject constructor() {

    private val tagRegex = Regex("""#([\p{L}\p{N}_-]+)""")
    private val time24Regex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
    private val time12EnRegex = Regex("""\b(?:at\s+)?(1[0-2]|0?\d)(?::([0-5]\d))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val timeArRegex = Regex("""(?:الساعة\s*)?(\d{1,2})(?::(\d{2}))?\s*(صباحا|صباحاً|ص|مساء|مساءً|م)(?=\s|$)""")
    private val dateSlashRegex = Regex("""\b(\d{1,2})/(\d{1,2})\b""")

    fun parse(rawInput: String, today: LocalDate = LocalDate.now()): ParsedTaskInput {
        if (rawInput.isBlank()) return ParsedTaskInput("")

        var working = rawInput.trim()

        // 1. Extract tags (#work, #دراسة)
        val tags = tagRegex.findAll(working).map { it.groupValues[1] }.distinct().toList()
        working = tagRegex.replace(working, " ")

        // 2. Extract priority (!urgent, !high, urgent, عاجل, مهم)
        var priority = Priority.MEDIUM
        val priorityRules: List<Pair<Regex, Priority>> = listOf(
            Regex("""(!urgent|\burgent\b|\basap\b|عاجل|طارئ)""", RegexOption.IGNORE_CASE) to Priority.URGENT,
            Regex("""(!high|\bhigh\s+priority\b|أولوية\s+عالية|مهم\s+جداً|مهم\s+جدا|مهم)""", RegexOption.IGNORE_CASE) to Priority.HIGH,
            Regex("""(!low|\blow\s+priority\b|أولوية\s+منخفضة|غير\s+عاجل)""", RegexOption.IGNORE_CASE) to Priority.LOW,
            Regex("""(!med(?:ium)?|\bmedium\s+priority\b|أولوية\s+متوسطة)""", RegexOption.IGNORE_CASE) to Priority.MEDIUM
        )
        for ((regex, matchedPriority) in priorityRules) {
            if (regex.containsMatchIn(working)) {
                priority = matchedPriority
                working = regex.replace(working, " ")
                break
            }
        }

        // 3. Extract time
        var dueTime: LocalTime? = null
        time12EnRegex.find(working)?.let { match ->
            var hour = match.groupValues[1].toIntOrNull() ?: 9
            val minute = match.groupValues[2].toIntOrNull() ?: 0
            val period = match.groupValues[3].lowercase()
            if (period == "pm" && hour < 12) hour += 12
            if (period == "am" && hour == 12) hour = 0
            if (hour in 0..23 && minute in 0..59) {
                dueTime = LocalTime.of(hour, minute)
                working = working.removeRange(match.range)
            }
        }

        if (dueTime == null) {
            timeArRegex.find(working)?.let { match ->
                var hour = match.groupValues[1].toIntOrNull() ?: 9
                val minute = match.groupValues[2].toIntOrNull() ?: 0
                val period = match.groupValues[3]
                val isPm = period.startsWith("م")
                val isAm = period.startsWith("ص")
                if (isPm && hour < 12) hour += 12
                if (isAm && hour == 12) hour = 0
                if (hour in 0..23 && minute in 0..59) {
                    dueTime = LocalTime.of(hour, minute)
                    working = working.removeRange(match.range)
                }
            }
        }

        if (dueTime == null) {
            time24Regex.find(working)?.let { match ->
                val hour = match.groupValues[1].toIntOrNull()
                val minute = match.groupValues[2].toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    dueTime = LocalTime.of(hour, minute)
                    working = working.removeRange(match.range)
                }
            }
        }

        // 4. Extract TimeBlock
        var timeBlock = when {
            dueTime != null && dueTime!!.hour < 12 -> TimeBlock.MORNING
            dueTime != null && dueTime!!.hour < 17 -> TimeBlock.AFTERNOON
            dueTime != null -> TimeBlock.EVENING
            else -> TimeBlock.ANYTIME
        }

        val blockKeywords: List<Pair<Regex, TimeBlock>> = listOf(
            Regex("""(\bmorning\b|صباحاً|صباحا|الصباح|صباح)""", RegexOption.IGNORE_CASE) to TimeBlock.MORNING,
            Regex("""(\bafternoon\b|ظهراً|ظهرا|العصر|بعد\s+الظهر)""", RegexOption.IGNORE_CASE) to TimeBlock.AFTERNOON,
            Regex("""(\bevening\b|\btonight\b|\bnight\b|مساءً|مساءا|المساء|الليل|مساء)""", RegexOption.IGNORE_CASE) to TimeBlock.EVENING
        )
        for ((regex, block) in blockKeywords) {
            if (regex.containsMatchIn(working)) {
                if (dueTime == null) timeBlock = block
                working = regex.replace(working, " ")
                break
            }
        }

        // 5. Extract Date
        var dueDate: LocalDate? = null
        val relativeDates: List<Pair<Regex, LocalDate>> = listOf(
            Regex("""(\bday after tomorrow\b|بعد\s+غد|بعد\s+بكرة)""", RegexOption.IGNORE_CASE) to today.plusDays(2),
            Regex("""(\btomorrow\b|\btmrw\b|غداً|غدا|بكرة)""", RegexOption.IGNORE_CASE) to today.plusDays(1),
            Regex("""(\btoday\b|اليوم|النهارده)""", RegexOption.IGNORE_CASE) to today
        )
        for ((regex, date) in relativeDates) {
            if (regex.containsMatchIn(working)) {
                dueDate = date
                working = regex.replace(working, " ")
                break
            }
        }

        if (dueDate == null) {
            val weekdayPatterns: List<Pair<Regex, DayOfWeek>> = listOf(
                Regex("""(\bmonday\b|\bmon\b|الإثنين|الاثنين)""", RegexOption.IGNORE_CASE) to DayOfWeek.MONDAY,
                Regex("""(\btuesday\b|\btue\b|الثلاثاء)""", RegexOption.IGNORE_CASE) to DayOfWeek.TUESDAY,
                Regex("""(\bwednesday\b|\bwed\b|الأربعاء|الاربعاء)""", RegexOption.IGNORE_CASE) to DayOfWeek.WEDNESDAY,
                Regex("""(\bthursday\b|\bthu\b|الخميس)""", RegexOption.IGNORE_CASE) to DayOfWeek.THURSDAY,
                Regex("""(\bfriday\b|\bfri\b|الجمعة)""", RegexOption.IGNORE_CASE) to DayOfWeek.FRIDAY,
                Regex("""(\bsaturday\b|\bsat\b|السبت)""", RegexOption.IGNORE_CASE) to DayOfWeek.SATURDAY,
                Regex("""(\bsunday\b|\bsun\b|الأحد|الاحد)""", RegexOption.IGNORE_CASE) to DayOfWeek.SUNDAY
            )
            for ((regex, dow) in weekdayPatterns) {
                if (regex.containsMatchIn(working)) {
                    dueDate = today.with(TemporalAdjusters.nextOrSame(dow))
                    working = regex.replace(working, " ")
                    break
                }
            }
        }

        if (dueDate == null) {
            dateSlashRegex.find(working)?.let { match ->
                val day = match.groupValues[1].toIntOrNull()
                val month = match.groupValues[2].toIntOrNull()
                if (day != null && month != null && month in 1..12 && day in 1..31) {
                    runCatching {
                        var candidate = LocalDate.of(today.year, month, day)
                        if (candidate.isBefore(today)) {
                            candidate = candidate.plusYears(1)
                        }
                        dueDate = candidate
                        working = working.removeRange(match.range)
                    }
                }
            }
        }

        // If user specified a time or block without a date, default date to today
        if (dueDate == null && (dueTime != null || timeBlock != TimeBlock.ANYTIME)) {
            dueDate = today
        }

        val cleanTitle = working
            .replace(Regex("""\s+(at|في|الساعة)\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()
            .ifEmpty { rawInput.trim() }

        return ParsedTaskInput(
            cleanTitle = cleanTitle,
            dueDate = dueDate,
            dueTime = dueTime,
            timeBlock = timeBlock,
            priority = priority,
            tags = tags
        )
    }
}
