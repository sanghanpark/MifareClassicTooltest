/*
 * Copyright 2025 Gerhard Klostermeier
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.svws_nfc.simpleclone

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData

class CloneViewModel(app: Application) : AndroidViewModel(app) {

    enum class Phase { WAIT_READ, READ_RUNNING, WAIT_WRITE, WRITE_RUNNING, DONE, ERROR }

    data class UiState(
        val phase: Phase = Phase.WAIT_READ,
        val message: String = "",
        val progress: Int = 0
    )

    val uiState = MutableLiveData(UiState())

    var service: CloneService? = null
    fun onTagScanned(tag: Tag) {
        when (uiState.value?.phase) {
            Phase.WAIT_READ -> {
                service?.startRead(tag)
            }
            Phase.WAIT_WRITE -> {
                service?.startWrite(tag)
            }
            else -> {} // DONE, ERROR 일 땐 무시
        }
    }

    /** Service 콜백이 호출할 메서드 */
    fun update(phase: CloneService.Phase, msg: String) {
        when (phase) {
            CloneService.Phase.READ  ->
                uiState.postValue(uiState.value?.copy(phase = Phase.READ_RUNNING, message = msg))
            CloneService.Phase.WRITE ->
                uiState.postValue(uiState.value?.copy(phase = Phase.WRITE_RUNNING, message = msg))
        }
    }

    // ...추가 로직
}
