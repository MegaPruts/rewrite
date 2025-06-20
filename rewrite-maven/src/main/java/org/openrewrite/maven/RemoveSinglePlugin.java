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
package org.openrewrite.maven;

import static org.openrewrite.internal.StringUtils.*;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.xml.XPathMatcher;
import org.openrewrite.xml.tree.Xml;

import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = false)
public class RemoveSinglePlugin extends Recipe {
    private static final XPathMatcher PLUGINS_MATCHER = new XPathMatcher("//plugins");

    @Option(displayName = "XPath to container",
            description = "The xpath to reach the container holding the plugin.",
            example = "//build/pluginManagement/plugins")
    String container;

    @Option(displayName = "Plugin GAV",
            description = "GroupId:ArtifactId{:Version} the plugin to remove.",
            example = "my.company:my.artifact:1.2.3")
    String pluginGav;

    public static RemoveSinglePlugin build(final String container, final String pluginGav) {
        return new RemoveSinglePlugin(container, pluginGav);
    }

    @Override
    public String getDisplayName() {
        return "Remove Maven plugin from the given container";
    }

    @Override
    public String getDescription() {
        return "Removes a plugin from the plugin container in the pom.xml.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new RemoveDependencyVisitor();
    }

    private class RemoveDependencyVisitor extends MavenIsoVisitor<ExecutionContext> {
        @Override
        public Xml.Tag visitTag(Xml.Tag tag, ExecutionContext ctx) {
            Xml.Tag plugins = super.visitTag(tag, ctx);
            if (container().matches(getCursor())) {
                final String[] pluginGavParts = pluginGav.split(":");
                final String pluginGroupId = pluginGavParts[0];
                final String pluginArtifactId = pluginGavParts[1];
                final String pluginVersion = pluginGavParts.length > 2 ? pluginGavParts[3] : null;

                if (childValueMatches(tag, "groupId", pluginGroupId) &&
                        childValueMatches(tag, "artifactId", pluginArtifactId))
                    return null;
            }
            return plugins;
        }

        private XPathMatcher container() {
            return new XPathMatcher(container + "/plugin");
        }

        private boolean childValueMatches(Xml.Tag tag, String childValueName, String globPattern) {
            return tag.getChildValue(childValueName).map(it -> matchesGlob(it, globPattern)).orElse(false);
        }
    }
}
