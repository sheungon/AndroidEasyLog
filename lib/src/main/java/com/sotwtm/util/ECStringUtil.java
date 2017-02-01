package com.sotwtm.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Base64;

import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Some basic string utils
 * @author sheungon
 */
@SuppressWarnings("unused")
public class ECStringUtil {

    public static final String QUOTE_ENCODE = "&quot;";
    public static final String APOS_ENCODE = "&apos;";
    public static final String AMP_ENCODE = "&amp;";
    public static final String LT_ENCODE = "&lt;";
    public static final String GT_ENCODE = "&gt;";

    /**
     * Pseudo-random number generator object for use with randomString().
     * The Random class is not considered to be cryptographically secure, so
     * only use these random Strings for low to medium security applications.
     */
    private static final Random RAND_GEN = new Random();

    /**
     * Array of numbers and letters of mixed case. Numbers appear in the list
     * twice so that there is a more equal chance that a number will be picked.
     * We can use the array to get a random number or letter by picking a random
     * array index.
     */
    public static final char[] NUMBERS_AND_LETTERS = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();

    /**
     * Escapes all necessary characters in the String so that it can be used
     * in an XML doc.
     *
     * @param string the string to escape.
     * @return the string with appropriate characters escaped.
     */
    public static CharSequence escapeForXML(final String string) {
        if (string == null) {
            return null;
        }
        final char[] input = string.toCharArray();
        final int len = input.length;
        final StringBuilder out = new StringBuilder((int) (len * 1.3));
        CharSequence toAppend;
        char ch;
        int last = 0;
        int i = 0;
        while (i < len) {
            toAppend = null;
            ch = input[i];
            switch (ch) {
                case '<':
                    toAppend = LT_ENCODE;
                    break;
                case '>':
                    toAppend = GT_ENCODE;
                    break;
                case '&':
                    toAppend = AMP_ENCODE;
                    break;
                case '"':
                    toAppend = QUOTE_ENCODE;
                    break;
                case '\'':
                    toAppend = APOS_ENCODE;
                    break;
                default:
                    break;
            }
            if (toAppend != null) {
                if (i > last) {
                    out.append(input, last, i - last);
                }
                out.append(toAppend);
                last = ++i;
            } else {
                i++;
            }
        }
        if (last == 0) {
            return string;
        }
        if (i > last) {
            out.append(input, last, i - last);
        }
        return out;
    }

    /**
     * Encodes an array of bytes as String representation of hexadecimal.
     *
     * @param bytes an array of bytes to convert to a hex string.
     * @return generated hex string.
     */
    public static String encodeHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);

        for (byte aByte : bytes) {
            if (((int) aByte & 0xff) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toString((int) aByte & 0xff, 16));
        }

        return hex.toString();
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
    public static String encodeBase64(byte[] data, int offset, int len, boolean lineBreaks) {
        return Base64.encodeToString(data, offset, len, (lineBreaks ? Base64.DEFAULT : Base64.NO_WRAP));
    }

    /**
     * Encodes a byte array into a bse64 String.
     *
     * @param data       The byte arry to encode.
     * @param lineBreaks True if the encoding should contain line breaks and false if it should not.
     * @return A base64 encoded String.
     */
    public static String encodeBase64(byte[] data, boolean lineBreaks) {
        return encodeBase64(data, 0, data.length, lineBreaks);
    }

    /**
     * Encodes a byte array into a base64 String.
     *
     * @param data a byte array to encode.
     * @return a base64 encode String.
     */
    public static String encodeBase64(byte[] data) {
        return encodeBase64(data, false);
    }

    /**
     * Encodes a String as a base64 String.
     *
     * @param data     a String to encode.
     * @param encoding A specific encoding for the input string
     * @return a base64 encoded String.
     */
    @Nullable
    public static String encodeBase64(@NonNull String data,
                                      @NonNull String encoding) {
        byte[] bytes;
        try {
            bytes = data.getBytes(encoding);
        } catch (UnsupportedEncodingException uee) {
            Log.e("UnsupportedEncoding : " + encoding, uee);
            return null;
        }
        return encodeBase64(bytes);
    }

    /**
     * Encodes a String as a base64 String.
     * Using default encoding, "UTF-8"
     *
     * @param data a String to encode.
     * @return a base64 encoded String.
     */
    @Nullable
    public static String encodeBase64(@NonNull String data) {
        return encodeBase64(data, "UTF-8");
    }

    /**
     * Decodes a base64 String.
     *
     * @param data     a base64 encoded String to decode.
     * @param encoding A specific encoding for the ouput string
     * @return the decoded String.
     */
    public static byte[] decodeBase64(@NonNull String data,
                                      @NonNull String encoding) {
        byte[] bytes;
        try {
            bytes = data.getBytes(encoding);
        } catch (java.io.UnsupportedEncodingException uee) {
            bytes = data.getBytes();
        }

        bytes = Base64.decode(bytes, 0, bytes.length, Base64.DEFAULT);
        return bytes;
    }

    /**
     * Decodes a base64 String.
     * Using default encoding, "UTF-8"
     *
     * @param data a base64 encoded String to decode.
     * @return the decoded String.
     */
    public static byte[] decodeBase64(String data) {
        return decodeBase64(data, "UTF-8");
    }

    /**
     * Returns a random String of numbers and letters (lower and upper case)
     * of the specified length. The method uses the Random class that is
     * built-in to Java which is suitable for low to medium grade security uses.
     * This means that the output is only pseudo random, i.e., each number is
     * mathematically generated so is not truly random.
     * <p>
     * The specified length must be at least one. If not, the method will return empty string.
     *
     * @param length the desired length of the random String to return.
     * @return a random String of numbers and letters of the specified length.
     */
    @NonNull
    public static String randomString(int length) {
        return randomString(length, NUMBERS_AND_LETTERS);
    }

    /**
     * Returns a random String of the specified length. The method uses the Random class that is
     * built-in to Java which is suitable for low to medium grade security uses.
     * This means that the output is only pseudo random, i.e., each number is
     * mathematically generated so is not truly random.
     * <p>
     * The specified length must be at least one. If not, the method will return empty string.
     *
     * @param length      the desired length of the random String to return.
     * @param sourceChars A set of chars allowed to use in the output random string
     * @return a random String of numbers and letters of the specified length.
     */
    @NonNull
    public static String randomString(int length,
                                      @NonNull char[] sourceChars) {
        if (length < 1) {
            return "";
        }

        // Create a char buffer to put random letters and numbers in.
        char[] randBuffer = new char[length];
        for (int i = 0; i < randBuffer.length; i++) {
            randBuffer[i] = sourceChars[RAND_GEN.nextInt(sourceChars.length)];
        }

        return new String(randBuffer);
    }
}