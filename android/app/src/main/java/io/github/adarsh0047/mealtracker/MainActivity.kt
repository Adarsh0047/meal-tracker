package io.github.adarsh0047.mealtracker

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MealTrackerTheme {
                Surface(Modifier.fillMaxSize()) {
                    val viewModel: MealTrackerViewModel = viewModel()
                    MealTrackerApp(
                        state = viewModel.state.collectAsStateWithLifecycle().value,
                        onPreviousMonth = viewModel::previousMonth,
                        onNextMonth = viewModel::nextMonth,
                        onToday = viewModel::currentMonth,
                        onMealChanged = viewModel::setMeal,
                        onSaveCosts = viewModel::saveCosts,
                        onDismissError = viewModel::dismissError,
                        diagnosticReport = viewModel::diagnosticReport,
                        onClearDiagnostics = viewModel::clearDiagnostics,
                        onShareDiagnostics = {
                            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Meal Tracker diagnostics")
                                putExtra(Intent.EXTRA_TEXT, viewModel.diagnosticReport())
                            }, "Share diagnostics"))
                        }
                    )
                }
            }
        }
    }
}
