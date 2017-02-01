package com.sotwtm.log.sample;

import android.support.annotation.NonNull;

import com.sotwtm.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Util for file operations.
 * @author sheungon
 */
public class FileUtil {

    /**
     * Read a text file as {@link String}
     * @param file To be read.
     * @param stringBuilder A builder to storage the file content. All content will be append to
     *                      the end of this {@link StringBuilder}
     * @return {@code true} if the operation was successfully executed.
     * */
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
