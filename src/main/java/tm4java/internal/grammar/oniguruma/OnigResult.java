package tm4java.internal.grammar.oniguruma;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.Arrays;

/**
 * Represents the result of an Oniguruma regular expression match.
 *
 * @see <a href="https://github.com/atom/node-oniguruma/blob/master/src/onig-result.cc">
 * node-oniguruma/src/onig-result.cc</a>
 */
public final class OnigResult {

    // capture start/end positions copied from the native OnigRegion struct (fields beg/end)
    private final int[] beg;
    private final int[] end;

    // the index of this search result in the scanner used to track the best match
    private int indexInScanner = -1;

    /**
     * Creates a new OnigResult instance.
     *
     * @param regionPtr the region of the match
     */
    OnigResult(MemorySegment regionPtr) {
        // regionPtr is a raw pointer, reinterpret it with the size of OnigRegion so fields can be read

        MemorySegment r = regionPtr.reinterpret(Oniguruma.REGION_LAYOUT.byteSize());
        // num_regs from OnigRegion: number of capture groups, including group 0 (the whole match)
        int numRegs = r.get(ValueLayout.JAVA_INT, Oniguruma.NUM_REGS_OFFSET);

        this.beg = new int[numRegs];
        this.end = new int[numRegs];

        // beg/end in OnigRegion are C int* arrays; reinterpret with the byte length and copy into Java arrays
        if (numRegs > 0) {
            long bytes = (long) numRegs * Integer.BYTES;
            MemorySegment begArr = r.get(ValueLayout.ADDRESS, Oniguruma.BEG_OFFSET).reinterpret(bytes);
            MemorySegment endArr = r.get(ValueLayout.ADDRESS, Oniguruma.END_OFFSET).reinterpret(bytes);
            MemorySegment.copy(begArr, ValueLayout.JAVA_INT, 0L, this.beg, 0, numRegs);
            MemorySegment.copy(endArr, ValueLayout.JAVA_INT, 0L, this.end, 0, numRegs);
        }
    }

    /**
     * Creates a new OnigResult instance from pre-computed arrays.
     *
     * @param beg the start positions
     * @param end the end positions
     */
    OnigResult(int[] beg, int[] end) {
        this.beg = beg;
        this.end = end;
    }

    /**
     * Returns the start location of the specified capture group.
     *
     * @param index the index of the capture group
     */
    public int locationAt(int index) {
        if (index < 0 || index >= beg.length) {
            return -1;
        }
        return beg[index];
    }

    /**
     * Returns the length of the specified capture group.
     *
     * @param index the index of the capture group
     */
    public int lengthAt(int index) {
        if (index < 0 || index >= beg.length || beg[index] < 0 || end[index] < 0) {
            return -1;
        }
        return end[index] - beg[index];
    }

    /**
     * Returns the number of capture groups.
     */
    public int count() {
        return beg.length;
    }

    /**
     * Returns the array of start positions.
     */
    int[] begArray() {
        return beg;
    }

    /**
     * Returns the array of end positions.
     */
    int[] endArray() {
        return end;
    }

    /**
     * Returns a string representation of the capture start positions, for logging.
     */
    String begAsString() {
        return Arrays.toString(beg);
    }

    /**
     * Returns a string representation of the capture end positions, for logging.
     */
    String endAsString() {
        return Arrays.toString(end);
    }

    /**
     * Returns the index of the match, for scanner.
     */
    int getIndex() {
        return indexInScanner;
    }

    /**
     * Sets the index of the match, for scanner.
     */
    void setIndex(int index) {
        indexInScanner = index;
    }

    @Override
    public String toString() {
        return "OnigResult [indexInScanner=" + indexInScanner
            + ", count=" + count()
            + ", beg=" + Arrays.toString(beg)
            + ", end=" + Arrays.toString(end) + "]";
    }
}