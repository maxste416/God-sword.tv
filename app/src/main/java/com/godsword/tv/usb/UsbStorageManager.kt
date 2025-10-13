package com.godsword.tv.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import android.util.Log

class UsbStorageManager(
    private val context: Context,
    private val onUsbStateChanged: (Boolean) -> Unit
) {
    
    companion object {
        private const val TAG = "UsbStorageManager"
    }
    
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_MEDIA_MOUNTED -> {
                    Log.d(TAG, "USB Drive Mounted")
                    onUsbStateChanged(true)
                }
                Intent.ACTION_MEDIA_UNMOUNTED,
                Intent.ACTION_MEDIA_REMOVED,
                Intent.ACTION_MEDIA_EJECT -> {
                    Log.d(TAG, "USB Drive Removed")
                    onUsbStateChanged(false)
                }
            }
        }
    }
    
    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_MEDIA_MOUNTED)
            addAction(Intent.ACTION_MEDIA_UNMOUNTED)
            addAction(Intent.ACTION_MEDIA_REMOVED)
            addAction(Intent.ACTION_MEDIA_EJECT)
            addDataScheme("file")
        }
        context.registerReceiver(usbReceiver, filter)
    }
    
    fun unregister() {
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
    
    fun isUsbConnected(): Boolean {
        val scanner = UsbVideoScanner(context)
        return scanner.getUsbDrivePaths().isNotEmpty()
    }
}