package de.svws_nfc.simpleclone

import android.nfc.Tag
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class UiState(val statusText: String = "NFC 태그를 기기 뒷면에 갖다 대세요.")

class CloneViewModel : ViewModel() {

    private val _uiState = MutableLiveData(UiState())
    fun getUiState(): LiveData<UiState> = _uiState

    fun onTagScanned(tag: Tag) {
        val uid = tag.id?.joinToString("") { b -> "%02X".format(b) } ?: "unknown"
        _uiState.postValue(UiState(statusText = "Tag UID: $uid 인식됨"))
        // TODO: Hook up CloneService if/when it exists.
    }
}
