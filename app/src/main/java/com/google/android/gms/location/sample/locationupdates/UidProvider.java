package com.google.android.gms.location.sample.locationupdates;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.UUID;

public class UidProvider {

    public static String getUniqueId(Context context) {
        String deviceId = null;

        deviceId = DeviceSerialIdProvider.getDeviceSerialId();
        if (deviceId != null)
            return generateUuidStringFromId(deviceId);

        deviceId = AndroidSecureIdProvider.getAndroidSecureId(context);
        if (deviceId != null)
            return generateUuidStringFromId(deviceId);

        deviceId = SoftInstallationIdProvider.getSoftInstallationId(context);
        return deviceId;
    }

    private static String generateUuidStringFromId(String deviceId) {
        return UUID.nameUUIDFromBytes(deviceId.getBytes()).toString();
    }

    private static class DeviceSerialIdProvider {
        private static final String[] IGNORED_SERIAL_PATTERNS = {"1234567", "abcdef", "dead00beef"};

        public synchronized static String getDeviceSerialId() {
            try {
                if (isValidDeviceSerialId(Build.SERIAL)) {
                    return Build.SERIAL;
                }
            } catch (NoSuchFieldError error) {
            }
            return null;
        }

        private static boolean isValidDeviceSerialId(String serialId) {
            return (!TextUtils.isEmpty(serialId) && !Build.UNKNOWN.equals(serialId) && !isIgnoredSerial(serialId));
        }

        private static boolean isIgnoredSerial(String id) {
            if (!TextUtils.isEmpty(id)) {
                id = id.toLowerCase();
                for (String pattern : IGNORED_SERIAL_PATTERNS) {
                    if (id.contains(pattern)) {
                        return true;
                    }
                }
            } else {
                return true;
            }
            return false;
        }
    }

    private static class AndroidSecureIdProvider {
        private static final String ANDROID_EMULATOR_ID = "9774d56d682e549c";

        public synchronized static String getAndroidSecureId(Context context) {
            String androidSecureId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            if (isValidAndroidSecureId(androidSecureId)) {
                return androidSecureId;
            }
            return null;
        }

        private static boolean isValidAndroidSecureId(String androidSecureId) {
            return (!TextUtils.isEmpty(androidSecureId) && !ANDROID_EMULATOR_ID.equals(androidSecureId) && !isIgnoredDeviceId(androidSecureId)
                    && androidSecureId.length() == ANDROID_EMULATOR_ID.length());
        }

        private static boolean isIgnoredDeviceId(String id) {
            // empty or contains only spaces or 0
            return TextUtils.isEmpty(id) || TextUtils.isEmpty(id.replace('0', ' ').replace('-', ' ').trim());
        }
    }

    private static class SoftInstallationIdProvider {
        private static final String INSTALLATION_FILE_NAME = "INSTALLATION";
        private static String sID = null;

        public synchronized static String getSoftInstallationId(Context context) {
            if (sID == null) {
                File installation = new File(context.getFilesDir(), INSTALLATION_FILE_NAME);
                try {
                    if (!installation.exists()) {
                        writeInstallationFile(installation);
                    }
                    sID = readInstallationFile(installation);
                } catch (Exception epicFail) {
                    throw new RuntimeException(epicFail);
                }
            }
            return sID;
        }

        private static String readInstallationFile(File installation) throws IOException {
            RandomAccessFile f = new RandomAccessFile(installation, "r");
            try {
                byte[] bytes = new byte[(int) f.length()];
                f.readFully(bytes);
                f.close();
                return new String(bytes);
            } finally {
                f.close();
            }
        }

        private static void writeInstallationFile(File installation) throws IOException {
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(installation);
                String id = UUID.randomUUID().toString();
                out.write(id.getBytes());
            } finally {
                out.close();
            }
        }
    }
}