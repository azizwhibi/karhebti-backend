package com.example.karhebti_android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.*
import com.example.karhebti_android.MainActivity
import com.example.karhebti_android.R
import com.example.karhebti_android.data.database.AppDatabase
import com.example.karhebti_android.data.newrepo.MaintenanceRepositoryImpl
import com.example.karhebti_android.data.repository.Resource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EntretiensWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Schedule periodic updates
        scheduleWidgetUpdate(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Cancel periodic updates
        WorkManager.getInstance(context).cancelUniqueWork(WIDGET_UPDATE_WORK)
    }

    companion object {
        const val WIDGET_UPDATE_WORK = "widget_update_work"
        const val EXTRA_MAINTENANCE_ID = "maintenance_id"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_entretiens)

            // Load data from Room database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val database = AppDatabase.getInstance(context)
                    val maintenances = database.upcomingMaintenanceDao()
                        .getUpcomingMaintenancesList(5)

                    CoroutineScope(Dispatchers.Main).launch {
                        if (maintenances.isEmpty()) {
                            views.setTextViewText(
                                R.id.widget_empty_text,
                                "Aucun entretien prévu"
                            )
                        } else {
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
                            val sb = StringBuilder()

                            maintenances.forEachIndexed { index, maintenance ->
                                if (index < 5) {
                                    sb.append("${maintenance.title}\n")
                                    if (maintenance.plate != null) {
                                        sb.append("${maintenance.plate} - ")
                                    }
                                    sb.append(dateFormat.format(maintenance.dueAt))
                                    sb.append("\n\n")
                                }
                            }

                            views.setTextViewText(R.id.widget_content_text, sb.toString())
                        }

                        // Set click intent to open app
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        val pendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            intent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun scheduleWidgetUpdate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
                30, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WIDGET_UPDATE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                updateRequest
            )
        }
    }
}

class WidgetUpdateWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Use the implementation that accepts a Context (it handles caching)
            val repository = MaintenanceRepositoryImpl(applicationContext)

            // Fetch latest data from API
            val result = repository.getUpcomingMaintenances(
                limit = 5,
                includePlate = true
            )

            // Use a generic-safe type check
            if (result is Resource.Success<*>) {
                // Update all widgets
                val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(
                        applicationContext,
                        EntretiensWidget::class.java
                    )
                )

                for (appWidgetId in appWidgetIds) {
                    EntretiensWidget.updateAppWidget(
                        applicationContext,
                        appWidgetManager,
                        appWidgetId
                    )
                }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
