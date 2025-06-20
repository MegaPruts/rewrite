package org.openrewrite.xml;

import static org.junit.jupiter.api.Assertions.*;
import static org.openrewrite.xml.ElementMatcher.*;

import org.junit.jupiter.api.Test;
import org.openrewrite.xml.tree.Xml;

public class ElementMatcherTest {

    @Test
    public void testMatchOnElementName() {
        assertTrue(elementMatcher("artifactId").matches(Xml.Tag.build("<artifactId/>")));
    }

    @Test
    public void testMatchOnElementNameAndElementValue() {
        assertTrue(elementMatcher("artifactId[text()='maven-compiler-plugin']").matches(Xml.Tag.build("<artifactId>maven-compiler-plugin</artifactId>")));
    }
}
