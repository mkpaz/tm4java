/*
 * Copyright © 2025 tm4java authors
 * Original authors (EPL-2.0): Sebastian Thomschke, Angelo Zerr (tm4e).
 * Initial code (MIT): Microsoft Corporation (vscode-textmate).
 *
 * This program is licensed under the Eclipse Public License 2.0 (EPL-2.0).
 * See https://www.eclipse.org/legal/epl-2.0/ for details.
 */

package tm4java.parser;

import static java.lang.System.Logger.Level.ERROR;
import static tm4java.internal.utils.NullSafetyHelper.castNonNull;

import java.io.ByteArrayInputStream;
import java.io.Reader;
import java.lang.System.Logger;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import org.jspecify.annotations.Nullable;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import tm4java.parser.PropertyPath.ListBasedPropertyPath;

/**
 * A JDK DOM-based implementation of the {@link TMParser} for parsing
 * PList documents into a PropertySettable hierarchical structure.
 */
public final class TMParserPList implements TMParser {

    private static final Logger LOGGER = System.getLogger(TMParserPList.class.getName());
    private static final TMParserPList INSTANCE = new TMParserPList();

    private static final String PLIST_ARRAY = "array";
    private static final String PLIST_DICT = "dict";

    private TMParserPList() {
        // singleton
    }

    public static TMParserPList instance() {
        return INSTANCE;
    }

    @Override
    public <T extends PropertySettable<?>> T parse(Reader source, ObjectFactory<T> factory) throws Exception {
        var spf = SAXParserFactory.newInstance();
        spf.setNamespaceAware(true);

        // make parser invulnerable to XXE attacks, see https://rules.sonarsource.com/java/RSPEC-2755
        spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        var saxParser = spf.newSAXParser();

        // make parser invulnerable to XXE attacks, see https://rules.sonarsource.com/java/RSPEC-2755
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxParser.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        XMLReader xmlReader = saxParser.getXMLReader();
        xmlReader.setEntityResolver((_, _) -> new InputSource(
            new ByteArrayInputStream("<?xml version='1.0' encoding='UTF-8'?>".getBytes())
        ));

        T root = factory.createRoot();

        xmlReader.setContentHandler(new ContentHandler<>(root, factory));
        xmlReader.parse(new InputSource(source));

        return root;
    }

    //*************************************************************************

    private static final class ContentHandler<T extends PropertySettable<?>> extends DefaultHandler {

        final List<ParentRef> parents = new ArrayList<>();
        final ListBasedPropertyPath path = new ListBasedPropertyPath();
        final StringBuilder text = new StringBuilder(); // captures the text content of an XML node

        final T root;
        final ObjectFactory<T> factory;

        public ContentHandler(T root, ObjectFactory<T> factory) {
            this.root = root;
            this.factory = factory;
        }

        @Override
        public void characters(char[] chars, int start, int count) {
            text.append(chars, start, count);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
            text.setLength(0);

            switch (localName) {
                case PLIST_DICT: {
                    if (parents.isEmpty()) {
                        parents.add(new ParentRef(localName, root));
                        return;
                    }
                    parents.add(new ParentRef(localName, factory.createChild(path, Map.class)));
                    break;
                }
                case PLIST_ARRAY: {
                    var newParentRef = new ParentRef(localName, factory.createChild(path, List.class));
                    parents.add(newParentRef);

                    newParentRef.nextPropertyToSet = 0;
                    path.add(newParentRef.nextPropertyToSet);
                    break;
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (localName) {
                case PLIST_ARRAY: {
                    var parentRef = parents.removeLast();

                    path.removeLast(); // removes the remaining array index from the path
                    setCurrentProperty(parentRef.parent); // register the constructed object with its parent
                    break;
                }
                case PLIST_DICT: {
                    var parentRef = parents.removeLast();

                    if (!parents.isEmpty()) {
                        setCurrentProperty(parentRef.parent); // register the constructed object with its parent
                    }
                    break;
                }
                case "key": {
                    var parentRef = parents.getLast();

                    if (!PLIST_DICT.equals(parentRef.sourceKind)) {
                        LOGGER.log(ERROR, "<key> tag can only be used inside an open <dict> element");
                        break;
                    }

                    String key = text.toString();
                    parentRef.nextPropertyToSet = key;
                    path.add(key);
                    break;
                }
                case "data", "string":
                    setCurrentProperty(text.toString());
                    break;
                case "date": // e.g. <date>2007-10-25T12:36:35Z</date>
                    try {
                        setCurrentProperty(ZonedDateTime.parse(text.toString()));
                    } catch (DateTimeParseException e) {
                        LOGGER.log(ERROR, "Failed to parse date '" + text + "'. " + e);
                    }
                    break;
                case "integer":
                    try {
                        setCurrentProperty(Integer.parseInt(text.toString()));
                    } catch (NumberFormatException e) {
                        LOGGER.log(ERROR, "Failed to parse integer '" + text + "'. " + e);
                    }
                    break;
                case "real":
                    try {
                        setCurrentProperty(Float.parseFloat(text.toString()));
                    } catch (NumberFormatException e) {
                        LOGGER.log(ERROR, "Failed to parse real as float '" + text + "'. " + e);
                    }
                    break;
                case "true":
                    setCurrentProperty(Boolean.TRUE);
                    break;
                case "false":
                    setCurrentProperty(Boolean.FALSE);
                    break;
                case "plist":
                    // ignore
                    break;
                default:
                    LOGGER.log(ERROR, "Invalid tag name: " + localName);
            }
        }

        @SuppressWarnings("unchecked")
        private void setCurrentProperty(Object value) {
            path.removeLast();
            var obj = parents.getLast();

            switch (obj.sourceKind) {
                case PLIST_ARRAY:
                    var idx = castNonNull((Integer) obj.nextPropertyToSet);
                    ((PropertySettable<Object>) obj.parent).setProperty(idx.toString(), value);

                    obj.nextPropertyToSet = idx + 1;
                    path.add(obj.nextPropertyToSet);
                    break;
                case PLIST_DICT:
                    ((PropertySettable<Object>) obj.parent).setProperty(
                        castNonNull(obj.nextPropertyToSet).toString(), value
                    );
                    break;
            }
        }
    }

    private static final class ParentRef {

        final String sourceKind;
        final PropertySettable<?> parent;

        @Nullable
        Object nextPropertyToSet;

        ParentRef(String sourceKind, PropertySettable<?> parent) {
            this.sourceKind = sourceKind;
            this.parent = parent;
        }
    }
}
