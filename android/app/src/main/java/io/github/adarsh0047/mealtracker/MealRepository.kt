package io.github.adarsh0047.mealtracker

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.YearMonth

class MealRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    companion object {
        private const val COLLECTION = "trackers"
        private const val MEALS_DOCUMENT = "my_secret_meal_data_9876"
        private const val COSTS_DOCUMENT = "my_secret_meal_costs_9876"
    }

    fun listenToMeals(
        onChange: (Map<LocalDate, DayMeals>) -> Unit,
        onError: (Throwable) -> Unit,
        onStatus: (String) -> Unit
    ): ListenerRegistration {
        onStatus("Starting meals listener")
        return db.collection(COLLECTION).document(MEALS_DOCUMENT).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onStatus("Meals listener error: ${error.diagnosticMessage()}")
                return@addSnapshotListener onError(error)
            }
            val parsed = snapshot?.data.orEmpty().mapNotNull { (key, value) ->
                val date = runCatching { LocalDate.parse(key) }.getOrNull() ?: return@mapNotNull null
                val map = value as? Map<*, *> ?: return@mapNotNull null
                date to DayMeals(
                    breakfast = map["breakfast"] as? Boolean ?: false,
                    lunch = map["lunch"] as? Boolean ?: false,
                    dinner = map["dinner"] as? Boolean ?: false
                )
            }.toMap()
            onStatus(
                "Meals snapshot: source=${if (snapshot?.metadata?.isFromCache == true) "cache" else "server"}, " +
                    "exists=${snapshot?.exists() == true}, days=${parsed.size}, " +
                    "pendingWrites=${snapshot?.metadata?.hasPendingWrites() == true}"
            )
            onChange(parsed)
        }
    }

    fun listenToCosts(
        onChange: (Map<YearMonth, MonthCosts>) -> Unit,
        onError: (Throwable) -> Unit,
        onStatus: (String) -> Unit
    ): ListenerRegistration {
        onStatus("Starting costs listener")
        return db.collection(COLLECTION).document(COSTS_DOCUMENT).addSnapshotListener { snapshot, error ->
            if (error != null) {
                onStatus("Costs listener error: ${error.diagnosticMessage()}")
                return@addSnapshotListener onError(error)
            }
            val parsed = snapshot?.data.orEmpty().mapNotNull { (key, value) ->
                val month = runCatching { YearMonth.parse(key) }.getOrNull() ?: return@mapNotNull null
                val map = value as? Map<*, *> ?: return@mapNotNull null
                month to MonthCosts(
                    breakfast = (map["breakfast"] as? Number)?.toDouble() ?: 0.0,
                    lunch = (map["lunch"] as? Number)?.toDouble() ?: 0.0,
                    dinner = (map["dinner"] as? Number)?.toDouble() ?: 0.0,
                    delivery = (map["delivery"] as? Number)?.toDouble() ?: 0.0
                )
            }.toMap()
            onStatus(
                "Costs snapshot: source=${if (snapshot?.metadata?.isFromCache == true) "cache" else "server"}, " +
                    "exists=${snapshot?.exists() == true}, months=${parsed.size}, " +
                    "pendingWrites=${snapshot?.metadata?.hasPendingWrites() == true}"
            )
            onChange(parsed)
        }
    }

    fun updateMeal(date: LocalDate, meals: DayMeals, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        db.collection(COLLECTION).document(MEALS_DOCUMENT)
            .set(mapOf(date.toString() to meals.toFirestore()), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    fun updateCosts(month: YearMonth, costs: MonthCosts, onSuccess: () -> Unit, onError: (Throwable) -> Unit) {
        db.collection(COLLECTION).document(COSTS_DOCUMENT)
            .set(mapOf(month.toString() to costs.toFirestore()), SetOptions.merge())
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener(onError)
    }

    private fun Throwable.diagnosticMessage(): String {
        val code = (this as? FirebaseFirestoreException)?.code?.name
        return listOfNotNull(javaClass.simpleName, code, localizedMessage).joinToString(" | ")
    }
}
