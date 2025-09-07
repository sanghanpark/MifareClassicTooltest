package de.svws_nfc.simpleclone

import android.app.Application
import android.content.Intent
import android.nfc.Tag
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

data class UiState(val statusText: String = "NFC 태그를 기기 뒷면에 갖다 대세요.")

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    // Backing state
    private val _uiState = MutableLiveData(UiState())

    // Distinct property name (avoid JVM getter collision with getUiState())
    val uiStateLiveData: LiveData<UiState> get() = _uiState

    // Java-friendly accessor used by SimpleCloneActivity
    fun getUiState(): LiveData<UiState> = _uiState

    fun onTagScanned(tag: Tag) {
        val uid = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: "unknown"
        _uiState.postValue(
            UiState(statusText = "UID : $uid 카드키가 인식되었습니다. 1단계완료시까지 카드키를 떼지마세요")
        )
    }

    fun startClone() {
        val ctx = getApplication<Application>()
        try {
            val svcClass = Class.forName("de.svws_nfc.simpleclone.CloneService")
            val intent = Intent(ctx, svcClass).apply {
                action = "de.svws_nfc.simpleclone.action.START"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
            _uiState.postValue(UiState(statusText = "복제를 시작했습니다. 태그를 대세요."))
        } catch (_: ClassNotFoundException) {
            _uiState.postValue(UiState(statusText = "CloneService가 없습니다. 나중에 추가해 주세요."))
        }
    }

    fun stopClone() {
        val ctx = getApplication<Application>()
        try {
            val svcClass = Class.forName("de.svws_nfc.simpleclone.CloneService")
            val intent = Intent(ctx, svcClass).apply {
                action = "de.svws_nfc.simpleclone.action.STOP"
            }
            ctx.startService(intent)
            _uiState.postValue(UiState(statusText = "복제를 중지했습니다."))
        } catch (_: ClassNotFoundException) {
            _uiState.postValue(UiState(statusText = "CloneService가 없어 중지 요청을 보낼 수 없습니다."))
        }
    }
}
