package com.healthos.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class NormalSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result = Result.success()
    }

@HiltWorker
class CriticalSyncWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val eventId = inputData.getString("EVENT_ID") ?: return Result.failure()
            return if (eventId.isBlank()) Result.failure() else Result.success()
        }
    }
