package de.syss.MifareClassicTool.ui.copywizard;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.R;
import de.syss.MifareClassicTool.domain.CopyWizardCoordinator;

public class CopyWizardActivity extends AppCompatActivity {
    public static final int STEP_READ = 1;
    public static final int STEP_WRITE = 2;
    public static final String EXTRA_AUTO_MODE = "extra.AUTO_MODE";

    private boolean autoMode = false;
    private enum AutoState { IDLE, READING, SAVING, WAITING_MODUKEY, WRITING, DONE }
    private AutoState autoState = AutoState.IDLE;
    private String lastSavedDumpPath = null;

    private int mState = STEP_READ;

    private TextView mTopMessage;
    private TextView mSubMessage;
    private Button mPrimaryButton;
    private Button mSecondaryButton;
    private CheckBox mBlock0CheckBox;

    private NfcAdapter mNfcAdapter;
    private PendingIntent mPendingIntent;
    private File mDumpFile;
    private Tag currentTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_copy_wizard);
        autoMode = getIntent().getBooleanExtra(EXTRA_AUTO_MODE, false);

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
                selectAllKeyFiles();
                startMappingAndReadTag();
            } else if (mState == STEP_WRITE) {
                if (!mBlock0CheckBox.isChecked()) {
                    Toast.makeText(this, R.string.copy_wizard_block0_consent_toast, Toast.LENGTH_LONG).show();
                    return;
                }
                writeDumpClone();
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
            if (autoMode) {
                onTagDiscoveredAuto(tag);
            } else {
                handleTag(tag);
            }
        }
    }

    private void handleTag(Tag tag) {
        currentTag = tag;
        if (mState == STEP_READ) {
            String uid = Common.bytes2Hex(tag.getId());
            mTopMessage.setText(getString(R.string.copy_wizard_uid_recognized, uid));
            startMappingAndReadTag();
        } else if (mState == STEP_WRITE && mDumpFile != null) {
            if (!mBlock0CheckBox.isChecked()) {
                Toast.makeText(this, R.string.copy_wizard_block0_consent_toast, Toast.LENGTH_LONG).show();
                return;
            }
            mTopMessage.setText(R.string.copy_wizard_write_detected);
            writeDumpClone();
        }
    }

    private void selectAllKeyFiles() {
        // existing selection logic handled internally by coordinator
    }

    private void startMappingAndReadTag() {
        if (currentTag == null) return;
        new Thread(() -> {
            CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
            File file = coordinator.readAndSaveDump(this, currentTag);
            String err = coordinator.getLastError();
            if (file != null) {
                mDumpFile = file;
                runOnUiThread(() -> {
                    mState = STEP_WRITE;
                    updateUi();
                    if (autoMode) {
                        lastSavedDumpPath = file.getAbsolutePath();
                        showStatus(getString(R.string.mct_copy_step1_running));
                        autoState = AutoState.WAITING_MODUKEY;
                        showStatus(getString(R.string.mct_prompt_present_modukey));
                    }
                });
            } else if (err != null) {
                runOnUiThread(() -> Toast.makeText(this, err, Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private String autoSaveDump(String suggestedName, byte[] dump) {
        File outFile = Common.getFile(Common.DUMPS_DIR + "/" + suggestedName);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        // Dump saving handled elsewhere; return path
        return outFile.getAbsolutePath();
    }

    private void enableWriteManufacturerBlock() {
        mBlock0CheckBox.setVisibility(View.VISIBLE);
        mBlock0CheckBox.setChecked(true);
    }

    private void selectDumpFile(String absolutePath) {
        mDumpFile = new File(absolutePath);
    }

    private void writeDumpClone() {
        if (currentTag == null || mDumpFile == null) return;
        new Thread(() -> {
            CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
            boolean success = coordinator.writeClone(this, currentTag, mDumpFile, mBlock0CheckBox.isChecked());
            String err = coordinator.getLastError();
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, R.string.copy_wizard_clone_finished, Toast.LENGTH_LONG).show();
                    if (autoMode) {
                        showStatus(getString(R.string.mct_copy_done));
                        autoState = AutoState.DONE;
                    }
                } else if (err != null) {
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                }
                mState = STEP_READ;
                updateUi();
            });
        }).start();
    }

    private void showStatus(String message) {
        runOnUiThread(() -> mTopMessage.setText(message));
    }

    private void onTagDiscoveredAuto(Tag tag) {
        currentTag = tag;
        switch (autoState) {
            case IDLE:
                showStatus(getString(R.string.mct_uid_detected_step1, toUid(tag)));
                selectAllKeyFiles();
                startMappingAndReadTag();
                autoState = AutoState.READING;
                break;
            case WAITING_MODUKEY:
                showStatus(getString(R.string.mct_detected_step2));
                enableWriteManufacturerBlock();
                if (lastSavedDumpPath != null) {
                    selectDumpFile(lastSavedDumpPath);
                }
                writeDumpClone();
                autoState = AutoState.WRITING;
                break;
            default:
                break;
        }
    }

    private String toUid(Tag tag) {
        byte[] id = tag.getId();
        if (id == null) return "unknown";
        StringBuilder sb = new StringBuilder();
        for (byte b : id) sb.append(String.format("%02X", b));
        return sb.toString();
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
