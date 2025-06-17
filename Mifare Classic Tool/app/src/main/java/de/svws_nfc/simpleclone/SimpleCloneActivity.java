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

package de.svws_nfc.simpleclone;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import androidx.lifecycle.ViewModelProvider;

import de.syss.MifareClassicTool.Activities.BasicActivity;

/**
 * 단순 2-단계 카드 복제를 위한 전용 화면.
 * READ 단계 → WRITE 단계로만 흐르며, 나머지 세부 옵션은 자동 처리된다.
 */
public class SimpleCloneActivity extends BasicActivity {
    private CloneViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_clone);
        viewModel = new ViewModelProvider(this).get(CloneViewModel.class);
        viewModel.service = new CloneService();

        viewModel.getUiState().observe(this, state -> {
            // TODO: 단계별 메시지/버튼 상태 업데이트
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) viewModel.onTagScanned(tag);
    }
}
