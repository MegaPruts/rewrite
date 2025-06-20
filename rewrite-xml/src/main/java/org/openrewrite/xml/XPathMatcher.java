/*
 * Copyright 2020 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.xml.search.FindTags;
import org.openrewrite.xml.trait.Namespaced;
import org.openrewrite.xml.tree.Xml;

/**
 * Supports a limited set of XPath expressions, specifically those documented on <a
 * href="https://www.w3schools.com/xml/xpath_syntax.asp">this page</a>. Additionally, supports `local-name()` and `namespace-uri()`
 * conditions, `and`/`or` operators, and chained conditions.
 * <p>
 * Used for checking whether a visitor's cursor meets a certain XPath expression.
 * <p>
 * The "current node" for XPath evaluation is always the root node of the document. As a result, '.' and '..' are not recognized.
 */
public class XPathMatcher {

    private static final Pattern XPATH_ELEMENT_SPLITTER = Pattern.compile("((?<=/)(?=/)|[^/\\[]|\\[[^]]*])+");
    // Regular expression to support conditional tags like `plugin[artifactId='maven-compiler-plugin']` or foo[@bar='baz']
    private static final Pattern ELEMENT_WITH_CONDITION_PATTERN = Pattern.compile("(@)?([-:\\w]+|\\*)(\\[.+])");
    private static final Pattern CONDITION_PATTERN = Pattern.compile("(\\[.*?])+?");
    private static final Pattern CONDITION_CONJUNCTION_PATTERN =
            Pattern.compile("(((local-name|namespace-uri|text)\\(\\)|(@)?([-\\w:]+|\\*))=[\"'](.*?)[\"'](\\h?(or|and)\\h?)?)+?");

    private final String xPathToMatch;
    private final boolean startsWithSlash;
    private final boolean startsWithDoubleSlash;
    private final String[] xPathParts;
    private final long tagMatchingParts;

    public static XPathMatcher xPathMatcher(String xPath) {
        return new XPathMatcher(xPath);
    }

    public static String[] xPathParts(String xPath) {
        return splitOnXPathSeparator(xPath.substring(xPath.startsWith("//") ? 2 : xPath.startsWith("/") ? 1 : 0));
    }

    public XPathMatcher(String xPathToMatch) {
        this.xPathToMatch = xPathToMatch;
        startsWithSlash = xPathToMatch.startsWith("/");
        startsWithDoubleSlash = xPathToMatch.startsWith("//");
        xPathParts = xPathParts(xPathToMatch);
        tagMatchingParts = Arrays.stream(xPathParts).filter(part -> !part.isEmpty() && !part.startsWith("@")).count();
    }

    private static String[] splitOnXPathSeparator(String input) {
        List<String> matches = new ArrayList<>();
        Matcher m = XPATH_ELEMENT_SPLITTER.matcher(input);
        while (m.find()) {
            matches.add(m.group());
        }
        return matches.toArray(new String[0]);
    }

    /**
     * Checks if the given XPath expression matches the provided cursor.
     *
     * @param cursor the cursor representing the XML document
     * @return true if the expression matches the cursor, false otherwise
     */
    public boolean matches(Cursor cursor) {
        List<Xml.Tag> pathToTheCursor = new ArrayList<>();
        for (Cursor c = cursor; c != null; c = c.getParent()) {
            if (c.getValue() instanceof Xml.Tag) {
                pathToTheCursor.add(c.getValue());
            }
        }

        if (startsWithDoubleSlash || !startsWithSlash) {
            int pathIndex = 0;
            for (int i = xPathParts.length - 1; i >= 0; i--, pathIndex++) {
                String part = xPathParts[i];

                String partWithCondition = null;
                Xml.Tag tagForCondition = null;
                boolean conditionIsBefore = false;
                if (part.endsWith("]") && i < pathToTheCursor.size()) {
                    int index = part.indexOf("[");
                    if (index < 0) {
                        return false;
                    }
                    partWithCondition = part;
                    tagForCondition = pathToTheCursor.get(pathIndex);
                } else if (i < pathToTheCursor.size() && i > 0 && xPathParts[i - 1].endsWith("]")) {
                    String partBefore = xPathParts[i - 1];
                    int index = partBefore.indexOf("[");
                    if (index < 0) {
                        return false;
                    }
                    if (!partBefore.contains("@")) {
                        conditionIsBefore = true;
                        partWithCondition = partBefore;
                        tagForCondition = pathToTheCursor.get(xPathParts.length - i);
                    }
                } else if (part.endsWith(")")) { // is xpath method
                    // TODO: implement other xpath methods
                    throw new UnsupportedOperationException("XPath methods are not supported");
                }

                String partName;
                boolean matchedCondition = false;

                Matcher matcher;
                if (tagForCondition != null && partWithCondition.endsWith("]") &&
                        (matcher = ELEMENT_WITH_CONDITION_PATTERN.matcher(partWithCondition)).matches()) {
                    String optionalPartName = matchesElementWithConditionFunction(matcher, tagForCondition, cursor);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                    matchedCondition = true;
                } else {
                    partName = null;
                }

                if (part.startsWith("@")) {
                    if (!matchedCondition) {
                        if (!(cursor.getValue() instanceof Xml.Attribute)) {
                            return false;
                        }
                        Xml.Attribute attribute = cursor.getValue();
                        if (!attribute.getKeyAsString().equals(part.substring(1)) && !"*".equals(part.substring(1))) {
                            return false;
                        }
                    }

                    pathIndex--;
                    continue;
                }

                boolean conditionNotFulfilled = tagForCondition == null ||
                        (!part.equals(partName) && !tagForCondition.getName().equals(partName));

                int idx = part.indexOf("[");
                if (idx > 0) {
                    part = part.substring(0, idx);
                }
                if (pathToTheCursor.size() < i + 1 ||
                        (!(pathToTheCursor.get(pathIndex).getName().equals(part)) && !"*".equals(part)) ||
                        conditionIsBefore && conditionNotFulfilled) {
                    return false;
                }
            }

            // we have matched the whole XPath, and it does not start with the root
            return true;
        } else {
            Collections.reverse(pathToTheCursor);

            // Deal with the two forward slashes in the expression; works, but I'm not proud of it.
            if (xPathToMatch.contains("//") && Arrays.stream(xPathParts).anyMatch(StringUtils::isBlank)) {
                int blankPartIndex = Arrays.asList(xPathParts).indexOf("");
                int doubleSlashIndex = xPathToMatch.indexOf("//");

                if (pathToTheCursor.size() > blankPartIndex && pathToTheCursor.size() >= tagMatchingParts) {
                    Xml.Tag blankPartTag = pathToTheCursor.get(blankPartIndex);
                    String part = xPathParts[blankPartIndex + 1];
                    Matcher matcher = ELEMENT_WITH_CONDITION_PATTERN.matcher(part);
                    if (matcher.matches() ?
                            matchesElementWithConditionFunction(matcher, blankPartTag, cursor) != null :
                            Objects.equals(blankPartTag.getName(), part)) {
                        if (matchesWithoutDoubleSlashesAt(cursor, doubleSlashIndex)) {
                            return true;
                        }
                        // fall-through: maybe we can skip this element and match further down
                    }
                    String newExpression = String.format(
                            // the // here allows to skip several levels of nested elements
                            "%s/%s//%s",
                            xPathToMatch.substring(0, doubleSlashIndex),
                            blankPartTag.getName(),
                            xPathToMatch.substring(doubleSlashIndex + 2)
                    );
                    return new XPathMatcher(newExpression).matches(cursor);
                } else if (pathToTheCursor.size() == tagMatchingParts) {
                    return matchesWithoutDoubleSlashesAt(cursor, doubleSlashIndex);
                }
            }

            if (tagMatchingParts > pathToTheCursor.size()) {
                return false;
            }

            for (int i = 0; i < xPathParts.length; i++) {
                String part = xPathParts[i];

                int isAttr = part.startsWith("@") ? 1 : 0;
                Xml.Tag tag = i - isAttr < pathToTheCursor.size() ? pathToTheCursor.get(i - isAttr) : null;
                String partName;
                boolean matchedCondition = false;

                Matcher matcher;
                if (tag != null && part.endsWith("]") && (matcher = ELEMENT_WITH_CONDITION_PATTERN.matcher(part)).matches()) {
                    String optionalPartName = matchesElementWithConditionFunction(matcher, tag, cursor);
                    if (optionalPartName == null) {
                        return false;
                    }
                    partName = optionalPartName;
                    matchedCondition = true;
                } else {
                    partName = part;
                }

                if (part.startsWith("@")) {
                    if (matchedCondition) {
                        return true;
                    }
                    return cursor.getValue() instanceof Xml.Attribute &&
                            (((Xml.Attribute) cursor.getValue()).getKeyAsString().equals(part.substring(1)) ||
                                    "*".equals(part.substring(1)));
                }

                if (pathToTheCursor.size() < i + 1 || (tag != null
                        && !tag.getName().equals(partName)
                        && !partName.equals("*")
                        && !"*".equals(
                        part))) {
                    return false;
                }
            }

            return cursor.getValue() instanceof Xml.Tag && pathToTheCursor.size() == xPathParts.length;
        }
    }

    private boolean matchesWithoutDoubleSlashesAt(Cursor cursor, int doubleSlashIndex) {
        String newExpression = String.format(
                "%s/%s",
                xPathToMatch.substring(0, doubleSlashIndex),
                xPathToMatch.substring(doubleSlashIndex + 2)
        );
        return new XPathMatcher(newExpression).matches(cursor);
    }

    /**
     * Checks that the given {@code tag} matches the XPath part represented by {@code matcher}.
     *
     * @param matcher an XPath part matcher for {@link #ELEMENT_WITH_CONDITION_PATTERN}
     * @param tag a tag to match
     * @param cursor the cursor we are trying to match
     * @return the element name specified before the condition of the part (either {@code tag.getName()}, {@code "*"} or an attribute name)
     *         or {@code null} if the tag did not match
     */
    private @Nullable String matchesElementWithConditionFunction(Matcher matcher, Xml.Tag tag, Cursor cursor) {
        boolean isAttributeElement = matcher.group(1) != null;
        String element = matcher.group(2);
        String allConditions = matcher.group(3);

        // Fail quickly if element name doesn't match
        if (!isAttributeElement && !tag.getName().equals(element) && !"*".equals(element)) {
            return null;
        }

        // check that all conditions match on current element
        Matcher conditions = CONDITION_PATTERN.matcher(allConditions);
        boolean stillMatchesConditions = true;
        while (conditions.find() && stillMatchesConditions) {
            String conditionGroup = conditions.group(1);
            Matcher condition = CONDITION_CONJUNCTION_PATTERN.matcher(conditionGroup);
            boolean orCondition = false;

            while (condition.find() && (stillMatchesConditions || orCondition)) {
                boolean matchCurrentCondition = false;

                boolean isAttributeCondition = condition.group(4) != null;
                String selector = isAttributeCondition ? condition.group(5) : condition.group(2);
                boolean isFunctionCondition = selector.endsWith("()");
                String value = condition.group(6);
                String conjunction = condition.group(8);
                orCondition = conjunction != null && conjunction.equals("or");

                // invalid conjunction if not 'or' or 'and'
                if (!orCondition && conjunction != null && !conjunction.equals("and")) {
                    // TODO: throw exception for invalid or unsupported XPath conjunction?
                    stillMatchesConditions = false;
                    break;
                }

                if (isAttributeCondition) { // [@attr='value'] pattern
                    for (Xml.Attribute a : tag.getAttributes()) {
                        if ((a.getKeyAsString().equals(selector) || "*".equals(selector)) && a.getValueAsString().equals(value)) {
                            matchCurrentCondition = true;
                            break;
                        }
                    }
                } else if (isFunctionCondition) { // [local-name()='name'] pattern
                    if (isAttributeElement) {
                        for (Xml.Attribute a : tag.getAttributes()) {
                            if (matchesElementAndFunction(new Cursor(cursor, a), element, selector, value)) {
                                matchCurrentCondition = true;
                                break;
                            }
                        }
                    } else {
                        matchCurrentCondition = matchesElementAndFunction(cursor, element, selector, value);
                    }
                } else { // other [] conditions
                    for (Xml.Tag t : FindTags.find(tag, selector)) {
                        if (t.getValue().map(v -> v.equals(value)).orElse(false)) {
                            matchCurrentCondition = true;
                            break;
                        }
                    }
                }
                // break condition early if first OR condition is fulfilled
                if (matchCurrentCondition && orCondition) {
                    break;
                }

                stillMatchesConditions = matchCurrentCondition;
            }
        }

        return stillMatchesConditions ? element : null;
    }

    private static boolean matchesElementAndFunction(Cursor cursor, String element, String selector, String value) {
        Namespaced namespaced = new Namespaced(cursor);
        if (!element.equals("*") && !Objects.equals(namespaced.getName().orElse(null), element)) {
            return false;
        } else if (selector.equals("local-name()")) {
            return Objects.equals(namespaced.getLocalName().orElse(null), value);
        } else if (selector.equals("namespace-uri()")) {
            Optional<String> nsUri = namespaced.getNamespaceUri();
            return nsUri.isPresent() && nsUri.get().equals(value);
        } else if (selector.equals("text()")) {
            Object cursorElement = namespaced.getCursor().getValue();
            if (cursorElement instanceof Xml.Tag) {
                Optional<String> textValue = ((Xml.Tag) cursorElement).getValue();
                return textValue.isPresent() && textValue.get().equals(value);
            }
            ;
            return false;
        }
        return false;
    }

    public List<String> tagNames() {
        return Arrays.stream(xPathParts).map(this::tagName).collect(Collectors.toList());
    }

    private String tagName(String tagName) {
        return CONDITION_PATTERN.matcher(tagName).find() ? tagName.split("\\[")[0] : tagName;
    }
}
