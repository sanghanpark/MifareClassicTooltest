package de.svws_nfc.simpleclone;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import de.syss.MifareClassicTool.BasicActivity;
import de.syss.MifareClassicTool.R;

public class SimpleCloneActivity extends BasicActivity {

    private CloneViewModel viewModel;
    private TextView txtStatus;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_clone);

        txtStatus = findViewById(R.id.txtStatus);
        viewModel = new ViewModelProvider(this).get(CloneViewModel.class);

        viewModel.getUiState().observe(this, state -> {
            if (state != null) {
                txtStatus.setText(state.getStatusText());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // Must be public to match BasicActivity signature
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) return;

        // For API 33+ you can use: getParcelableExtra(String, Class)
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            viewModel.onTagScanned(tag);
        }
    }
}
