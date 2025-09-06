package de.svws_nfc.simpleclone;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import de.syss.MifareClassicTool.R;

public class SimpleCloneActivity extends AppCompatActivity {

    private CloneViewModel viewModel;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_clone);

        tvStatus = findViewById(R.id.tvStatus);
        viewModel = new ViewModelProvider(this).get(CloneViewModel.class);
        viewModel.getUiState().observe(this, state -> {
            if (state != null) {
                tvStatus.setText(state.getStatusText());
            }
        });

        handleIntent(getIntent());
    }

    // Must be public so Android can call it
    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;

        Tag tag;
        if (Build.VERSION.SDK_INT >= 33) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);
        } else {
            //noinspection deprecation
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }

        if (tag != null) {
            viewModel.onTagScanned(tag);
        }
    }
}
