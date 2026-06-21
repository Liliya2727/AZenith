/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.receiver


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zx.azenith.MainActivity
import zx.azenith.R


class ZenithReceiver : BroadcastReceiver() {

    companion object {
        private const val CH_PROFILE = "az_profile"
        private const val CH_SYSTEM = "az_system"
        private const val PROFILE_ID = 1001
        private const val GROUP_SYSTEM_LOGS = "zx.azenith.GROUP_SYSTEM"
        
        const val ACTION_MANAGE = "zx.azenith.ACTION_MANAGE"
        private const val ACTION_RESHOW = "zx.azenith.ACTION_RESHOW"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_MANAGE -> {
                val clearAll = intent.getBooleanExtra("clearall", false) || 
                              intent.getStringExtra("clearall") == "true"
                if (clearAll) {
                    manager.cancelAll()
                }
                intent.getStringExtra("toasttext")?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }
                intent.getStringExtra("notifytext")?.let {
                    handleNotification(context, intent, manager)
                }
            }

            ACTION_RESHOW -> {


                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.Default).launch {
                    delay(3000)
                    val reshow = Intent(context, ZenithReceiver::class.java).apply {
                        this.action = ACTION_MANAGE
                        putExtras(intent.extras ?: return@launch)
                    }
                    context.sendBroadcast(reshow)
                    pendingResult.finish()
                }
            }
        }
    }

    private fun handleNotification(context: Context, intent: Intent, manager: NotificationManager) {
        val title = intent.getStringExtra("notifytitle") ?: "AZenith"
        val message = intent.getStringExtra("notifytext") ?: ""
        
        val chrono = intent.getBooleanExtra("chrono_bool", 
            intent.getStringExtra("chrono") == "true")
        
        val timeout = intent.getStringExtra("timeout")?.toLongOrNull() ?: 0L

        val isProfile = title.lowercase(Locale.ROOT).let {
            it.contains("profile") || it.contains("mode") || it.contains("initializing...")
        }

        val channelId = if (isProfile) CH_PROFILE else CH_SYSTEM


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isProfile) context.getString(R.string.notif_ch_profiles) else context.getString(R.string.notif_ch_system)

            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }


        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }


        val clickIntent = Intent(context, MainActivity::class.java)
        val clickPI = PendingIntent.getActivity(context, 0, clickIntent, pendingIntentFlags)


        val builder = NotificationCompat.Builder(context, channelId).apply {

            setSmallIcon(context.applicationInfo.icon) 
            setContentTitle(title)
            setContentText(message)
            setUsesChronometer(chrono)
            setOngoing(isProfile)
            setAutoCancel(!isProfile)
            setContentIntent(clickPI)
            

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            }

            if (isProfile) {


                val reshowIntent = Intent(context, ZenithReceiver::class.java).apply {
                    this.action = ACTION_RESHOW
                    putExtra("notifytitle", title)
                    putExtra("notifytext", message)
                    putExtra("chrono_bool", chrono)
                }
                val deletePI = PendingIntent.getBroadcast(context, title.hashCode(), reshowIntent, pendingIntentFlags)
                setDeleteIntent(deletePI)
            } else {

                setGroup(GROUP_SYSTEM_LOGS)
            }

            if (timeout > 0L) {
                setTimeoutAfter(timeout)
            }
        }

        manager.notify(if (isProfile) PROFILE_ID else title.hashCode(), builder.build())
    }
}
