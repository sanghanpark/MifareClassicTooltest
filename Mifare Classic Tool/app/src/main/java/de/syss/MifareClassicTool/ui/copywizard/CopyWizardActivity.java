package de.syss.MifareClassicTool.ui.copywizard;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;
import de.syss.MifareClassicTool.domain.CopyWizardCoordinator;

public class CopyWizardActivity extends AppCompatActivity {
    public static final int STEP_READ = 1;
    public static final int STEP_WRITE = 2;

    private int mState = STEP_READ;

    private TextView mTopMessage;
    private TextView mSubMessage;
    private Button mPrimaryButton;
    private Button mSecondaryButton;
    private CheckBox mBlock0CheckBox;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private File mDumpFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy_wizard);

        mTopMessage = findViewById(R.id.top_message);
        mSubMessage = findViewById(R.id.sub_message);
        mPrimaryButton = findViewById(R.id.primary_button);
        mSecondaryButton = findViewById(R.id.secondary_button);
        mBlock0CheckBox = findViewById(R.id.checkbox_block0);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = PendingIntent.FLAG_MUTABLE;
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        mPrimaryButton.setOnClickListener(v -> {
            if (mState == STEP_READ) {
                // TODO: trigger read process
            } else if (mState == STEP_WRITE) {
                // TODO: trigger write process
            }
        });

        mSecondaryButton.setOnClickListener(v -> {
            if (mState == STEP_WRITE) {
                mState = STEP_READ;
                updateUi();
            }
        });

        updateUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mNfcAdapter != null) {
            if (!mNfcAdapter.isEnabled()) {
                Toast.makeText(this, R.string.copy_wizard_nfc_off, Toast.LENGTH_LONG).show();
                startActivity(new Intent(Settings.ACTION_NFC_SETTINGS));
            }
            String[][] techList = new String[][] { new String[] { MifareClassic.class.getName() } };
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, null, techList);
        }
    }

    @Override
    protected void onPause() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
        super.onPause();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            handleTag(tag);
        }
    }

    private void handleTag(Tag tag) {
        if (mState == STEP_READ) {
            String uid = Common.bytes2Hex(tag.getId());
            mTopMessage.setText(getString(R.string.copy_wizard_uid_recognized, uid));
            new Thread(() -> {
                CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
                File file = coordinator.readAndSaveDump(this, tag);
                String err = coordinator.getLastError();
                if (file != null) {
                    mDumpFile = file;
                    runOnUiThread(() -> {
                        mState = STEP_WRITE;
                        updateUi();
                    });
                } else if (err != null) {
                    runOnUiThread(() ->
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show());
                }
            }).start();
        } else if (mState == STEP_WRITE && mDumpFile != null) {
            if (!mBlock0CheckBox.isChecked()) {
                Toast.makeText(this, R.string.copy_wizard_block0_consent_toast, Toast.LENGTH_LONG).show();
                return;
            }
            mTopMessage.setText(R.string.copy_wizard_write_detected);
            new Thread(() -> {
                CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
                boolean success = coordinator.writeClone(this, tag, mDumpFile, mBlock0CheckBox.isChecked());
                String err = coordinator.getLastError();
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, R.string.copy_wizard_clone_finished, Toast.LENGTH_LONG).show();
                    } else if (err != null) {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                    mState = STEP_READ;
                    updateUi();
                });
            }).start();
        }
    }

    private void updateUi() {
        if (mState == STEP_READ) {
            mTopMessage.setText(R.string.copy_wizard_read_top);
            mSubMessage.setText(R.string.copy_wizard_read_sub);
            mPrimaryButton.setText(R.string.copy_wizard_read_button);
            mPrimaryButton.setEnabled(true);
            mSecondaryButton.setText(R.string.copy_wizard_write_button);
            mSecondaryButton.setEnabled(false);
            mBlock0CheckBox.setVisibility(View.GONE);
            mBlock0CheckBox.setChecked(false);
        } else if (mState == STEP_WRITE) {
            mTopMessage.setText(R.string.copy_wizard_write_top);
            mSubMessage.setText(R.string.copy_wizard_write_sub);
            mPrimaryButton.setText(R.string.copy_wizard_read_button);
            mPrimaryButton.setEnabled(false);
            mSecondaryButton.setText(R.string.copy_wizard_write_button);
            mSecondaryButton.setEnabled(true);
             mBlock0CheckBox.setVisibility(View.VISIBLE);
             mBlock0CheckBox.setChecked(false);
        }
    }
}

