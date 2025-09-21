package de.syss.MifareClassicTool.ui.copywizard;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
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

    private enum AutoState { IDLE, READING, WAITING_MODUKEY, WRITING, DONE }

    private boolean autoMode = false;
    private AutoState autoState = AutoState.IDLE;
    private String lastSavedDumpPath = null;
    private boolean hasSourceTag = false;
    private boolean hasTargetTag = false;
    private boolean isReadingInProgress = false;
    private boolean isWritingInProgress = false;
    private Animation blinkAnimation;

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

        Intent launchIntent = getIntent();
        boolean launchAutoMode = shouldEnterAutoMode(launchIntent);
        if (launchIntent != null && launchIntent.hasExtra(EXTRA_AUTO_MODE)) {
            launchAutoMode = launchIntent.getBooleanExtra(EXTRA_AUTO_MODE, launchAutoMode);
        }
        updateAutoMode(launchAutoMode);

        mTopMessage = findViewById(R.id.top_message);
        mSubMessage = findViewById(R.id.sub_message);
        mPrimaryButton = findViewById(R.id.primary_button);
        mSecondaryButton = findViewById(R.id.secondary_button);
        mBlock0CheckBox = findViewById(R.id.checkbox_block0);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        int flags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? (PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT)
                : PendingIntent.FLAG_UPDATE_CURRENT;
        mPendingIntent = PendingIntent.getActivity(this, 0, intent, flags);

        mPrimaryButton.setOnClickListener(v -> {
            if (mState == STEP_READ && hasSourceTag && !isReadingInProgress) {
                selectAllKeyFiles();
                startMappingAndReadTag();
            }
        });

        mSecondaryButton.setOnClickListener(v -> {
            if (mState == STEP_WRITE && hasTargetTag && !isWritingInProgress) {
                writeDumpClone();
            }
        });

        updateUi(true);
        handleIntent(getIntent());
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
        if (intent == null) {
            return;
        }
        setIntent(intent);

        boolean nextAutoMode = shouldEnterAutoMode(intent);
        if (intent.hasExtra(EXTRA_AUTO_MODE)) {
            nextAutoMode = intent.getBooleanExtra(EXTRA_AUTO_MODE, nextAutoMode);
        }
        updateAutoMode(nextAutoMode);

        handleIntent(intent);
    }

    private void handleTag(Tag tag) {
        if (tag == null) {
            return;
        }
        if (mState == STEP_READ) {
            if (isReadingInProgress) {
                return;
            }
            currentTag = tag;
            hasSourceTag = true;
            String uid = Common.bytes2Hex(tag.getId());
            mTopMessage.setText(getString(R.string.mct_uid_detected_step1, uid));
            setSubMessage("", false);
            updateUi(false);
        } else if (mState == STEP_WRITE && mDumpFile != null) {
            if (isWritingInProgress) {
                return;
            }
            currentTag = tag;
            hasTargetTag = true;
            mTopMessage.setText(R.string.mct_detected_step2);
            setSubMessage("", false);
            updateUi(false);
        }
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            return;
        }

        final Tag tag;
        if (Build.VERSION.SDK_INT >= 33) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag.class);
        } else {
            //noinspection deprecation
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        }

        if (tag == null) {
            return;
        }

        if (autoMode) {
            onTagDiscoveredAuto(tag);
        } else {
            handleTag(tag);
        }
    }

    private boolean shouldEnterAutoMode(Intent intent) {
        if (intent == null) {
            return false;
        }
        if (intent.getBooleanExtra(EXTRA_AUTO_MODE, false)) {
            return true;
        }
        String action = intent.getAction();
        return NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action);
    }

    private void updateAutoMode(boolean enableAutoMode) {
        if (enableAutoMode && (!autoMode || autoState == AutoState.DONE)) {
            autoState = AutoState.IDLE;
        }
        autoMode = enableAutoMode;
    }

    private void selectAllKeyFiles() {
        // Selection is handled internally by the coordinator, keep placeholder for future use.
    }

    private void startMappingAndReadTag() {
        if (currentTag == null || isReadingInProgress) {
            return;
        }
        final Tag tag = currentTag;
        isReadingInProgress = true;
        setSubMessage(getString(R.string.mct_copy_step1_running), true);
        new Thread(() -> {
            CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
            File file = coordinator.readAndSaveDump(this, tag);
            String err = coordinator.getLastError();
            runOnUiThread(() -> {
                isReadingInProgress = false;
                setSubMessage("", false);
                if (file != null) {
                    mDumpFile = file;
                    lastSavedDumpPath = file.getAbsolutePath();
                    currentTag = null;
                    hasSourceTag = false;
                    mState = STEP_WRITE;
                    hasTargetTag = false;
                    updateUi(true);
                    if (autoMode) {
                        autoState = AutoState.WAITING_MODUKEY;
                    }
                } else {
                    hasSourceTag = true;
                    if (err != null) {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                    if (autoMode) {
                        autoState = AutoState.IDLE;
                    }
                    updateUi(false);
                }
            });
        }).start();
    }

    private void enableWriteManufacturerBlock() {
        mBlock0CheckBox.setChecked(true);
    }

    private void selectDumpFile(String absolutePath) {
        mDumpFile = new File(absolutePath);
    }

    private void writeDumpClone() {
        if (currentTag == null || mDumpFile == null || isWritingInProgress) {
            return;
        }
        final Tag tag = currentTag;
        enableWriteManufacturerBlock();
        isWritingInProgress = true;
        setSubMessage(getString(R.string.mct_copy_step2_running), true);
        new Thread(() -> {
            CopyWizardCoordinator coordinator = new CopyWizardCoordinator();
            boolean success = coordinator.writeClone(this, tag, mDumpFile, mBlock0CheckBox.isChecked());
            String err = coordinator.getLastError();
            runOnUiThread(() -> {
                isWritingInProgress = false;
                currentTag = null;
                hasTargetTag = false;
                setSubMessage("", false);
                if (success) {
                    mTopMessage.setText(R.string.mct_copy_done);
                    Toast.makeText(this, R.string.mct_copy_done, Toast.LENGTH_LONG).show();
                    mState = STEP_READ;
                    hasSourceTag = false;
                    mDumpFile = null;
                    if (autoMode) {
                        autoState = AutoState.DONE;
                    }
                    updateUi(false);
                } else {
                    if (err != null) {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    }
                    mTopMessage.setText(R.string.mct_prompt_present_modukey);
                    if (autoMode) {
                        autoState = AutoState.WAITING_MODUKEY;
                    }
                    updateUi(false);
                }
            });
        }).start();
    }

    private void setSubMessage(CharSequence text, boolean blink) {
        if (text == null || text.length() == 0) {
            mSubMessage.setText("");
            mSubMessage.setVisibility(View.GONE);
            stopBlinking();
        } else {
            mSubMessage.setText(text);
            mSubMessage.setVisibility(View.VISIBLE);
            if (blink) {
                startBlinking();
            } else {
                stopBlinking();
            }
        }
    }

    private void startBlinking() {
        if (blinkAnimation == null) {
            blinkAnimation = new AlphaAnimation(1.0f, 0.2f);
            blinkAnimation.setDuration(500);
            blinkAnimation.setRepeatMode(Animation.REVERSE);
            blinkAnimation.setRepeatCount(Animation.INFINITE);
        }
        mSubMessage.startAnimation(blinkAnimation);
    }

    private void stopBlinking() {
        if (blinkAnimation != null) {
            mSubMessage.clearAnimation();
        }
    }

    private void onTagDiscoveredAuto(Tag tag) {
        if (tag == null) {
            return;
        }
        if (isReadingInProgress || isWritingInProgress) {
            return;
        }
        currentTag = tag;
        switch (autoState) {
            case IDLE: {
                hasSourceTag = true;
                String uid = Common.bytes2Hex(tag.getId());
                mTopMessage.setText(getString(R.string.mct_uid_detected_step1, uid));
                setSubMessage("", false);
                updateUi(false);
                selectAllKeyFiles();
                startMappingAndReadTag();
                autoState = AutoState.READING;
                break;
            }
            case WAITING_MODUKEY: {
                hasTargetTag = true;
                mTopMessage.setText(R.string.mct_detected_step2);
                setSubMessage("", false);
                updateUi(false);
                if (lastSavedDumpPath != null) {
                    selectDumpFile(lastSavedDumpPath);
                }
                writeDumpClone();
                autoState = AutoState.WRITING;
                break;
            }
            default:
                // Ignore other states
                break;
        }
    }

    private void updateUi(boolean resetMessages) {
        if (mState == STEP_READ) {
            mPrimaryButton.setVisibility(View.VISIBLE);
            mPrimaryButton.setText(R.string.mct_copy_start);
            mPrimaryButton.setEnabled(hasSourceTag && !isReadingInProgress);
            mSecondaryButton.setVisibility(View.GONE);
            mSecondaryButton.setEnabled(false);
            if (resetMessages) {
                mTopMessage.setText(R.string.mct_prompt_present_original);
                setSubMessage("", false);
            }
        } else if (mState == STEP_WRITE) {
            mPrimaryButton.setVisibility(View.GONE);
            mPrimaryButton.setEnabled(false);
            mSecondaryButton.setVisibility(View.VISIBLE);
            mSecondaryButton.setText(R.string.mct_copy_step2_start);
            mSecondaryButton.setEnabled(hasTargetTag && !isWritingInProgress && mDumpFile != null);
            if (resetMessages) {
                mTopMessage.setText(R.string.mct_prompt_present_modukey);
                setSubMessage("", false);
            }
        }
        mBlock0CheckBox.setVisibility(View.GONE);
        mBlock0CheckBox.setChecked(true);
    }
}
