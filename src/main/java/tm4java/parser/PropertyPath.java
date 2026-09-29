/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Represents the hierarchical path of a property.
 *
 * <pre>
 * /fileTypes
 * /fileTypes/0
 * /scopeName
 * /patterns/0/captures/0/name
 * /repository/constants/patterns/0/name
 * /repository/statements/patterns/3/include
 * /repository/variable/patterns/1/captures/1/name
 * </pre>
 */
public interface PropertyPath extends Iterable<Object> {

    /**
     * Returns the first element of the path.
     *
     * @return the first element; {@link String} or {@link Integer}
     * @throws NoSuchElementException if the path is empty
     */
    Object first();

    /**
     * Returns the element at the given index.
     *
     * @param index the index, 0-based
     * @return the element; {@link String} or {@link Integer}
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    Object get(int index);

    /**
     * Returns the last element of the path.
     *
     * @return the last element; {@link String} or {@link Integer}
     * @throws NoSuchElementException if the path is empty
     */
    Object last();

    /**
     * Returns the depth of the path.
     *
     * @return the total number of segments in this path
     */
    int depth();

    //*************************************************************************

    /**
     * An {@link ArrayList}-backed implementation of {@link PropertyPath}.
     */
    final class ListBasedPropertyPath extends ArrayList<Object> implements PropertyPath {

        @Serial
        private static final long serialVersionUID = 1L;

        /**
         * {@inheritDoc}
         *
         * @return the first segment in the path ({@link String} or {@link Integer})
         * @throws NoSuchElementException if this path is empty
         */
        @Override
        public Object first() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return get(0);
        }

        /**
         * {@inheritDoc}
         *
         * @param index the 0-based position of the segment
         * @return the segment at the specified index
         * @throws IndexOutOfBoundsException if {@code index} is out of range
         */
        @Override
        public Object get(int index) {
            return super.get(index);
        }

        /**
         * {@inheritDoc}
         *
         * @return the last segment in the path ({@link String} or {@link Integer})
         * @throws NoSuchElementException if this path is empty
         */
        @Override
        public Object last() {
            if (isEmpty()) {
                throw new NoSuchElementException();
            }
            return get(size() - 1);
        }

        /**
         * {@inheritDoc}
         *
         * @return the size of the underlying list representing path depth
         */
        @Override
        public int depth() {
            return size();
        }

        /**
         * Returns a read-only iterator over the path segments.
         *
         * @return an {@link Iterator} that disables {@link Iterator#remove()}
         */
        @Override
        public Iterator<Object> iterator() {
            var it = super.iterator();
            return new Iterator<>() {
                @Override
                public Object next() {
                    return it.next();
                }

                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String toString() {
            return "/" + stream().map(Object::toString).collect(Collectors.joining("/"));
        }
    }
}