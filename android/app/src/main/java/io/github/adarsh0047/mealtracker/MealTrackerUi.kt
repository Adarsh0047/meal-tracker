package io.github.adarsh0047.mealtracker

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val Forest = Color(0xFF58D68D)
private val ForestDeep = Color(0xFF208A55)
private val Mint = Color(0xFFA8F0C6)
private val Ink = Color(0xFF09110E)
private val AppSurface = Color(0xFF121C18)
private val SurfaceHigh = Color(0xFF1B2822)
private val Stroke = Color(0xFF2A3A32)
private val Muted = Color(0xFF9AAFA4)
private val Breakfast = Color(0xFFFFC857)
private val Lunch = Color(0xFF65B8FF)
private val Dinner = Color(0xFFB99AFF)
private val money = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("en").setRegion("IN").build())
private val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM")
private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
private val clockFormatter = DateTimeFormatter.ofPattern("h:mm a")

@Composable
fun MealTrackerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Forest, onPrimary = Color(0xFF002112), primaryContainer = Color(0xFF163C29),
            onPrimaryContainer = Mint, secondary = Mint, tertiary = Breakfast, background = Ink,
            onBackground = Color(0xFFE5F2EA), surface = AppSurface, surfaceVariant = SurfaceHigh,
            outline = Stroke, error = Color(0xFFFF8A80)
        ),
        content = content
    )
}

private enum class AppTab(val label: String, val icon: ImageVector) {
    CALENDAR("Today", Icons.Rounded.CalendarMonth),
    REMINDERS("Remind", Icons.Rounded.NotificationsActive),
    COSTS("Costs", Icons.Rounded.AccountBalanceWallet),
    HISTORY("History", Icons.Rounded.Insights),
    LOGS("Logs", Icons.Rounded.BugReport)
}

@Composable
fun MealTrackerApp(
    state: MealUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onMealChanged: (LocalDate, MealType, Boolean) -> Unit,
    onSaveCosts: (MonthCosts) -> Unit,
    onDismissError: () -> Unit,
    diagnosticReport: () -> String,
    onClearDiagnostics: () -> Unit,
    onShareDiagnostics: () -> Unit
) {
    var tab by remember { mutableStateOf(AppTab.CALENDAR) }
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { AppHeader(state, tab) },
        bottomBar = {
            NavigationBar(containerColor = AppSurface.copy(alpha = .98f), tonalElevation = 0.dp) {
                AppTab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item, onClick = { tab = item },
                        icon = { Icon(item.icon, item.label) }, label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Ink, selectedTextColor = Forest, indicatorColor = Forest,
                            unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        },
        snackbarHost = {
            state.error?.let { message ->
                Snackbar(
                    modifier = Modifier.padding(16.dp), shape = RoundedCornerShape(16.dp),
                    action = { TextButton(onClick = onDismissError) { Text("Dismiss") } }
                ) { Text(message) }
            }
        }
    ) { padding ->
        Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink, Color(0xFF0D1713), Ink))).padding(padding)
        ) {
            AnimatedContent(
                targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "screen", modifier = Modifier.fillMaxSize()
            ) { destination ->
                ResponsiveContent {
                    when (destination) {
                        AppTab.CALENDAR -> CalendarScreen(state, onPreviousMonth, onNextMonth, onToday, onMealChanged)
                        AppTab.REMINDERS -> RemindersScreen()
                        AppTab.COSTS -> CostsScreen(state, onPreviousMonth, onNextMonth, onSaveCosts)
                        AppTab.HISTORY -> HistoryScreen(state)
                        AppTab.LOGS -> DiagnosticsScreen(state, diagnosticReport, onClearDiagnostics, onShareDiagnostics)
                    }
                }
            }
            if (state.loading) LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.TopCenter),
                color = Forest, trackColor = Color.Transparent
            )
        }
    }
}

@Composable
private fun AppHeader(state: MealUiState, tab: AppTab) {
    Surface(color = Ink.copy(alpha = .97f)) {
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(Forest, ForestDeep))),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.RestaurantMenu, null, tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Meal Tracker", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(tab.label, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            val statusColor = when { state.error != null -> MaterialTheme.colorScheme.error; state.loading -> Breakfast; else -> Forest }
            Surface(shape = CircleShape, color = statusColor.copy(alpha = .14f)) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(statusColor))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (state.loading) "Syncing" else if (state.error != null) "Issue" else "Live",
                        color = statusColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponsiveContent(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.fillMaxSize().widthIn(max = 760.dp), content = content)
    }
}

@Composable
private fun MonthControl(month: YearMonth, previous: () -> Unit, next: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = SurfaceHigh, border = CardDefaults.outlinedCardBorder()) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = previous, modifier = Modifier.background(AppSurface, CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Previous month")
            }
            AnimatedContent(month, label = "month", modifier = Modifier.weight(1f)) { selected ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(selected.format(monthNameFormatter), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(selected.year.toString(), color = Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            IconButton(onClick = next, modifier = Modifier.background(AppSurface, CircleShape)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, "Next month")
            }
        }
    }
}

@Composable
private fun CalendarScreen(
    state: MealUiState, previous: () -> Unit, next: () -> Unit, today: () -> Unit,
    onMealChanged: (LocalDate, MealType, Boolean) -> Unit
) {
    val month = state.displayedMonth
    val summary = calculateSummary(month, state.meals, state.costs[month] ?: MonthCosts())
    val dates = remember(month) { (1..month.lengthOfMonth()).map(month::atDay) }
    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { MonthControl(month, previous, next) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SummaryMetric("Breakfast", summary.counts.breakfast, Breakfast, Icons.Rounded.WbSunny, Modifier.weight(1f))
                SummaryMetric("Lunch", summary.counts.lunch, Lunch, Icons.Rounded.LunchDining, Modifier.weight(1f))
                SummaryMetric("Dinner", summary.counts.dinner, Dinner, Icons.Rounded.NightsStay, Modifier.weight(1f))
            }
        }
        if (month != YearMonth.now()) item {
            FilledTonalButton(onClick = today, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Rounded.Today, null); Spacer(Modifier.width(8.dp)); Text("Return to this month")
            }
        }
        item { SectionTitle("Daily meals", "Tap a meal to mark it complete") }
        items(dates, key = { it.toString() }, contentType = { "day" }) { date -> DayCard(date, state.meals[date] ?: DayMeals(), onMealChanged) }
    }
}

@Composable
private fun SummaryMetric(label: String, count: Int, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier, color = color.copy(alpha = .10f), shape = RoundedCornerShape(18.dp), border = CardDefaults.outlinedCardBorder()) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(19.dp)); Spacer(Modifier.height(10.dp))
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DayCard(date: LocalDate, meals: DayMeals, onChanged: (LocalDate, MealType, Boolean) -> Unit) {
    val isToday = date == LocalDate.now()
    val complete = listOf(meals.breakfast, meals.lunch, meals.dinner).count { it }
    Surface(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = if (isToday) Forest.copy(alpha = .10f) else AppSurface,
        border = BorderStroke(1.dp, if (isToday) Forest.copy(alpha = .65f) else Stroke)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(14.dp), color = if (isToday) Forest else SurfaceHigh) {
                    Column(Modifier.size(52.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(date.dayOfMonth.toString(), color = if (isToday) Ink else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                            color = if (isToday) Ink.copy(alpha = .7f) else Muted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()), fontWeight = FontWeight.SemiBold)
                    Text(if (isToday) "Today" else "$complete of 3 meals", color = if (isToday) Forest else Muted, style = MaterialTheme.typography.bodySmall)
                }
                if (complete == 3) Icon(Icons.Rounded.CheckCircle, "All meals complete", tint = Forest)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MealToggle(MealType.BREAKFAST, meals.breakfast, Breakfast, Modifier.weight(1f)) { onChanged(date, MealType.BREAKFAST, it) }
                MealToggle(MealType.LUNCH, meals.lunch, Lunch, Modifier.weight(1f)) { onChanged(date, MealType.LUNCH, it) }
                MealToggle(MealType.DINNER, meals.dinner, Dinner, Modifier.weight(1f)) { onChanged(date, MealType.DINNER, it) }
            }
        }
    }
}

@Composable
private fun MealToggle(type: MealType, checked: Boolean, color: Color, modifier: Modifier = Modifier, changed: (Boolean) -> Unit) {
    val container by animateColorAsState(if (checked) color.copy(alpha = .18f) else SurfaceHigh, label = "meal")
    Surface(
        onClick = { changed(!checked) }, modifier = modifier, color = container, shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, if (checked) color.copy(alpha = .6f) else Stroke)
    ) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(if (checked) Icons.Rounded.Check else Icons.Rounded.Add, null, tint = if (checked) color else Muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.height(3.dp))
            Text(type.label.take(1), color = if (checked) color else Muted, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemindersScreen() {
    val context = LocalContext.current
    val store = remember { ReminderStore(context) }
    var reminders by remember { mutableStateOf(store.load()) }
    var showEditor by remember { mutableStateOf(false) }
    var permissionRefresh by remember { mutableIntStateOf(0) }
    val notificationsAllowed = permissionRefresh.let {
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactAllowed = permissionRefresh.let { ReminderScheduler.canScheduleExact(context) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permissionRefresh++ }
    val exactPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { permissionRefresh++ }

    fun commit(updated: List<MealReminder>) {
        reminders = updated.sortedWith(compareBy({ it.days.minOfOrNull { day -> day.value } ?: 8 }, { it.hour }, { it.minute }))
        store.save(reminders)
    }

    LazyColumn(
        Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionTitle("Meal reminders", "Weekly notifications, even when the app is closed") }
        if (!notificationsAllowed) item {
            PermissionCard(
                icon = Icons.Rounded.NotificationsOff, title = "Notifications are off",
                message = "Allow notifications so scheduled reminders can appear on this device.", button = "Allow"
            ) { if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
        }
        if (!exactAllowed) item {
            PermissionCard(
                icon = Icons.Rounded.Schedule, title = "Precise timing is off",
                message = "Reminders will still work, but Android may delay them. Allow Alarms & reminders for exact delivery.", button = "Open settings"
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    exactPermission.launch(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
            }
        }
        item {
            Button(onClick = { showEditor = true }, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Rounded.AddAlarm, null); Spacer(Modifier.width(8.dp)); Text("Add reminder", fontWeight = FontWeight.Bold)
            }
        }
        if (reminders.isEmpty()) item {
            EmptyState(Icons.Rounded.AlarmAdd, "No reminders yet", "Add as many weekday and time combinations as you need.")
        }
        items(reminders, key = { it.id }, contentType = { "reminder" }) { reminder ->
            ReminderCard(
                reminder = reminder,
                onToggle = { enabled ->
                    val changed = reminder.copy(enabled = enabled)
                    commit(reminders.map { if (it.id == reminder.id) changed else it })
                    if (enabled) ReminderScheduler.schedule(context, changed) else ReminderScheduler.cancel(context, reminder.id)
                },
                onDelete = {
                    ReminderScheduler.cancel(context, reminder.id)
                    commit(reminders.filterNot { it.id == reminder.id })
                }
            )
        }
        item {
            Text(
                "Schedules are stored only on this phone. Battery-saving settings from your device manufacturer may affect delivery.",
                color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }

    if (showEditor) ReminderEditor(
        onDismiss = { showEditor = false },
        onAdd = { reminder ->
            commit(reminders + reminder)
            ReminderScheduler.schedule(context, reminder)
            showEditor = false
        }
    )
}

@Composable
private fun PermissionCard(icon: ImageVector, title: String, message: String, button: String, action: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Breakfast.copy(alpha = .09f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Breakfast.copy(alpha = .4f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = Breakfast); Spacer(Modifier.width(10.dp))
                Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Text(message, color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = action, modifier = Modifier.align(Alignment.End)) { Text(button) }
        }
    }
}

@Composable
private fun ReminderCard(reminder: MealReminder, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    val time = remember(reminder.hour, reminder.minute) { LocalTime.of(reminder.hour, reminder.minute).format(clockFormatter) }
    Surface(
        Modifier.fillMaxWidth(), color = if (reminder.enabled) Forest.copy(alpha = .08f) else AppSurface,
        shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, if (reminder.enabled) Forest.copy(alpha = .4f) else Stroke)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(15.dp), color = if (reminder.enabled) Forest else SurfaceHigh) {
                Icon(Icons.Rounded.Alarm, null, tint = if (reminder.enabled) Ink else Muted, modifier = Modifier.padding(13.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(time, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(reminder.days.sortedBy { it.value }.joinToString(" · ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) },
                    color = Forest, fontWeight = FontWeight.SemiBold)
                Text(reminder.label, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(checked = reminder.enabled, onCheckedChange = onToggle)
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete reminder", tint = Muted) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditor(onDismiss: () -> Unit, onAdd: (MealReminder) -> Unit) {
    var selectedDays by remember { mutableStateOf(setOf(DayOfWeek.MONDAY)) }
    var label by remember { mutableStateOf("Time to update your meals") }
    val timeState = rememberTimePickerState(initialHour = 20, initialMinute = 0, is24Hour = false)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New reminder", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Choose one or more days", color = Muted, style = MaterialTheme.typography.labelMedium)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DayOfWeek.entries.forEach { day ->
                        FilterChip(
                            selected = day in selectedDays,
                            onClick = {
                                selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                            },
                            label = { Text(day.getDisplayName(TextStyle.NARROW, Locale.getDefault())) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                TimePicker(state = timeState, modifier = Modifier.align(Alignment.CenterHorizontally))
                OutlinedTextField(
                    value = label, onValueChange = { label = it.take(80) }, label = { Text("Notification message") },
                    singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onAdd(MealReminder(days = selectedDays, hour = timeState.hour, minute = timeState.minute, label = label.ifBlank { "Time to update your meals" })) },
                enabled = selectedDays.isNotEmpty()
            ) {
                Text("Schedule")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CostsScreen(state: MealUiState, previous: () -> Unit, next: () -> Unit, save: (MonthCosts) -> Unit) {
    val month = state.displayedMonth
    val stored = state.costs[month] ?: MonthCosts()
    var breakfast by remember(month, stored) { mutableStateOf(stored.breakfast.editString()) }
    var lunch by remember(month, stored) { mutableStateOf(stored.lunch.editString()) }
    var dinner by remember(month, stored) { mutableStateOf(stored.dinner.editString()) }
    var delivery by remember(month, stored) { mutableStateOf(stored.delivery.editString()) }
    val draft = MonthCosts(breakfast.toDoubleOrNull() ?: 0.0, lunch.toDoubleOrNull() ?: 0.0, dinner.toDoubleOrNull() ?: 0.0, delivery.toDoubleOrNull() ?: 0.0)
    val summary = calculateSummary(month, state.meals, draft)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { MonthControl(month, previous, next) }
        item { TotalHero(summary.total, summary.counts.breakfast + summary.counts.lunch + summary.counts.dinner) }
        item { SectionTitle("Meal pricing", "Set the price paid for each completed meal") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CostField("Breakfast", breakfast, Icons.Rounded.WbSunny, Breakfast) { breakfast = it.validDecimal() }
                CostField("Lunch", lunch, Icons.Rounded.LunchDining, Lunch) { lunch = it.validDecimal() }
                CostField("Dinner", dinner, Icons.Rounded.NightsStay, Dinner) { dinner = it.validDecimal() }
                CostField("Monthly delivery", delivery, Icons.Rounded.DeliveryDining, Forest) { delivery = it.validDecimal() }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = AppSurface, border = CardDefaults.outlinedCardBorder()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
                    CostRow("Breakfast · ${summary.counts.breakfast}", summary.breakfastTotal, Breakfast)
                    CostRow("Lunch · ${summary.counts.lunch}", summary.lunchTotal, Lunch)
                    CostRow("Dinner · ${summary.counts.dinner}", summary.dinnerTotal, Dinner)
                    CostRow("Delivery", draft.delivery, Forest)
                    HorizontalDivider(color = Stroke)
                    CostRow("Monthly total", summary.total, Forest, true)
                }
            }
        }
        item {
            Button(onClick = { save(draft) }, enabled = !state.savingCosts, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                if (state.savingCosts) CircularProgressIndicator(Modifier.size(21.dp), strokeWidth = 2.dp, color = Ink)
                else { Icon(Icons.Rounded.CloudUpload, null); Spacer(Modifier.width(8.dp)); Text("Save prices", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TotalHero(total: Double, mealCount: Int) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Box(Modifier.background(Brush.linearGradient(listOf(ForestDeep, Color(0xFF285D47)))).padding(22.dp)) {
            Column {
                Text("ESTIMATED TOTAL", color = Mint.copy(alpha = .75f), style = MaterialTheme.typography.labelMedium)
                Text(money.format(total), color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(5.dp)); Text("$mealCount completed meals this month", color = Color.White.copy(alpha = .72f))
            }
            Icon(Icons.Rounded.ReceiptLong, null, tint = Color.White.copy(alpha = .16f), modifier = Modifier.size(78.dp).align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun CostField(label: String, value: String, icon: ImageVector, color: Color, changed: (String) -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = changed, label = { Text(label) }, prefix = { Text("₹") },
        leadingIcon = { Icon(icon, null, tint = color) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = color, unfocusedBorderColor = Stroke)
    )
}

@Composable
private fun CostRow(label: String, amount: Double, color: Color = Muted, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color)); Spacer(Modifier.width(10.dp))
        Text(label, modifier = Modifier.weight(1f), color = if (emphasize) MaterialTheme.colorScheme.onSurface else Muted,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal)
        Text(money.format(amount), color = if (emphasize) Forest else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun HistoryScreen(state: MealUiState) {
    val months = (state.costs.keys + state.meals.keys.map(YearMonth::from)).distinct().sortedDescending()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Monthly history", "Your spending and meal activity over time") }
        if (months.isEmpty()) item { EmptyState(Icons.Rounded.Insights, "No history yet", "Complete a meal to start building your timeline.") }
        items(months, key = { it.toString() }, contentType = { "history" }) { month -> HistoryCard(calculateSummary(month, state.meals, state.costs[month] ?: MonthCosts())) }
    }
}

@Composable
private fun HistoryCard(summary: MonthSummary) {
    val max = maxOf(summary.total, 1.0)
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = AppSurface, border = CardDefaults.outlinedCardBorder()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(summary.month.format(monthYearFormatter), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("${summary.counts.breakfast + summary.counts.lunch + summary.counts.dinner} meals completed", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                Text(money.format(summary.total), color = Forest, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HistoryBar("Breakfast", summary.breakfastTotal / max, Breakfast)
            HistoryBar("Lunch", summary.lunchTotal / max, Lunch)
            HistoryBar("Dinner", summary.dinnerTotal / max, Dinner)
            if (summary.costs.delivery > 0) HistoryBar("Delivery", summary.costs.delivery / max, Forest)
        }
    }
}

@Composable
private fun HistoryBar(label: String, ratio: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(62.dp))
        LinearProgressIndicator(progress = { ratio.toFloat().coerceIn(0f, 1f) }, modifier = Modifier.weight(1f).height(7.dp).clip(CircleShape), color = color, trackColor = SurfaceHigh)
    }
}

@Composable
private fun DiagnosticsScreen(state: MealUiState, report: () -> String, onClear: () -> Unit, onShare: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    val status = when { state.error != null -> "Connection issue"; state.loading -> "Connecting…"; else -> "Everything looks good" }
    val statusColor = when { state.error != null -> MaterialTheme.colorScheme.error; state.loading -> Breakfast; else -> Forest }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SectionTitle("Diagnostics", "Firestore activity from this device") }
        item {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = statusColor.copy(alpha = .10f), border = BorderStroke(1.dp, statusColor.copy(alpha = .45f))) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = statusColor.copy(alpha = .16f)) {
                        Icon(if (state.error != null) Icons.Rounded.ErrorOutline else Icons.Rounded.CloudDone, null, tint = statusColor, modifier = Modifier.padding(11.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(status, color = statusColor, fontWeight = FontWeight.Bold)
                        Text("${state.meals.size} days · ${state.costs.size} months loaded", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { clipboard.setText(AnnotatedString(report())) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.ContentCopy, null); Spacer(Modifier.width(7.dp)); Text("Copy")
                }
                Button(onClick = onShare, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Rounded.Share, null); Spacer(Modifier.width(7.dp)); Text("Share")
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Activity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClear) { Text("Clear log") }
            }
        }
        if (state.diagnostics.isEmpty()) item { EmptyState(Icons.Rounded.Terminal, "No events", "New connection activity will appear here.") }
        items(state.diagnostics.asReversed(), contentType = { "log" }) { entry ->
            Surface(Modifier.fillMaxWidth(), color = AppSurface, shape = RoundedCornerShape(14.dp), border = CardDefaults.outlinedCardBorder()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.padding(top = 5.dp).size(6.dp).clip(CircleShape).background(Forest)); Spacer(Modifier.width(9.dp))
                    Text(entry, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 42.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = Forest.copy(alpha = .10f)) { Icon(icon, null, tint = Forest, modifier = Modifier.padding(18.dp).size(30.dp)) }
        Spacer(Modifier.height(14.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
    }
}

private fun Double.editString() = if (this == 0.0) "" else if (this % 1.0 == 0.0) toLong().toString() else toString()
private fun String.validDecimal(): String = if (isEmpty() || matches(Regex("\\d*(\\.\\d{0,2})?"))) this else dropLast(1)
