package com.jovoc.ekadashi

import android.app.job.JobParameters
import android.app.job.JobService

class RefreshJob : JobService() {

    private var refreshThread: Thread? = null

    override fun onStartJob(params: JobParameters?): Boolean {
        refreshThread = Thread {
            try {
                Repo.refresh(applicationContext)
                Scheduler.scheduleAll(applicationContext)
            } catch (e: Exception) {
                // Ignore background sync failure
            } finally {
                jobFinished(params, false)
            }
        }.apply { start() }

        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        refreshThread?.interrupt()
        return true
    }
}
