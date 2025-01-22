// TimeSyncWorker.kt
import android.content.Context
import com.example.timemacro.SntpClient
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

class TimeSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        private const val NTP_SERVER = "pool.ntp.org"
        private const val TIMEOUT_MS = 10_000L
        private const val PREF_NAME = "TimePrefs"
        private const val OFFSET_KEY = "time_offset"

        fun schedulePeriodicSync(context: Context) {
            val request = PeriodicWorkRequestBuilder<TimeSyncWorker>(1, TimeUnit.HOURS)
                .addTag("ntp_sync")
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }

        fun getTimeOffset(context: Context): Long {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getLong(OFFSET_KEY, 0L)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val offset = syncWithNtp()
            saveOffset(offset)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun syncWithNtp(): Long {
        return try {
            val sntpClient = SntpClient()
            if (sntpClient.requestTime(NTP_SERVER, TIMEOUT_MS.toInt())) {
                val ntpTime = sntpClient.currentTimeMillis()
                val systemTime = System.currentTimeMillis()
                ntpTime - systemTime
            } else {
                throw Exception("NTP sync failed")
            }
        } catch (e: Exception) {
            delay(5000)
            syncWithNtp() // 재시도
        }
    }

    private fun saveOffset(offset: Long) {
        applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(OFFSET_KEY, offset)
            .apply()
    }
}