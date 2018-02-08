package com.sotwtm.util

import android.util.Base64
import java.io.UnsupportedEncodingException
import java.util.*

/**
 * Some basic string utils
 * @author sheungon
 */
object ECStringUtil {

    private const val QUOTE_ENCODE = "&quot;"
    private const val APOS_ENCODE = "&apos;"
    private const val AMP_ENCODE = "&amp;"
    private const val LT_ENCODE = "&lt;"
    private const val GT_ENCODE = "&gt;"

    /**
     * Pseudo-random number generator object for use with randomString().
     * The Random class is not considered to be cryptographically secure, so
     * only use these random Strings for low to medium security applications.
     */
    private val RAND_GEN = Random()

    /**
     * Array of numbers and letters of mixed case. Numbers appear in the list
     * twice so that there is a more equal chance that a number will be picked.
     * We can use the array to get a random number or letter by picking a random
     * array index.
     */
    private val NUMBERS_AND_LETTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    /**
     * Escapes all necessary characters in the String so that it can be used
     * in an XML doc.
     *
     * @param string the string to escape.
     * @return the string with appropriate characters escaped.
     */
    fun escapeForXML(string: String?): CharSequence? {
        if (string == null) {
            return null
        }
        val input = string.toCharArray()
        val len = input.size
        val out = StringBuilder((len * 1.3).toInt())
        var toAppend: CharSequence?
        var ch: Char
        var last = 0
        var i = 0
        while (i < len) {
            toAppend = null
            ch = input[i]
            when (ch) {
                '<' -> toAppend = LT_ENCODE
                '>' -> toAppend = GT_ENCODE
                '&' -> toAppend = AMP_ENCODE
                '"' -> toAppend = QUOTE_ENCODE
                '\'' -> toAppend = APOS_ENCODE
                else -> {
                }
            }
            if (toAppend != null) {
                if (i > last) {
                    out.append(input, last, i - last)
                }
                out.append(toAppend)
                last = ++i
            } else {
                i++
            }
        }
        if (last == 0) {
            return string
        }
        if (i > last) {
            out.append(input, last, i - last)
        }
        return out
    }

    /**
     * Encodes an array of bytes as String representation of hexadecimal.
     *
     * @param bytes an array of bytes to convert to a hex string.
     * @return generated hex string.
     */
    fun encodeHex(bytes: ByteArray): String {
        val hex = StringBuilder(bytes.size * 2)

        for (aByte in bytes) {
            if (aByte.toInt() and 0xff < 0x10) {
                hex.append("0")
            }
            hex.append(Integer.toString(aByte.toInt() and 0xff, 16))
        }

        return hex.toString()
    }

    /**
     * Encodes a byte array into a bse64 String.
     *
     * @param data       The byte arry to encode.
     * @param offset     the offset of the bytearray to begin encoding at.
     * @param len        the length of bytes to encode.
     * @param lineBreaks True if the encoding should contain line breaks and false if it should not.
     * @return A base64 encoded String.
     */
    fun encodeBase64(data: ByteArray, offset: Int, len: Int, lineBreaks: Boolean): String {
        return Base64.encodeToString(data, offset, len, if (lineBreaks) Base64.DEFAULT else Base64.NO_WRAP)
    }

    /**
     * Encodes a byte array into a bse64 String.
     *
     * @param data       The byte arry to encode.
     * @param lineBreaks True if the encoding should contain line breaks and false if it should not.
     * @return A base64 encoded String.
     */
    @JvmOverloads
    fun encodeBase64(data: ByteArray, lineBreaks: Boolean = false): String {
        return encodeBase64(data, 0, data.size, lineBreaks)
    }

    /**
     * Encodes a String as a base64 String.
     *
     * @param data     a String to encode.
     * @param encoding A specific encoding for the input string
     * @return a base64 encoded String.
     */
    @JvmOverloads
    fun encodeBase64(data: String,
                     encoding: String = "UTF-8"): String? {
        val bytes: ByteArray
        try {
            bytes = data.toByteArray(charset(encoding))
        } catch (uee: UnsupportedEncodingException) {
            Log.e("UnsupportedEncoding : " + encoding, uee)
            return null
        }

        return encodeBase64(bytes)
    }

    /**
     * Decodes a base64 String.
     *
     * @param data     a base64 encoded String to decode.
     * @param encoding A specific encoding for the ouput string
     * @return the decoded String.
     */
    @JvmOverloads
    fun decodeBase64(data: String,
                     encoding: String = "UTF-8"): ByteArray {
        var bytes: ByteArray
        try {
            bytes = data.toByteArray(charset(encoding))
        } catch (uee: java.io.UnsupportedEncodingException) {
            bytes = data.toByteArray()
        }

        bytes = Base64.decode(bytes, 0, bytes.size, Base64.DEFAULT)
        return bytes
    }

    /**
     * Returns a random String of the specified length. The method uses the Random class that is
     * built-in to Java which is suitable for low to medium grade security uses.
     * This means that the output is only pseudo random, i.e., each number is
     * mathematically generated so is not truly random.
     *
     *
     * The specified length must be at least one. If not, the method will return empty string.
     *
     * @param length      the desired length of the random String to return.
     * @param sourceChars A set of chars allowed to use in the output random string
     * @return a random String of numbers and letters of the specified length.
     */
    @JvmOverloads
    fun randomString(length: Int,
                     sourceChars: CharArray = NUMBERS_AND_LETTERS): String {
        if (length < 1) {
            return ""
        }

        // Create a char buffer to put random letters and numbers in.
        val randBuffer = CharArray(length)
        for (i in randBuffer.indices) {
            randBuffer[i] = sourceChars[RAND_GEN.nextInt(sourceChars.size)]
        }

        return String(randBuffer)
    }
}
