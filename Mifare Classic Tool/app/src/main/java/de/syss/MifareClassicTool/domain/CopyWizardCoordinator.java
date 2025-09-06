package de.syss.MifareClassicTool.domain;

import android.content.Context;
import android.nfc.Tag;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import de.syss.MifareClassicTool.BuildConfig;
import de.syss.MifareClassicTool.Common;
import de.syss.MifareClassicTool.MCReader;
import de.syss.MifareClassicTool.R;

/**
 * Helper to read a tag using all available key files and save the dump.
 */
public class CopyWizardCoordinator {

    private static final String TAG = "CopyWizardCoord";

    private String mLastError;

    public String getLastError() {
        return mLastError;
    }

    /**
     * Read the given tag and save the dump to internal storage.
     * @param context Context for file and Toast operations.
     * @param tag The tag to read.
     * @return The created dump file or null on error.
     */
    public File readAndSaveDump(Context context, Tag tag) {
        mLastError = null;
        if (BuildConfig.ENABLE_LOG) {
            Log.d(TAG, "readAndSaveDump invoked");
        }
        MCReader reader = MCReader.get(tag);
        if (reader == null) {
            mLastError = context.getString(R.string.copy_wizard_error_no_mfc_device);
            return null;
        }
        File keyDir = Common.getFile(Common.KEYS_DIR);
        File[] keyFiles = keyDir != null ? keyDir.listFiles(File::isFile) : null;
        if (keyFiles == null || reader.setKeyFile(keyFiles, context) <= 0) {
            int sc = reader.getSectorCount();
            reader.close();
            ArrayList<Integer> all = new ArrayList<>();
            for (int i = 0; i < sc; i++) {
                all.add(i);
            }
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", all));
            return null;
        }
        int sectorCount = reader.getSectorCount();
        reader.setMappingRange(0, sectorCount - 1);
        int status = 0;
        while (status != -1 && status < sectorCount - 1) {
            status = reader.buildNextKeyMapPart();
        }
        if (status == -1) {
            reader.close();
            ArrayList<Integer> all = new ArrayList<>();
            for (int i = 0; i < sectorCount; i++) {
                all.add(i);
            }
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", all));
            return null;
        }
        SparseArray<byte[][]> keyMap = reader.getKeyMap();
        ArrayList<Integer> missing = new ArrayList<>();
        for (int i = 0; i < sectorCount; i++) {
            if (keyMap.get(i) == null) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            reader.close();
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", missing));
            return null;
        }
        SparseArray<String[]> raw = reader.readAsMuchAsPossible(keyMap);
        reader.close();
        if (raw == null) {
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", missing));
            return null;
        }
        String uid = Common.bytes2Hex(tag.getId());
        String ts = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String name = "READ_" + ts + "_" + uid + ".mct";
        File outFile = Common.getFile(Common.DUMPS_DIR + "/" + name);
        if (outFile.getParentFile() != null) {
            outFile.getParentFile().mkdirs();
        }
        String[] dump = buildDump(raw, sectorCount);
        if (dump == null) {
            return null;
        }
        if (Common.saveFile(outFile, dump, false)) {
            return outFile;
        }
        mLastError = context.getString(R.string.copy_wizard_error_key_map,
                TextUtils.join(", ", missing));
        return null;
    }

    /**
     * Write the given dump to the target tag.
     * @param context Context for file operations.
     * @param tag Target tag.
     * @param dumpFile Dump previously produced by {@link #readAndSaveDump}.
     * @param enableManufacturerBlock If true, also try to write block 0.
     * @return True on success.
     */
    public boolean writeClone(Context context, Tag tag, File dumpFile,
                               boolean enableManufacturerBlock) {
        mLastError = null;
        if (BuildConfig.ENABLE_LOG) {
            Log.d(TAG, "writeClone invoked");
        }
        String[] dump = Common.readFileLineByLine(dumpFile, false, context);
        if (dump == null || Common.isValidDump(dump, false) != 0) {
            mLastError = context.getString(R.string.copy_wizard_error_key_map, "?");
            return false;
        }
        HashMap<Integer, HashMap<Integer, byte[]>> dumpWithPos =
                parseDump(dump);
        if (!enableManufacturerBlock) {
            if (dumpWithPos.containsKey(0)) {
                dumpWithPos.get(0).remove(0);
                if (dumpWithPos.get(0).isEmpty()) {
                    dumpWithPos.remove(0);
                }
            }
        } else {
            HashMap<Integer, byte[]> s0 = dumpWithPos.get(0);
            if (s0 != null && s0.containsKey(0)) {
                String block0 = Common.bytes2Hex(s0.get(0));
                if (!checkBlock0(tag, block0)) {
                    mLastError = context.getString(R.string.copy_wizard_error_tag_not_magic);
                    return false;
                }
            }
        }

        MCReader reader = MCReader.get(tag);
        if (reader == null) {
            mLastError = context.getString(R.string.copy_wizard_error_no_mfc_device);
            return false;
        }
        File keyDir = Common.getFile(Common.KEYS_DIR);
        File[] keyFiles = keyDir != null ? keyDir.listFiles(File::isFile) : null;
        if (keyFiles == null || reader.setKeyFile(keyFiles, context) <= 0) {
            reader.close();
            ArrayList<Integer> all = new ArrayList<>();
            for (int i = 0; i < reader.getSectorCount(); i++) {
                all.add(i);
            }
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", all));
            return false;
        }
        int sectorCount = reader.getSectorCount();
        reader.setMappingRange(0, sectorCount - 1);
        int status = 0;
        while (status != -1 && status < sectorCount - 1) {
            status = reader.buildNextKeyMapPart();
        }
        if (status == -1) {
            reader.close();
            ArrayList<Integer> all = new ArrayList<>();
            for (int i = 0; i < sectorCount; i++) {
                all.add(i);
            }
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", all));
            return false;
        }
        SparseArray<byte[][]> keyMap = reader.getKeyMap();
        ArrayList<Integer> missing = new ArrayList<>();
        for (int i = 0; i < sectorCount; i++) {
            if (keyMap.get(i) == null) {
                missing.add(i);
            }
        }
        if (!missing.isEmpty()) {
            reader.close();
            mLastError = context.getString(R.string.copy_wizard_error_key_map,
                    TextUtils.join(", ", missing));
            return false;
        }

        HashMap<Integer, int[]> pos = new HashMap<>();
        for (Map.Entry<Integer, HashMap<Integer, byte[]>> e : dumpWithPos.entrySet()) {
            Set<Integer> blocks = e.getValue().keySet();
            int[] arr = new int[blocks.size()];
            int idx = 0;
            for (int b : blocks) {
                arr[idx++] = b;
            }
            pos.put(e.getKey(), arr);
        }

        HashMap<Integer, HashMap<Integer, Integer>> writeOnPos =
                reader.isWritableOnPositions(pos, keyMap);
        if (writeOnPos == null) {
            reader.close();
            if (enableManufacturerBlock) {
                mLastError = context.getString(R.string.copy_wizard_error_tag_not_magic);
            } else {
                mLastError = context.getString(R.string.copy_wizard_error_key_map,
                        TextUtils.join(", ", missing));
            }
            return false;
        }

        for (int sector : writeOnPos.keySet()) {
            byte[][] keys = keyMap.get(sector);
            for (int block : writeOnPos.get(sector).keySet()) {
                byte[] writeKey = null;
                boolean useAsKeyB = true;
                int wi = writeOnPos.get(sector).get(block);
                if (wi == 1 || wi == 4) {
                    writeKey = keys[0];
                    useAsKeyB = false;
                } else if (wi == 2 || wi == 5 || wi == 6) {
                    writeKey = keys[1];
                }
                if (writeKey == null) {
                    reader.close();
                    return false;
                }
                byte[] data = dumpWithPos.get(sector).get(block);
                int result = 0;
                for (int i = 0; i < 2; i++) {
                    result = reader.writeBlock(sector, block, data, writeKey, useAsKeyB);
                    if (result == 0) {
                        break;
                    }
                }
                if (result != 0) {
                    reader.close();
                    if (enableManufacturerBlock && sector == 0 && block == 0) {
                        mLastError = context.getString(R.string.copy_wizard_error_tag_not_magic);
                    }
                    return false;
                }
            }
        }
        reader.close();
        return true;
    }

    private HashMap<Integer, HashMap<Integer, byte[]>> parseDump(String[] dump) {
        HashMap<Integer, HashMap<Integer, byte[]>> ret = new HashMap<>();
        int sector = 0;
        int block = 0;
        for (String line : dump) {
            if (line.startsWith("+")) {
                String[] tmp = line.split(": ");
                sector = Integer.parseInt(tmp[tmp.length - 1]);
                block = 0;
                ret.put(sector, new HashMap<>());
            } else if (!line.contains("-")) {
                ret.get(sector).put(block++, Common.hex2Bytes(line));
            } else {
                block++;
            }
        }
        return ret;
    }

    private boolean checkBlock0(Tag tag, String block0) {
        int uidLen = tag.getId().length;
        if (uidLen == 4) {
            byte bcc = Common.hex2Bytes(block0.substring(8, 10))[0];
            byte[] uid = Common.hex2Bytes(block0.substring(0, 8));
            if (!Common.isValidBcc(uid, bcc)) {
                return false;
            }
        }
        MCReader tmp = MCReader.get(tag);
        boolean valid = Common.isValidBlock0(block0, uidLen,
                tmp != null ? tmp.getSize() : 0, true);
        if (tmp != null) {
            tmp.close();
        }
        return valid;
    }

    private String[] buildDump(SparseArray<String[]> rawDump, int sectorCount) {
        ArrayList<String> tmpDump = new ArrayList<>();
        for (int i = 0; i < sectorCount; i++) {
            String[] val = rawDump.get(i);
            tmpDump.add("+Sector: " + i);
            if (val != null) {
                Collections.addAll(tmpDump, val);
            } else {
                tmpDump.add("*No keys found or dead sector");
            }
        }
        return tmpDump.toArray(new String[0]);
    }
}
