# CopyWizard Manual Test Plan

## Preconditions
- Android device with NFC support and MIFARE Classic capable.
- Install debug build of the app (`assembleDebug`).
- Prepare a writable MIFARE Classic tag and a second tag for cloning.
- Place at least one valid key file in the app's keys directory.

## Test Cases

- [ ] **Step1 Read success**
  1. Launch *CopyWizardActivity*.
  2. Tap the primary button to start reading.
  3. Tap source tag to device.
  4. Verify toast/text shows `UID : {uid} 카드키가 인식되었습니다. 1단계완료시까지 카드키를 떼지마세요`.
  5. Confirm the UI transitions to Step 2.

- [ ] **Auto-save name pattern**
  1. After Step 1 completes, using a file browser or `adb shell`, navigate to the app's internal dumps folder.
  2. Confirm a file named `READ_yyyyMMdd_HHmm_{UID}.mct` exists and contains dump data.

- [ ] **Step2 Write success**
  1. On Step 2 screen, ensure manufacturer block checkbox is **unchecked**.
  2. Tap the target tag and hold it until completion.
  3. Verify message `복사가 완료되었습니다.` appears.
  4. Remove tag and confirm data cloned (e.g., via ReadTag activity).

- [ ] **Manufacturer block disabled**
  1. With checkbox unchecked, ensure block 0 of target tag remains unchanged after writing.

- [ ] **Manufacturer block enabled**
  1. Enable checkbox `제조사 블록 쓰기(UID 포함) 활성화 — 위험, 법적 준수/승인 환경에서만 사용`.
  2. Tap target tag again.
  3. Confirm block 0 writes only if tag is magic; otherwise error `이 태그는 UID 쓰기를 지원하지 않습니다(일반 카드).` is shown.

- [ ] **Error handling**
  - **Unsupported device**: Run on a phone known to lack MIFARE Classic support and verify message `이 기기는 MIFARE Classic을 지원하지 않습니다`.
  - **Key mapping failure**: Remove key files, attempt Step 1, and ensure error lists unmapped sectors.
  - **NFC off**: Disable NFC in system settings, reopen activity, and confirm quick-settings prompt appears.
  - **Non‑magic tag**: Enable manufacturer block option, present a standard card, and verify guidance `이 태그는 UID 쓰기를 지원하지 않습니다(일반 카드).`.

