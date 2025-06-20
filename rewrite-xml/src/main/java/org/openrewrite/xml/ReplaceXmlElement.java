/*
 * Copyright 2023 the original author or authors.
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

import java.util.Optional;
import java.util.logging.Logger;

import javax.xml.xpath.XPath;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Cursor;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ReplaceXmlElement extends Recipe {
    private static final Logger LOG = Logger.getLogger(ReplaceXmlElement.class.getName());

    @Option(displayName = "partToRemove", description = "The xpath to the xml element to find.",
            example = "//wsdlOptions/genClient[text()='true']")
    final String partToRemove;

    @Option(displayName = "insertionPoint", description = "The xpath to the position where to create the xml element",
            example = "//wsdlOptions/extraargs/extraarg{-client}")
    @Nullable
    final String insertionPoint;

    @Option(displayName = "newElement", description = "The xml element to create",
            example = "<extraargs><extraarg>-client<extraarg><extraargs>")
    @Nullable
    final String newElement;

    public static Recipe newInstance(String ifExistsPath) {
        return newInstance(ifExistsPath, null, null);
    }

    public static Recipe newInstance(String ifExistsPath, String insertionPoint, String create) {
        return new ReplaceXmlElement(ifExistsPath, insertionPoint, create);
    }

    @Override
    public String getDisplayName() {
        return "Replace xml element";
    }

    @Override
    public String getDescription() {
        return "Lookup a xml element by the given xpath (ifExistPath:) and (optionaly)" + "remove it and/or create another xml element.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new XmlIsoVisitor<ExecutionContext>() {
            @Override
            public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
                if (cursorMatches(getCursor(), existingRootElementName())) {
                    doAfterVisit(new RemoveXmlTag(partToRemove, null).getVisitor());
                    if (insertionPoint != null && newElement != null) {
                        doAfterVisit(new AddOrUpdateChildTag(
                                insertionPoint,
                                newElement,
                                true).getVisitor());
                    }
                    return super.visitTag(tag, ctx);
                }
                return super.visitTag(tag, ctx);
            }
        };

    }

    private boolean cursorMatches(Cursor cursor, String elementName) {
        final XPathMatcher matcher = new XPathMatcher(elementName);
        return matcher.matches(cursor);
        //return ((Xml.Tag) cursor.getValue()).getName().equals(elementName(elementName));
    }

    private String elementName(final String elementName) {
        final String result = elementName.split("\\[")[0];
        return result;
    }

    private String existingRootElementName() {

        //        if (existingRootElementName == null) existingRootElementName = rootElementName(ifExistsPath);
        //        return existingRootElementName;

        return rootElementName(partToRemove);
    }

    private String rootElementName(String xPath) {
        return XPathMatcher.xPathParts(xPath)[0];
    }

    private Xml.Tag traverseToElement(Cursor cursor, @Nullable String elementNameToFind) {
        Cursor current = cursor.getParent();
        while (!((Xml.Tag) current.getValue()).getName().equals(elementNameToFind)) {
            current = current.getParent();
        }
        return (Xml.Tag) current.getValue();
    }

    private boolean tagMatches(Cursor cursor, String xPathToMatch) {
        return new XPathMatcher(xPathToMatch).matches(cursor);
    }

    private boolean valueMatches(Optional<String> sourceValue, String targetValue) {
        return sourceValue.isPresent() && sourceValue.get().equals(targetValue);
    }

}
