/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.jcodings.specific.UTF8Encoding;
import org.jspecify.annotations.Nullable;

/**
 * Represents an Oniguruma string.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/main/src/onig-string.cc">
 * node-oniguruma/src/onig-string.cc</a>
 */
public abstract class OnigString {

    /**
     * The content of the string.
     */
    public final String content;

    /**
     * The number of bytes in the string.
     */
    public final int bytesCount;

    protected final byte[] bytesUTF8;

    private OnigString(String content, byte[] bytesUTF8) {
        this.content = content;
        this.bytesCount = bytesUTF8.length;
        this.bytesUTF8 = bytesUTF8;
    }

    /**
     * Returns UTF-8 bytes representation of the string.
     */
    public byte[] bytesUTF8() {
        return bytesUTF8;
    }

    /**
     * Returns the byte index of the given character index.
     *
     * @param charIndex the character index
     */
    abstract int getByteIndexOfChar(int charIndex);

    /**
     * Returns the character index of the given byte index.
     *
     * @param byteIndex the byte index
     */
    abstract int getCharIndexOfByte(int byteIndex);

    /**
     * Throws an ArrayIndexOutOfBoundsException if the given index is out of range.
     *
     * @param indexName the name of the index
     * @param index     the index value
     * @param minIndex  the minimum allowed index
     * @param maxIndex  the maximum allowed index
     */
    @SuppressWarnings("SameParameterValue")
    protected final void throwOutOfBoundsException(String indexName, int index, int minIndex, int maxIndex) {
        throw new ArrayIndexOutOfBoundsException(
            indexName + " index " + index + " is out of range " + minIndex + ".." + maxIndex + " of " + this
        );
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[string=\"" + content + "\"]";
    }

    /**
     * Creates a new OnigString instance from the given string.
     *
     * @param str the source string
     */
    public static OnigString of(String str) {
        byte[] bytesUTF8 = str.getBytes(StandardCharsets.UTF_8);
        if (bytesUTF8.length == str.length()) {
            return new SingleByteString(str, bytesUTF8);
        }
        return new MultiByteString(str, bytesUTF8);
    }

    //*************************************************************************

    /**
     * Represents a string that contains multibyte characters.
     */
    static final class MultiByteString extends OnigString {

        /**
         * Holds the index of the character corresponding to each byte. For example,
         * if {$code byteToCharOffsets[100] == 60} and {$code byteToCharOffsets[101] == 60},
         * then the bytes at indexes 100 and 101 both belong to the same multibyte character
         * at index 60.
         */
        private int @Nullable [] byteToCharOffsets;
        private final int lastCharIndex;

        private MultiByteString(String str, byte[] bytesUTF8) {
            super(str, bytesUTF8);
            lastCharIndex = str.length() - 1;
        }

        @Override
        int getByteIndexOfChar(int charIndex) {
            if (charIndex == lastCharIndex + 1) {
                // one off can happen when finding the end of a regexp (it's the right boundary)
                return bytesCount;
            }
            if (charIndex < 0 || charIndex > lastCharIndex) {
                throwOutOfBoundsException("Char", charIndex, 0, lastCharIndex);
            }
            if (charIndex == 0) {
                return 0;
            }

            int[] byteToCharOffsets = getByteToCharOffsets();
            int byteIndex = Arrays.binarySearch(byteToCharOffsets, charIndex);
            while (byteIndex > 0 && byteToCharOffsets[byteIndex - 1] == charIndex) {
                byteIndex--;
            }

            return byteIndex;
        }

        private int[] getByteToCharOffsets() {
            int[] offsets = byteToCharOffsets;
            if (offsets == null) {
                offsets = new int[bytesCount];
                int charIndex = 0;
                int byteIndex = 0;
                int maxByteIndex = bytesCount - 1;

                while (byteIndex <= maxByteIndex) {
                    int charLenInBytes = UTF8Encoding.INSTANCE.length(bytesUTF8, byteIndex, bytesCount);
                    // same as "Arrays.fill(offsets, byteIndex, byteIndex + charLenInBytes, charIndex)" but faster
                    for (int l = byteIndex + charLenInBytes; byteIndex < l; byteIndex++) {
                        offsets[byteIndex] = charIndex;
                    }
                    charIndex++;
                }
                byteToCharOffsets = offsets;
            }

            return offsets;
        }

        @Override
        int getCharIndexOfByte(int byteIndex) {
            if (byteIndex == bytesCount) {
                // one off can happen when finding the end of a regexp (it's the right boundary)
                return lastCharIndex + 1;
            }
            if (byteIndex < 0 || byteIndex >= bytesCount) {
                throwOutOfBoundsException("Byte", byteIndex, 0, bytesCount - 1);
            }
            if (byteIndex == 0) {
                return 0;
            }
            return getByteToCharOffsets()[byteIndex];
        }
    }

    /**
     * Represents a string is only composed of single-byte characters
     */
    static final class SingleByteString extends OnigString {

        private SingleByteString(String str, byte[] bytesUTF8) {
            super(str, bytesUTF8);
        }

        @Override
        int getByteIndexOfChar(int charIndex) {
            if (charIndex == bytesCount) {
                // one off can happen when finding the end of a regexp (it's the right boundary)
                return charIndex;
            }
            if (charIndex < 0 || charIndex >= bytesCount) {
                throwOutOfBoundsException("Char", charIndex, 0, bytesCount - 1);
            }
            return charIndex;
        }

        @Override
        int getCharIndexOfByte(int byteIndex) {
            if (byteIndex == bytesCount) {
                // one off can happen when finding the end of a regexp (it's the right boundary)
                return byteIndex;
            }
            if (byteIndex < 0 || byteIndex >= bytesCount) {
                throwOutOfBoundsException("Byte", byteIndex, 0, bytesCount - 1);
            }
            return byteIndex;
        }
    }
}
