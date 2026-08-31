package io.github.adarsh0047.mealtracker

import java.time.LocalDate
import java.time.YearMonth

enum class MealType(val label: String) {
    BREAKFAST("Breakfast"), LUNCH("Lunch"), DINNER("Dinner")
}

data class DayMeals(
    val breakfast: Boolean = false,
    val lunch: Boolean = false,
    val dinner: Boolean = false
) {
    fun value(type: MealType) = when (type) {
        MealType.BREAKFAST -> breakfast
        MealType.LUNCH -> lunch
        MealType.DINNER -> dinner
    }

    fun with(type: MealType, checked: Boolean) = when (type) {
        MealType.BREAKFAST -> copy(breakfast = checked)
        MealType.LUNCH -> copy(lunch = checked)
        MealType.DINNER -> copy(dinner = checked)
    }

    fun toFirestore() = mapOf(
        "breakfast" to breakfast,
        "lunch" to lunch,
        "dinner" to dinner
    )
}

data class MonthCosts(
    val breakfast: Double = 0.0,
    val lunch: Double = 0.0,
    val dinner: Double = 0.0,
    val delivery: Double = 0.0
) {
    fun toFirestore() = mapOf(
        "breakfast" to breakfast,
        "lunch" to lunch,
        "dinner" to dinner,
        "delivery" to delivery
    )
}

data class MealCounts(val breakfast: Int = 0, val lunch: Int = 0, val dinner: Int = 0)

data class MonthSummary(val month: YearMonth, val counts: MealCounts, val costs: MonthCosts) {
    val breakfastTotal get() = counts.breakfast * costs.breakfast
    val lunchTotal get() = counts.lunch * costs.lunch
    val dinnerTotal get() = counts.dinner * costs.dinner
    val total get() = breakfastTotal + lunchTotal + dinnerTotal + costs.delivery
}

fun calculateSummary(month: YearMonth, meals: Map<LocalDate, DayMeals>, costs: MonthCosts): MonthSummary {
    val entries = meals.filterKeys { YearMonth.from(it) == month }.values
    return MonthSummary(
        month,
        MealCounts(
            breakfast = entries.count { it.breakfast },
            lunch = entries.count { it.lunch },
            dinner = entries.count { it.dinner }
        ),
        costs
    )
}
