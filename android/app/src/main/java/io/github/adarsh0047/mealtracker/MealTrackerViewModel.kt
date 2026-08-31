package io.github.adarsh0047.mealtracker

import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

data class MealUiState(
    val meals: Map<LocalDate, DayMeals> = emptyMap(),
    val costs: Map<YearMonth, MonthCosts> = emptyMap(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val loading: Boolean = true,
    val savingCosts: Boolean = false,
    val error: String? = null,
    val diagnostics: List<String> = emptyList()
)

class MealTrackerViewModel(private val repository: MealRepository = MealRepository()) : ViewModel() {
    private val _state = MutableStateFlow(MealUiState())
    val state: StateFlow<MealUiState> = _state.asStateFlow()
    private val listeners = mutableListOf<ListenerRegistration>()
    private var mealsLoaded = false
    private var costsLoaded = false

    init {
        addDiagnostic("App session started")
        addDiagnostic("Firebase project: meal-tracker-62f05")
        addDiagnostic("Network listeners use Firestore offline persistence")
        listeners += repository.listenToMeals({ meals ->
            mealsLoaded = true
            _state.value = _state.value.copy(meals = meals, loading = !mealsLoaded || !costsLoaded, error = null)
        }, ::showError, ::addDiagnostic)
        listeners += repository.listenToCosts({ costs ->
            costsLoaded = true
            _state.value = _state.value.copy(costs = costs, loading = !mealsLoaded || !costsLoaded, error = null)
        }, ::showError, ::addDiagnostic)
    }

    fun previousMonth() = setMonth(_state.value.displayedMonth.minusMonths(1))
    fun nextMonth() = setMonth(_state.value.displayedMonth.plusMonths(1))
    fun currentMonth() = setMonth(YearMonth.now())
    private fun setMonth(month: YearMonth) { _state.value = _state.value.copy(displayedMonth = month) }

    fun setMeal(date: LocalDate, type: MealType, checked: Boolean) {
        val day = (_state.value.meals[date] ?: DayMeals()).with(type, checked)
        _state.value = _state.value.copy(meals = _state.value.meals + (date to day), error = null)
        addDiagnostic("Writing ${type.label.lowercase()} for $date")
        repository.updateMeal(date, day,
            onSuccess = { addDiagnostic("Meal write confirmed for $date") },
            onError = { addDiagnostic("Meal write failed: ${it.fullDiagnostic()}"); showError(it) })
    }

    fun saveCosts(costs: MonthCosts) {
        val month = _state.value.displayedMonth
        _state.value = _state.value.copy(costs = _state.value.costs + (month to costs), savingCosts = true, error = null)
        addDiagnostic("Writing monthly costs for $month")
        repository.updateCosts(month, costs,
            onSuccess = { _state.value = _state.value.copy(savingCosts = false); addDiagnostic("Cost write confirmed for $month") },
            onError = { _state.value = _state.value.copy(savingCosts = false); addDiagnostic("Cost write failed: ${it.fullDiagnostic()}"); showError(it) })
    }

    fun dismissError() { _state.value = _state.value.copy(error = null) }
    fun clearDiagnostics() {
        _state.value = _state.value.copy(diagnostics = emptyList())
        addDiagnostic("Diagnostic log cleared")
    }

    fun diagnosticReport(): String = buildString {
        appendLine("Meal Tracker diagnostic report")
        appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("Android: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        appendLine("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}")
        appendLine("Firebase project: meal-tracker-62f05")
        appendLine("Generated: ${java.time.ZonedDateTime.now()}")
        appendLine()
        _state.value.diagnostics.forEach(::appendLine)
    }

    private fun showError(error: Throwable) {
        addDiagnostic("UI sync error: ${error.fullDiagnostic()}")
        _state.value = _state.value.copy(loading = false, error = error.localizedMessage ?: "Unable to sync with Firestore")
    }

    private fun addDiagnostic(message: String) {
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))
        _state.value = _state.value.copy(diagnostics = (_state.value.diagnostics + "[$time] $message").takeLast(200))
    }

    private fun Throwable.fullDiagnostic() =
        listOfNotNull(javaClass.simpleName, (this as? com.google.firebase.firestore.FirebaseFirestoreException)?.code?.name, localizedMessage)
            .joinToString(" | ")

    override fun onCleared() {
        listeners.forEach { it.remove() }
        super.onCleared()
    }
}
