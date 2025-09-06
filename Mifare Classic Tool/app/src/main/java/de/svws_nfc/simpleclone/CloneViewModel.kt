package de.svws_nfc.simpleclone

import android.app.Application
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CloneViewModel(app: Application) : AndroidViewModel(app) {

    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    fun startClone() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, CloneService::class.java).apply {
            action = CloneService.ACTION_START
        }
        ContextCompat.startForegroundService(ctx, intent)
        _running.value = true
    }

    fun stopClone() {
        val ctx = getApplication<Application>()
        val intent = Intent(ctx, CloneService::class.java).apply {
            action = CloneService.ACTION_STOP
        }
        ctx.startService(intent)
        _running.value = false
    }
}
