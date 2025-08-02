/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.internal.utils;

import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

import java.lang.System.Logger.Level;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;
import tm4java.internal.grammar.Grammar;

/**
 * A utility class for cloning objects.
 */
public final class ObjectCloner {

    private static final System.Logger LOGGER = System.getLogger(Grammar.class.getName());
    private static final WeakHashMap<Class<?>, Optional<Method>> CLONE_METHODS_CACHE = new WeakHashMap<>();

    private ObjectCloner() {
        // utility class
    }

    /**
     * Performs a deep clone of the given source object.
     */
    public static <T> T deepClone(T source) {
        return deepClone(source, new IdentityHashMap<>());
    }

    //*************************************************************************

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> T deepClone(T source, Map<Object, @Nullable Object> buffer) {
        Object clone = buffer.get(source);

        if (clone != null) {
            return (T) clone;
        }

        switch (source) {
            case List<?> list -> {
                List listClone = shallowClone(list, () -> new ArrayList<>(list));
                buffer.put(list, listClone);
                listClone.replaceAll(v -> deepCloneNullable(v, buffer));
                return (T) listClone;
            }
            case Set<?> set -> {
                var setClone = (Set<@Nullable Object>) shallowClone(set, HashSet::new);
                buffer.put(set, setClone);
                setClone.clear();
                for (var e : set) {
                    setClone.add(deepCloneNullable(e, buffer));
                }
                return (T) setClone;
            }
            case Map<?, ?> map -> {
                Map mapClone = shallowClone(map, () -> new HashMap<>(map));
                buffer.put(map, mapClone);
                mapClone.replaceAll((_, v) -> deepCloneNullable(v, buffer));
                return (T) mapClone;
            }
            default -> {
            }
        }

        if (source.getClass().isArray()) {
            int len = Array.getLength(source);
            var arrayType = castNonNull(source.getClass().getComponentType());
            var arrayClone = Array.newInstance(arrayType, len);

            buffer.put(source, arrayClone);
            for (int i = 0; i < len; i++) {
                Array.set(arrayClone, i, deepCloneNullable(Array.get(source, i), buffer));
            }

            return (T) arrayClone;
        }

        var shallowClone = shallowClone(source, () -> source);
        buffer.put(source, shallowClone);

        return source;
    }

    private static @Nullable <T> T deepCloneNullable(@Nullable T source,
                                                     Map<Object, @Nullable Object> buffer) {
        if (source == null) {
            return null;
        }

        return deepClone(source, buffer);
    }

    @SuppressWarnings("unchecked")
    private static <T> T shallowClone(T source, Supplier<T> fallback) {
        if (source instanceof Cloneable) {
            try {
                var cloneMethod = CLONE_METHODS_CACHE.computeIfAbsent(source.getClass(), cls -> {
                    try {
                        return Optional.of(cls.getMethod("clone"));
                    } catch (Exception e) {
                        return Optional.empty();
                    }
                });
                if (cloneMethod.isPresent()) {
                    return (T) cloneMethod.get().invoke(source);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to clone object", e);
            }
        }

        return fallback.get();
    }
}
