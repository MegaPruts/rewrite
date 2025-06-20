package org.openrewrite.xml;

import org.openrewrite.xml.tree.Xml;

public class ElementMatcher {

    public static ElementMatcher elementMatcher(final String elementSelector) {
        return new ElementMatcher(elementSelector);
    }

    private final String[] selectorParts;
    private final String selectorTagName;
    private final String selectorValue;

    public ElementMatcher(final String elementSelector) {
        this.selectorParts = elementSelector.replace("//","").split("[\\[\\]]");
        this.selectorTagName = selectorParts[0];
        this.selectorValue = selectorParts.length > 1 ? selectorParts[1].replace("text()='", "").replace("'", "") : null;
    }

    public boolean matches(final Xml.Tag tag) {
        return tagNameMatches(tag.getName()) && tagValueMatches(tag);
    }

    private boolean tagValueMatches(final Xml.Tag tag) {
        final String tagValue = tag.getValue().orElse(null);
        return selectorValue == tagValue || selectorValue != null && selectorValue.equals(tagValue);

    }

    private boolean tagNameMatches(final String tagName) {
        return selectorTagName != null && selectorTagName.equals(tagName);
    }
}