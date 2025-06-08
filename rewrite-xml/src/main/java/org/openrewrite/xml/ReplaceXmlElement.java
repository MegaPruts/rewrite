package org.openrewrite.xml;

import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.xml.tree.Xml;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ReplaceXmlElement extends Recipe {
    private static final Logger LOG = Logger.getLogger(ReplaceXmlElement.class.getName());

    @Option(displayName = "partToRemove", description = "The xpath to the xml element to find.", example = "//wsdlOptions/genClient[text()='true']")
    final String partToRemove;

    @Option(displayName = "insertionPoint", description = "The xpath to the position where to create the xml element", example = "//wsdlOptions/extraargs/extraarg{-client}")
    @Nullable
    final String insertionPoint;

    @Option(displayName = "newElement", description = "The xml element to create", example = "<extraargs><extraarg>-client<extraarg><extraargs>")
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
                    doAfterVisit(new AddOrUpdateChildTag(
                            insertionPoint,
                            newElement,
                            true).getVisitor());

                    return super.visitTag(tag, ctx);
                }
                return super.visitTag(tag, ctx);
            }
        };
    }

    private List<Xml.Tag> childrenAfterRemoval(Xml.Tag tag, String ifExistsPath) {
        List<String> xPathPartsToRemove = Arrays.stream(XPathMatcher.xPathParts(ifExistsPath)).filter(p -> !p.equals(existingRootElementName())).collect(Collectors.toList());

        @NotNull List<Xml.Tag> newChildren = tag.getChildren().stream().filter(c -> !xPathPartsToRemove.contains(c.getName())).collect(Collectors.toList());
        return newChildren;
    }


    private boolean cursorMatches(Cursor cursor, String elementName) {
        return ((Xml.Tag) cursor.getValue()).getName().equals(elementName);
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
