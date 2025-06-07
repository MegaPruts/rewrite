package org.openrewrite.xml;

import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@AllArgsConstructor
public class ReplaceXmlElement extends Recipe {
    private static final Logger LOG = Logger.getLogger(ReplaceXmlElement.class.getName());

    @Option(displayName = "ifExistsPath", description = "The xpath to the xml element to find.", example = "//wsdlOptions/frontEnd{genClient}")
    String ifExistsPath;

    @Option(displayName = "create", description = "The xpath to the xml element to create", example = "//wsdlOptions/extraargs/extraarg{-client}")
    String create;



//    public ReplaceXmlElement(String ifExistsPath, String create) {
//        super();
//        this.ifExistsPath = ifExistsPath;
//        this.create = create;
//
//        parentTag = parentTag(ifExistsPath);
//        tagToReplace = tagToReplace(ifExistsPath);
//        tagValueToReplace = tagValueToReplace(ifExistsPath);
//
//        replacementParent = replacementParent(create);
//        replacementElementName = replacementElementName(create);
//        replacementElementValue = replacementElementValue(create);
//
//    }

    private String replacementElementValue() {
 return create.split("/")[1].split("[\\[\\]]")[1];

    }

    private String replacementElementName() {
        return create.split("/")[1].split("\\[")[0];
    }

    private String replacementParent() {
return   create.split("/")[0];
    }

    private String tagToReplace() {
return  ifExistsPath.split("/")[1].split("\\[")[0];
    }

    private String tagValueToReplace() {
return ifExistsPath.split("/")[1].split("[\\[\\]]")[1];
    }

    private String parentTag() {
       return ifExistsPath.split("/")[0];
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

                if (!parentTag().equals(tag.getName())) {
                    LOG.finest("Tag to replace doesn't match %s <> %s".formatted(tag.getName(), parentTag()));
                    return super.visitTag(tag, ctx);
                }
                // LookFor the <genClient> tag
                Optional<Xml.Tag> optionalTagToReplace = tag.getChildren().stream().filter(c -> tagToReplace().equals(c.getName())).findAny();
                if (!optionalTagToReplace.isPresent()) {
                    LOG.fine("Tag to replace not found %s".formatted(tagToReplace()));
                    return super.visitTag(tag, ctx);
                }

                if (!valueMatches(optionalTagToReplace.get().getValue(), tagValueToReplace())) {
                    LOG.fine("Value to replace not found %s".formatted(tagValueToReplace()));
                    return super.visitTag(tag, ctx);
                }

                @NotNull List<Xml.Tag> newChildren = tag.getChildren().stream().filter(c -> c != optionalTagToReplace.get()).collect(Collectors.toList());
                // Build <extraargs>-client</extraarg>
                Xml.Tag replacementElement = Xml.Tag.build("<%s></%s>".formatted(replacementElementName(), replacementElementName()))
                        .withValue(replacementElementValue());


                // LookFor the <extraargs> tag. Create a new one if none exists
                Xml.Tag replacementParent = tag.getChildren().stream().filter(c -> replacementParent().equals(c.getName())).findAny().orElse(Xml.Tag.build("<%s></%s>".formatted(replacementParent(), replacementParent()))).withContent(List.of(replacementElement));

                // Add extraargs to the content
                newChildren.add(replacementParent);

                // Return new <wsdlOption> with updated children
                return tag.withContent(newChildren);
            }
        };
    }

    private boolean valueMatches(Optional<String> sourceValue, String targetValue) {
        return sourceValue.isPresent() && sourceValue.get().equals(targetValue);
    }

}
