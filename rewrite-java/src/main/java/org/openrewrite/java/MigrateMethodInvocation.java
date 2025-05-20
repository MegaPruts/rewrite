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
package org.openrewrite.java;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.java.search.UsesMethod;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import static java.util.Objects.requireNonNull;

@Value
@EqualsAndHashCode(callSuper = false)
public class MigrateMethodInvocation extends Recipe {

    @Option(displayName = "Method pattern", description = MethodMatcher.METHOD_PATTERN_DESCRIPTION, example = "org.mockito.Matchers anyVararg()")
    String methodPattern;

    @Option(displayName = "New method name", description = "The method name that will replace the existing name.", example = "any")
    String newMethodReference;

    @Override
    public String getDisplayName() {
        return "Change method name";
    }

    @Override
    public String getDescription() {
        return "Rename a method.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(MethodMatcher.validate(methodPattern));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        requireNonNull(methodPattern);
        return Preconditions.check(new UsesMethod<>(methodPattern), new JavaIsoVisitor<ExecutionContext>() {
            private final MethodMatcher methodMatcher = new MethodMatcher(methodPattern);


            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation pMethodInvocation, ExecutionContext ctx) {
                if (!methodMatcher.matches(pMethodInvocation)) return pMethodInvocation;

                J.MethodInvocation methodInvocation = super.visitMethodInvocation(pMethodInvocation, ctx);

                String currentMethodClass = methodInvocation.getMethodType().getDeclaringType().getFullyQualifiedName();
                String newMethodClass = methodClass(newMethodReference);
                String newMethodName = methodName(newMethodReference);

                JavaType.Method type = methodInvocation.getMethodType();
                type = type
                        .withDeclaringType(JavaType.ShallowClass.build(newMethodClass))
                        .withName(newMethodName);

                maybeRemoveImport(currentMethodClass);
                maybeAddImport(newMethodClass, newMethodName);

                methodInvocation = methodInvocation
                        .withDeclaringType(type.getDeclaringType())
                        .withName(methodInvocation.getName().withSimpleName(newMethodName).withType(type))
                        .withMethodType(type);

                return methodInvocation;
            }

            private String methodClass(String newMethodName) {
                return newMethodName.split("#")[0];
            }

            private String methodName(String newMethodName) {
                return newMethodName.split("#")[1];
            }

        });

    }
}
