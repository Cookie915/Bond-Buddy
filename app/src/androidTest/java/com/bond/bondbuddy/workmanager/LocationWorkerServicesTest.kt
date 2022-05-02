package com.bond.bondbuddy.workmanager

import android.content.Context
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.impl.utils.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit


class LocationWorkerServicesTest {
    private val context: Context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setUp() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    class LocationWorkerTest(@ApplicationContext appContext: Context, workerParams: WorkerParameters)
        : Worker(appContext,workerParams){
        override fun doWork(): Result {
            return Result.success()
        }
    }

    @Test
    @Throws(Exception::class)
    fun testLocationWork(){
        //create request
        val request = PeriodicWorkRequestBuilder<LocationWorkerTest>(15, TimeUnit.MINUTES)
            .build()
        val workManager = WorkManager.getInstance(context)
        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        //enque and wait for result
        workManager.enqueue(request).result.get()
        //tell test framework delay is met
        testDriver?.setAllConstraintsMet(request.id)
        //get work info
        val workInfo = workManager.getWorkInfoById(request.id).get()

        //assert
        MatcherAssert.assertThat(workInfo.state, CoreMatchers.`is`(WorkInfo.State.ENQUEUED))
    }
}