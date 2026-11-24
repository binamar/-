package com.example

import com.example.domain.model.Priority
import com.example.domain.model.Routine
import com.example.domain.model.TimeBlock
import com.example.domain.parser.NaturalLanguageTaskParser
import com.example.domain.usecase.CalculateRoutineStatsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class FlowDayDomainTest {

    private val parser = NaturalLanguageTaskParser()
    private val streakCalculator = CalculateRoutineStatsUseCase()

    @Test
    fun `english natural language parses date time priority and tags`() {
        val today = LocalDate.of(2026, 11, 24)
        val parsed = parser.parse("Prepare sprint deck tomorrow at 4:30pm #work !urgent", today)
        assertEquals("Prepare sprint deck", parsed.cleanTitle)
        assertEquals(today.plusDays(1), parsed.dueDate)
        assertEquals(LocalTime.of(16, 30), parsed.dueTime)
        assertEquals(Priority.URGENT, parsed.priority)
        assertEquals(listOf("work"), parsed.tags)
    }

    @Test
    fun `arabic natural language parses date time priority and tags`() {
        val today = LocalDate.of(2026, 11, 24)
        val parsed = parser.parse("مراجعة الفصل غداً الساعة 5 مساءً #دراسة عاجل", today)
        assertEquals("مراجعة الفصل", parsed.cleanTitle)
        assertEquals(today.plusDays(1), parsed.dueDate)
        assertEquals(LocalTime.of(17, 0), parsed.dueTime)
        assertEquals(Priority.URGENT, parsed.priority)
        assertEquals(listOf("دراسة"), parsed.tags)
    }

    @Test
    fun `gentle streak tolerates one missed grace day`() {
        val today = LocalDate.of(2026, 11, 24)
        val routine = Routine(
            id = 1L,
            title = "Morning Walk",
            activeDays = setOf(1, 2, 3, 4, 5, 6, 7),
            createdAt = 0L
        )
        // Completed today, missed yesterday (grace day), completed 2 & 3 days ago
        val logs = setOf(today, today.minusDays(2), today.minusDays(3))
        val stats = streakCalculator.calculate(routine, logs, today)
        assertEquals(3, stats.currentStreak)
        assertTrue(stats.graceUsedThisWeek)
    }
}
