package com.healthos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class HealthOsApplication : Application(), Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.SENTRY_DSN.isNotBlank()) {
            SentryAndroid.init(this) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.isEnableUserInteractionTracing = true
                options.tracesSampleRate = 0.2
                options.setBeforeSend { event, _ ->
                    // Sanitizar mensajes o tags que pudieran contener PHI
                    event.breadcrumbs?.forEach { breadcrumb ->
                        if (breadcrumb.data.containsKey("Authorization")) {
                            breadcrumb.data["Authorization"] = "[REDACTED]"
                        }
                    }
                    event
                }
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
