/*
 * Copyright 2022 the original author or authors.
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

import static org.openrewrite.maven.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

class RemoveSinglePluginTest implements RewriteTest {

    @DocumentExample
    @Test
    void removePluginFromBuild() {
        rewriteRun(
          spec -> spec.recipe(RemoveSinglePlugin.build("//build/pluginManagement/plugins", "org.apache.maven:maven-release-plugin")),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <maven-release-plugin.version>3.1.1</maven-release-plugin.version>
                </properties>

                <build>
                   <pluginManagement>
                      <plugins>
                         <plugin>
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>${maven-compiler-plugin.version}</version>
                         </plugin>
                         <plugin>
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-release-plugin</artifactId>
                            <version>${maven-release-plugin.version}</version>
                         </plugin>
                      </plugins>
                   </pluginManagement>
                </build>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <maven-release-plugin.version>3.1.1</maven-release-plugin.version>
                </properties>

                <build>
                   <pluginManagement>
                      <plugins>
                         <plugin>
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-compiler-plugin</artifactId>
                            <version>${maven-compiler-plugin.version}</version>
                         </plugin>
                      </plugins>
                   </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }

}
