# CopyWizard Manual Test Plan

## Preconditions
- Android device with NFC support and MIFARE Classic capability.
- Install a debug build of the app (`assembleDebug`).
- Prepare one **source** MIFARE Classic tag (to read) and one **writable** tag (to clone to).
- Place at least one valid key file in the app's keys directory.

## Test Cases

- [ ] **Step 1 — Read success**
  1. Launch *CopyWizardActivity*.
  2. Tap the **primary** button to start reading.
  3. Present the **source** tag to the device and keep it there.
  4. Verify toast/text shows: `UID : {uid} 카드키가 인식되었습니다. 1단계완료시까지 카드키를 떼지마세요`.
  5. Confirm the UI transitions to **Step 2 (WRITE)**.

- [ ] **Auto-save name pattern**
  1. After Step 1 completes, use a file browser or `adb shell` to navigate to the app's dumps folder.
  2. Confirm a file named like `READ_yyyyMMdd_HHmm_{UID}.mct` exists and contains non-empty dump data.

- [ ] **Step 2 — Write success**
  1. On the Step 2 screen, ensure the **manufacturer block** checkbox is **unchecked** by default.
  2. Present the **target** (writable) tag and hold it until completion.
  3. Verify the message `복사가 완료되었습니다.` appears.
  4. Remove the tag and confirm the data was cloned (e.g., via *ReadTag* activity).

- [ ] **Manufacturer block disabled**
  1. With the checkbox **unchecked**, complete a write.
  2. Verify that block 0 (manufacturer block) on the target tag remains **unchanged**.

- [ ] **Manufacturer block enabled**
  1. Enable the checkbox: `제조사 블록 쓰기(UID 포함) 활성화 — 위험, 법적 준수/승인 환경에서만 사용`.
  2. Present the **target** tag again.
  3. If the tag is a “magic” card, confirm block 0 (including UID) is written successfully.
  4. If the tag is a standard (non-magic) card, verify the error: `이 태그는 UID 쓰기를 지원하지 않습니다(일반 카드).`.

- [ ] **Error handling**
  - **Unsupported device**: Run on a phone known **not** to support MIFARE Classic and verify message: `이 기기는 MIFARE Classic을 지원하지 않습니다`.
  - **Key mapping failure**: Temporarily remove key files, attempt Step 1, and ensure the error lists **unmapped sectors**.
  - **NFC off**: Disable NFC in system settings, reopen the activity, and confirm the **system NFC settings prompt** appears.
  - **Non-magic tag with manufacturer write enabled**: Enable the manufacturer block option, present a standard card, and verify guidance: `이 태그는 UID 쓰기를 지원하지 않습니다(일반 카드).`.
