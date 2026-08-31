package io.github.adarsh0047.mealtracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MealModelsTest {
    @Test
    fun summaryCountsOnlyRequestedMonthAndCalculatesTotal() {
        val meals = mapOf(
            LocalDate.of(2026, 8, 1) to DayMeals(breakfast = true, lunch = true),
            LocalDate.of(2026, 8, 2) to DayMeals(breakfast = true, dinner = true),
            LocalDate.of(2026, 9, 1) to DayMeals(breakfast = true, lunch = true, dinner = true)
        )
        val summary = calculateSummary(YearMonth.of(2026, 8), meals, MonthCosts(10.0, 20.0, 30.0, 40.0))

        assertEquals(MealCounts(2, 1, 1), summary.counts)
        assertEquals(110.0, summary.total, 0.0)
    }

    @Test
    fun mealToggleChangesOnlySelectedMeal() {
        assertEquals(DayMeals(lunch = true), DayMeals().with(MealType.LUNCH, true))
    }
}
