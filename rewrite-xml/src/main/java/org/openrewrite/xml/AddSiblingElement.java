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

import static org.openrewrite.xml.XPathMatcher.*;

import java.util.logging.Logger;

import org.jspecify.annotations.Nullable;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.tree.Xml;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AddSiblingElement extends Recipe {
    private static final Logger LOG = Logger.getLogger(AddSiblingElement.class.getName());

    @Option(displayName = "ParentXpath",
            description = "The xpath to the xml element to add the new element to.",
            example = "//build/pluginManagement/plugins")
    final String parentXpath;

    //@Option(displayName = "ChildSelector",
    //        description = "The selector of the child that must exist in the parent",
    //        example = "artifactId[text()='maven-compiler-plugin']")
    //@Nullable
    //final String childSelector;

    @Option(displayName = "NewElement", description = "The xml element to create",
            example = "<version>7.8.9</version>")
    @Nullable
    final String newElement;

    public static Recipe newInstance(String ifExistsPath, String create) {
        return new AddSiblingElement(ifExistsPath, create);
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
                if (xPathMatcher(parentXpath).matches(getCursor())) {
                     doAfterVisit(new AddOrUpdateChildTag(
                            parentXpath,
                            newElement,
                            true).getVisitor());
                }
                return super.visitTag(tag, ctx);
            }

        };
    }
}