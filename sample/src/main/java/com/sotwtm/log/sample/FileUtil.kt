package com.sotwtm.log.sample

import com.sotwtm.util.Log

import java.io.BufferedReader
import java.io.File
import java.io.FileReader

/**
 * Util for file operations.
 * @author sheungon
 */
object FileUtil {

    /**
     * Read a text file as [String]
     * @param file To be read.
     * @param stringBuilder A builder to storage the file content. All content will be append to
     * the end of this [StringBuilder]
     * @return `true` if the operation was successfully executed.
     */
    fun readTextFile(file: File,
                     stringBuilder: StringBuilder): Boolean {

        var br: BufferedReader? = null
        try {
            br = BufferedReader(FileReader(file))

            var firstLine = true
            var line: String? = br.readLine()
            while (line != null) {
                if (firstLine) {
                    firstLine = false
                } else {
                    stringBuilder.append("\n")
                }
                stringBuilder.append(line)

                line = br.readLine()
            }

            return true
        } catch (e: Exception) {
            Log.e("Error on read file : $file", e)
        } finally {
            try {
                br?.close()
            } catch (e: Exception) {
                Log.e("Error on close read file : $file", e)
            }
        }

        return false
    }
}
