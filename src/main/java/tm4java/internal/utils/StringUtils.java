/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.jspecify.annotations.Nullable;

/**
 * A utility class for performing common string operations.
 */
public final class StringUtils {

    private static final List<String> LIST_WITH_EMPTY_STRING = List.of("");

    private StringUtils() {
        // utility class
    }

    /**
     * See {@link #splitToArray(String, char, int)}.
     */
    public static String[] splitToArray(String line, char separator) {
        return splitToArray(line, separator, -1);
    }

    /**
     * Splits the given line into an array using the specified separator and limit.
     *
     * <p>Very fast string splitting. 7.5 times faster than {@link String#split(String)}
     * and 2.5 times faster than {@code com.google.common.base.Splitter}.
     *
     * @param line      the line to split
     * @param separator the separator character
     * @param limit     the maximum number of elements to include in the resulting array
     */
    public static String[] splitToArray(String line, char separator, int limit) {
        if (line.isEmpty()) {
            return new String[] {""};
        }

        var tmp = new String[8];
        int count = 0;
        int start = 0;
        int end = line.indexOf(separator);

        while (end >= 0) {
            if (count == tmp.length) { // check if array needs resize
                var tmp2 = new String[tmp.length + (tmp.length >> 1)];
                System.arraycopy(tmp, 0, tmp2, 0, count);
                tmp = tmp2;
            }
            tmp[count] = line.substring(start, end);
            count++;
            start = end + 1;
            if (count == limit) {
                break;
            }
            end = line.indexOf(separator, start);
        }

        if (count == tmp.length) { // check if array needs resize
            var tmp2 = new String[tmp.length + 1];
            System.arraycopy(tmp, 0, tmp2, 0, count);
            tmp = tmp2;
        }

        tmp[count] = line.substring(start);
        count++;

        if (count == tmp.length) {
            return tmp;
        }

        var result = new String[count];
        System.arraycopy(tmp, 0, result, 0, count);

        return result;
    }

    /**
     * Splits the given string into a list using the specified separator.
     *
     * @param line      the string to split
     * @param separator the separator character
     */
    public static List<String> splitToList(String line, char separator) {
        if (line.isEmpty()) {
            return LIST_WITH_EMPTY_STRING;
        }

        var result = new ArrayList<String>(8);
        int start = 0;
        int end = line.indexOf(separator);

        while (end >= 0) {
            result.add(line.substring(start, end));
            start = end + 1;
            end = line.indexOf(separator, start);
        }
        result.add(line.substring(start));

        return result;
    }

    /**
     * Returns true if `str` contains any Unicode character that is classified as "R" or "AL".
     */
    public static boolean containsRTL(String str) {
        Pattern p = CONTAINS_RTL;
        if (p == null) {
            p = CONTAINS_RTL = makeContainsRtl();
        }
        return p.matcher(str).find();
    }

    private static volatile @Nullable Pattern CONTAINS_RTL;

    private static Pattern makeContainsRtl() {
        return Pattern.compile(
            "(?:[\\u05BE\\u05C0\\u05C3\\u05C6\\u05D0-\\u05F4\\u0608\\u060B\\u060D\\u061B-\\u064A"
                + "\\u066D-\\u066F\\u0671-\\u06D5\\u06E5\\u06E6\\u06EE\\u06EF\\u06FA-\\u0710"
                + "\\u0712-\\u072F\\u074D-\\u07A5\\u07B1-\\u07EA\\u07F4\\u07F5\\u07FA"
                + "\\u07FE-\\u0815\\u081A\\u0824\\u0828\\u0830-\\u0858\\u085E-\\u088E"
                + "\\u08A0-\\u08C9\\u200F\\uFB1D\\uFB1F-\\uFB28\\uFB2A-\\uFD3D"
                + "\\uFD50-\\uFDC7\\uFDF0-\\uFDFC\\uFE70-\\uFEFC]"
                + "|\\uD802[\\uDC00-\\uDD1B\\uDD20-\\uDE00\\uDE10-\\uDE35\\uDE40-\\uDEE4"
                + "\\uDEEB-\\uDF35\\uDF40-\\uDFFF]"
                + "|\\uD803[\\uDC00-\\uDD23\\uDE80-\\uDEA9\\uDEAD-\\uDF45\\uDF51-\\uDF81"
                + "\\uDF86-\\uDFF6]"
                + "|\\uD83A[\\uDC00-\\uDCCF\\uDD00-\\uDD43\\uDD4B-\\uDFFF]"
                + "|\\uD83B[\\uDC00-\\uDEBB])"
        );
    }

    /**
     * Compares two strings lexicographically.
     */
    public static int strCmp(String a, String b) {
        int result = a.compareTo(b);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Compares two strings lexicographically.
     */
    public static int strcmp(String a, String b) {
        final int result = a.compareTo(b);
        if (result < 0) {
            return -1;
        } else if (result > 0) {
            return 1;
        }
        return 0;
    }

    /**
     * Compares two lists of strings lexicographically.
     */
    public static int strArrCmp(@Nullable List<String> a, @Nullable List<String> b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }

        int len1 = a.size();
        int len2 = b.size();

        if (len1 == len2) {
            for (int i = 0; i < len1; i++) {
                int res = strcmp(a.get(i), b.get(i));
                if (res != 0) {
                    return res;
                }
            }
            return 0;
        }
        return len1 - len2;
    }
}
