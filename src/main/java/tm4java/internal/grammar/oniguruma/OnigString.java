/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.grammar.oniguruma;

import org.jspecify.annotations.Nullable;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.charset.StandardCharsets;

/**
 * Represents an Oniguruma string.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/main/src/onig-string.cc">
 * node-oniguruma/src/onig-string.cc</a>
 */
public abstract class OnigString {

    private record NativeBuffer(MemorySegment segment, MemorySegment end) { }

    private volatile @Nullable NativeBuffer nativeBuffer;

    /**
     * The content of the string.
     */
    public final String content;

    /**
     * The number of bytes in the string.
     */
    public final int bytesCount;

    /**
     * The UTF-8 representation of the string.
     */
    protected final byte[] bytesUTF8;

    /**
     * Constructs a new {@code OnigString} instance.
     *
     * @param content   the source string
     * @param bytesUTF8 the UTF-8 encoded bytes of {@code content}
     */
    private OnigString(String content, byte[] bytesUTF8) {
        this.content = content;
        this.bytesCount = bytesUTF8.length;
        this.bytesUTF8 = bytesUTF8;
    }

    /**
     * Returns the UTF-8 bytes of the string.
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
     * Returns the native memory segment holding the string bytes.
     */
    MemorySegment nativeSegment() {
        return nativeBuffer().segment();
    }

    /**
     * Returns the native memory segment marking the end of the string.
     */
    MemorySegment nativeEnd() {
        return nativeBuffer().end();
    }

    /**
     * Returns the lazily allocated native buffer for this string.
     */
    private NativeBuffer nativeBuffer() {
        NativeBuffer b = nativeBuffer;
        if (b == null) {
            synchronized (this) {
                b = nativeBuffer;
                if (b == null) {
                    // allocateFrom copies the bytes into off-heap memory
                    MemorySegment seg = Arena.ofAuto().allocateFrom(ValueLayout.JAVA_BYTE, bytesUTF8);
                    b = new NativeBuffer(seg, seg.asSlice(bytesUTF8.length));
                    nativeBuffer = b;
                }
            }
        }
        return b;
    }

    /**
     * Throws an ArrayIndexOutOfBoundsException if the given index is out of range.
     *
     * @param indexName the name of the index
     * @param index     the index value
     * @param minIndex  the minimum allowed index
     * @param maxIndex  the maximum allowed index
     */
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
     * Creates a new {@code OnigString} instance from the given string.
     *
     * @param str the source string
     */
    public static OnigString of(String str) {
        byte[] bytesUTF8 = str.getBytes(StandardCharsets.UTF_8);
        // if byte length equals char length, all characters are single-byte
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
         * if {@code byteToCharOffsets[100] == 60} and {@code byteToCharOffsets[101] == 60},
         * then the bytes at indexes 100 and 101 both belong to the same multibyte character
         * at index 60.
         */
        private final int[] byteToCharOffsets;
        private final int[] charToByteOffsets;
        private final int lastCharIndex;

        private MultiByteString(String str, byte[] bytesUTF8) {
            super(str, bytesUTF8);
            this.lastCharIndex = str.length() - 1;

            int strLen = str.length();
            this.charToByteOffsets = new int[strLen + 1];
            this.byteToCharOffsets = new int[bytesUTF8.length];

            int charIndex = 0;
            int byteIndex = 0;

            while (charIndex < strLen) {
                charToByteOffsets[charIndex] = byteIndex;
                int codePoint = str.codePointAt(charIndex);
                int charCount = Character.charCount(codePoint);
                if (charCount == 2) { // surrogate pairs
                    charToByteOffsets[charIndex + 1] = byteIndex;
                }
                int utf8Len = getUtf8Length(codePoint);
                for (int i = 0; i < utf8Len && byteIndex < bytesUTF8.length; i++) {
                    byteToCharOffsets[byteIndex++] = charIndex;
                }
                charIndex += charCount;
            }

            charToByteOffsets[strLen] = byteIndex;
        }

        @Override
        int getByteIndexOfChar(int charIndex) {
            if (charIndex < 0 || charIndex > lastCharIndex + 1) {
                throwOutOfBoundsException("Char", charIndex, 0, lastCharIndex);
            }
            return charToByteOffsets[charIndex];
        }

        @Override
        int getCharIndexOfByte(int byteIndex) {
            if (byteIndex < 0 || byteIndex > bytesCount) {
                throwOutOfBoundsException("Byte", byteIndex, 0, bytesCount);
            }
            if (byteIndex == bytesCount) {
                return lastCharIndex + 1;
            }
            return byteToCharOffsets[byteIndex];
        }

        private static int getUtf8Length(int codePoint) {
            if (codePoint <= 0x7F) {
                return 1;
            }
            if (codePoint <= 0x7FF) {
                return 2;
            }
            if (codePoint <= 0xFFFF) {
                return 3;
            }
            return 4;
        }
    }

    /**
     * Represents a string is only composed of single-byte characters.
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
