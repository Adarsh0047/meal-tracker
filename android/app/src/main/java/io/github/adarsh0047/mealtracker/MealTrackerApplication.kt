package io.github.adarsh0047.mealtracker

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MealTrackerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (FirebaseApp.getApps(this).isEmpty()) {
            val options = FirebaseOptions.Builder()
                .setApiKey("AIzaSyAIgKKkqEVg5JLTQfsbSogpOOFXupGd61M")
                .setApplicationId("1:329752809344:web:43b4291428877fe852fb65")
                .setProjectId("meal-tracker-62f05")
                .setStorageBucket("meal-tracker-62f05.firebasestorage.app")
                .setGcmSenderId("329752809344")
                .build()
            FirebaseApp.initializeApp(this, options)
        }
        ReminderScheduler.createChannel(this)
    }
}
