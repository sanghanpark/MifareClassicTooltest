package de.svws_nfc.simpleclone

import android.nfc.Tag

/**
 * Placeholder service handling NFC read/write operations.
 * This will be expanded to automate cloning in a later update.
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

