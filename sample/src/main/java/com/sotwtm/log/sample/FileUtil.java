package com.sotwtm.log.sample;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.sotwtm.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * @author John
 */
public class FileUtil {

    public static final String LOG_FILE_NAME = "sms_interceptor.txt";

    @Nullable
    public static File getLogFile() {

        // external directory
        File externalStorageDir = Environment.getExternalStorageDirectory();
        if (externalStorageDir != null) {
            return new File(externalStorageDir, LOG_FILE_NAME);
        }

        return null;
    }

    public static boolean readTextFile(@NonNull File file,
                                       @NonNull StringBuilder stringBuilder) {

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));

            boolean firstLine = true;
            String line;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                } else {
                    stringBuilder.append("\n");
                }
                stringBuilder.append(line);
            }

            return true;
        } catch (Exception e) {
            Log.e("Error on read file : " + file, e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (Exception e) {
                    Log.e("Error on close read file : " + file, e);
                }
            }
        }

        return false;
    }
}
