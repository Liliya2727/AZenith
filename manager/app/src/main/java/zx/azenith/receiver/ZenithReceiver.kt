/*
 * Copyright (C) 2026-2027 Zexshia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zx.azenith.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.util.Locale

class ZenithReceiver : BroadcastReceiver() {

    companion object {
        private const val CH_PROFILE = "az_profile"
        private const val CH_SYSTEM = "az_system"
        private const val PROFILE_ID = 1001

        // Actions
        const val ACTION_MANAGE = "zx.azenith.ACTION_MANAGE"
        private const val ACTION_RESHOW = "zx.azenith.ACTION_RESHOW"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            ACTION_MANAGE -> {
                // 1. Clear All
                val clearAll = intent.getBooleanExtra("clearall", false) || 
                              intent.getStringExtra("clearall") == "true"
                if (clearAll) {
                    manager.cancelAll()
                }

                // 2. Toast
                intent.getStringExtra("toasttext")?.let {
                    Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                }

                // 3. Notification
                intent.getStringExtra("notifytext")?.let {
                    handleNotification(context, intent, manager)
                }
            }

            ACTION_RESHOW -> {
                // Logika Auto-Reshow (Tiru MyReceiver lama)
                Handler(Looper.getMainLooper()).postDelayed({
                    val reshow = Intent(context, ZenithReceiver::class.java).apply {
                        this.action = ACTION_MANAGE
                        putExtras(intent.extras ?: return@apply)
                    }
                    context.sendBroadcast(reshow)
                }, 3000)
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

        // Create Channel (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = if (isProfile) "AZenith Profiles" else "AZenith System"
            val channel = NotificationChannel(channelId, name, NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }

        builder.apply {
            setSmallIcon(context.applicationInfo.icon)
            setContentTitle(title)
            setContentText(message)
            setUsesChronometer(chrono)
            setOngoing(isProfile) // Profile notif biasanya persisten
            setAutoCancel(!isProfile)

            // Jika Profile, pasang DeleteIntent untuk trigger ACTION_RESHOW
            if (isProfile) {
                val reshowIntent = Intent(context, ZenithReceiver::class.java).apply {
                    this.action = ACTION_RESHOW
                    putExtra("notifytitle", title)
                    putExtra("notifytext", message)
                    putExtra("chrono_bool", chrono)
                }

                var flags = PendingIntent.FLAG_UPDATE_CURRENT
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    flags = flags or PendingIntent.FLAG_IMMUTABLE
                }

                val deletePI = PendingIntent.getBroadcast(context, title.hashCode(), reshowIntent, flags)
                setDeleteIntent(deletePI)
            }

            if (timeout > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setTimeoutAfter(timeout)
            }
        }

        manager.notify(if (isProfile) PROFILE_ID else title.hashCode(), builder.build())
    }
}
