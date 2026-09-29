package tm4java.internal.grammar.oniguruma;

import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.System.Logger.Level;

/**
 * Provides low-level access to the Oniguruma library via the FFM API.
 */
final class Oniguruma {

    private static final System.Logger LOG = System.getLogger(Oniguruma.class.getName());

    static final int ONIG_MISMATCH = -1;
    static final int ONIG_OPTION_NONE = 0;
    static final int ONIG_OPTION_IGNORE_CASE = 1;
    static final int ONIG_OPTION_CAPTURE_GROUP = 256;

    static final MemorySegment ENC_UTF8;
    static final MemorySegment SYNTAX_ONIGURUMA;
    static final MethodHandle ONIG_NEW;
    static final MethodHandle ONIG_SEARCH;
    static final MethodHandle ONIG_REGION_NEW;
    static final MethodHandle ONIG_REGION_FREE;
    static final MethodHandle ONIG_FREE;
    static final MethodHandle ONIG_ERROR_CODE_TO_STR;

    // regions are reused to avoid frequent native allocations
    private static final ConcurrentLinkedQueue<MemorySegment> REGION_POOL = new ConcurrentLinkedQueue<>();

    /**
     * Acquires an OnigRegion from the pool, or allocates a new one if empty.
     *
     * @return the acquired region
     */
    static MemorySegment acquireRegion() {
        MemorySegment available = REGION_POOL.poll();
        if (available != null) {
            return available;
        }

        try {
            MemorySegment allocated = (MemorySegment) ONIG_REGION_NEW.invokeExact();
            if (allocated.address() == 0) {
                throw new OutOfMemoryError("onig_region_new returned NULL");
            }
            return allocated;
        } catch (Throwable t) {
            throw new IllegalStateException("Failed to allocate OnigRegion", t);
        }
    }

    /**
     * Returns an OnigRegion to the pool for reuse.
     *
     * @param r the region to release
     */
    static void releaseRegion(MemorySegment r) {
        REGION_POOL.offer(r);
    }

    /*
     * C struct OnigRegion (64-bit alignment):
     * int allocated;     // offset 0,  size 4
     * int num_regs;      // offset 4,  size 4
     * int* beg;          // offset 8,  size 8
     * int* end;          // offset 16, size 8
     * int* history_root; // offset 24, size 8
     */
    static final StructLayout REGION_LAYOUT = MemoryLayout.structLayout(
        ValueLayout.JAVA_INT.withName("allocated"),
        ValueLayout.JAVA_INT.withName("num_regs"),
        ValueLayout.ADDRESS.withName("beg"),
        ValueLayout.ADDRESS.withName("end"),
        ValueLayout.ADDRESS.withName("history_root")
    );
    static final long NUM_REGS_OFFSET = REGION_LAYOUT.byteOffset(
        MemoryLayout.PathElement.groupElement("num_regs")
    );
    static final long BEG_OFFSET = REGION_LAYOUT.byteOffset(
        MemoryLayout.PathElement.groupElement("beg")
    );
    static final long END_OFFSET = REGION_LAYOUT.byteOffset(
        MemoryLayout.PathElement.groupElement("end")
    );

    private Oniguruma() {
        // static
    }

    static {
        SymbolLookup lookup = loadNativeLibrary(Arena.global());
        Linker linker = Linker.nativeLinker();

        ENC_UTF8 = lookup.find("OnigEncodingUTF8").orElseThrow();
        SYNTAX_ONIGURUMA = lookup.find("OnigSyntaxOniguruma").orElseThrow();

        ONIG_NEW = downcall(linker, lookup, "onig_new", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
        );

        ONIG_SEARCH = downcall(linker, lookup, "onig_search", FunctionDescriptor.of(
            ValueLayout.JAVA_INT,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
            ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );

        ONIG_REGION_NEW = downcall(linker, lookup, "onig_region_new",
            FunctionDescriptor.of(ValueLayout.ADDRESS)
        );

        ONIG_REGION_FREE = downcall(linker, lookup, "onig_region_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
        );

        ONIG_FREE = downcall(linker, lookup, "onig_free",
            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS)
        );

        ONIG_ERROR_CODE_TO_STR = downcall(linker, lookup, "onig_error_code_to_str",
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT),
            Linker.Option.firstVariadicArg(2)
        );
    }

    /**
     * Returns a human-readable message for the given Oniguruma error code.
     *
     * @param code the Oniguruma error code
     * @return the error message
     */
    static String getErrorMessage(int code) {
        try (var arena = Arena.ofConfined()) {
            MemorySegment buf = arena.allocate(1084); // 1084 is the buffer size required by Oniguruma
            int len = (int) ONIG_ERROR_CODE_TO_STR.invokeExact(buf, code);
            return buf.reinterpret(len).getString(0);
        } catch (Throwable e) {
            return "ERROR_CODE=" + code;
        }
    }

    //*************************************************************************

    /**
     * Creates a downcall handle for the given native function.
     *
     * @param linker the native linker
     * @param lookup the symbol lookup
     * @param name   the native function name
     * @param fd     the function descriptor
     * @param opts   optional linker options
     * @return the downcall handle
     */
    private static MethodHandle downcall(Linker linker,
                                         SymbolLookup lookup,
                                         String name,
                                         FunctionDescriptor fd,
                                         Linker.Option... opts) {
        MemorySegment symbol = lookup.find(name).orElseThrow(
            () -> new IllegalStateException("Missing native symbol: " + name)
        );
        return linker.downcallHandle(symbol, fd, opts);
    }

    /**
     * Loads the native Oniguruma library for the current OS and architecture.
     *
     * @param arena the arena used for library lookup
     * @return the symbol lookup for the loaded library
     */
    private static SymbolLookup loadNativeLibrary(Arena arena) {
        // allow overriding library path via system property
        String libPath = System.getProperty("oniguruma.library.path");
        if (libPath != null && !libPath.isBlank()) {
            return SymbolLookup.libraryLookup(Path.of(libPath), arena);
        }

        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        boolean isArm = arch.contains("aarch64") || arch.contains("arm64");

        String osDir;
        String libName;

        if (os.contains("linux")) {
            osDir = isArm ? "linux-aarch64" : "linux-x86_64";
            libName = "libonig.so";
        } else if (os.contains("win")) {
            osDir = isArm ? "windows-aarch64" : "windows-x86_64";
            libName = "onig.dll";
        } else if (os.contains("mac")) {
            osDir = isArm ? "macos-aarch64" : "macos-x86_64";
            libName = "libonig.dylib";
        } else {
            throw new UnsupportedOperationException(
                "Unsupported OS: %s. Use the -Doniguruma.library.path=<path> to specify the native library location."
                    .formatted(os)
            );
        }

        libPath = "/native/" + osDir + "/" + libName;

        try (InputStream in = Oniguruma.class.getResourceAsStream(libPath)) {
            if (in == null) {
                // fallback to system-installed library
                return SymbolLookup.libraryLookup(System.mapLibraryName("onig"), arena);
            }

            Path tempLib = Files.createTempFile("libonig_", "_" + libName);
            tempLib.toFile().deleteOnExit();
            Files.copy(in, tempLib, StandardCopyOption.REPLACE_EXISTING);

            SymbolLookup lookup = SymbolLookup.libraryLookup(tempLib, arena);
            if (!os.contains("win")) {
                // the mapping stays valid after the file is deleted on non-Windows systems
                Files.deleteIfExists(tempLib);
            }
            return lookup;
        } catch (Exception e) {
            LOG.log(Level.ERROR, "Error loading Oniguruma native library", e);
            throw new IllegalStateException("Error loading Oniguruma native library", e);
        }
    }
}