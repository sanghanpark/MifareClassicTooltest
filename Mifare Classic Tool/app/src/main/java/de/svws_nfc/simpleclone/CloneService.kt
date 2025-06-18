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

import android.nfc.Tag

/**
 * Service 코드. READ → 덤프 생성, WRITE → 제조사 블록 쓰기까지 자동 실행.
 * Currently a placeholder for future NFC automation.
 * When implemented, it will read a tag, store the dump,
 * then write it back during the write phase.
 */
class CloneService {
    enum class Phase { READ, WRITE }

    fun startRead(tag: Tag) {
        // TODO implement NFC read logic
    }

    fun startWrite(tag: Tag) {
        // TODO implement NFC write logic
    }
}
