package de.svws_nfc.simpleclone

import android.app.Application
import android.content.Intent
import android.nfc.Tag
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UiState(val statusText: String = "NFC 태그를 기기 뒷면에 갖다 대세요.")

class CloneViewModel(application: Application) : AndroidViewModel(application) {

    // 기존 Java 코드 호환: LiveData로 UI 상태 노출
    private val _uiState = MutableLiveData(UiState())
    val uiState: LiveData<UiState> get() = _uiState
    // Java에서 getUiState()로 호출하므로 메서드도 유지
    fun getUiState(): LiveData<UiState> = _uiState

    // 진행 상태 (필요 시 Compose/코루틴에서 사용)
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running

    fun onTagScanned(tag: Tag) {
        val uid = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: "unknown"
        _uiState.postValue(UiState(statusText = "Tag UID: $uid 인식됨"))
    }

    fun startClone() {
        val ctx = getApplication<Application>()
        try {
            // CloneService가 없어도 컴파일되도록 리플렉션 사용
            val svcClass = Class.forName("de.svws_nfc.simpleclone.CloneService")
            val intent = Intent(ctx, svcClass).apply {
                action = "de.svws_nfc.simpleclone.action.START"
            }
            ContextCompat.startForegroundService(ctx, intent)
            _running.value = true
            _uiState.postValue(UiState(statusText = "복제를 시작했습니다. 태그를 대세요."))
        } catch (e: ClassNotFoundException) {
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
            _running.value = false
            _uiState.postValue(UiState(statusText = "복제를 중지했습니다."))
        } catch (e: ClassNotFoundException) {
            _uiState.postValue(UiState(statusText = "CloneService가 없어 중지 요청을 보낼 수 없습니다."))
        }
    }
}
