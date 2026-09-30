package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Boot receiver to restart OnChat background service after phone reboot or app update
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d("BootReceiver", "Starting OnChatBackgroundService after $action")
            try {
                OnChatBackgroundService.start(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed to start service on boot", e)
            }
        }
    }
}
