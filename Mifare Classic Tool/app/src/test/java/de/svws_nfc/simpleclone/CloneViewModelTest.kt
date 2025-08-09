package de.svws_nfc.simpleclone

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Test

/*
 * Simple unit test ensuring the ViewModel starts in WAIT_READ phase.
 */
class CloneViewModelTest {

    @Test
    fun defaultStateIsWaitRead() {
        val vm = CloneViewModel(Application())
        assertEquals(CloneViewModel.Phase.WAIT_READ, vm.uiState.value?.phase)
    }
}
