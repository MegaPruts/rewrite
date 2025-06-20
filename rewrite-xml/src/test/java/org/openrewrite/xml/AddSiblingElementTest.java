/*
 * Copyright 2024 the original author or authors.
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

import static org.openrewrite.xml.Assertions.*;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;

class AddSiblingElementTest implements RewriteTest {

    @Test
    void addVersionTagToPlugin() {
        rewriteRun(spec -> spec.recipe(new AddSiblingElement(
            "//plugin[groupId='org.apache.maven.plugins' and artifactId='maven-compiler-plugin']" ,
            "<version>3.14.0</version>")),
          xml(
            """
              <?xml version="1.0" encoding="UTF-8"?>
              <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-project</artifactId>
                  <version>1.0</version>
                                      
                  <build>
                      <pluginManagement>
                          <plugins>
                              <plugin>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-compiler-plugin</artifactId>
                              </plugin>
                          </plugins>
                      </pluginManagement>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.other.plugins</groupId>
                              <artifactId>maven-resources-plugin</artifactId>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
          <?xml version="1.0" encoding="UTF-8"?>
          <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>my-project</artifactId>
              <version>1.0</version>
                                  
              <build>
                  <pluginManagement>
                      <plugins>
                          <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-compiler-plugin</artifactId>
                              <version>3.14.0</version>
                          </plugin>
                      </plugins>
                  </pluginManagement>
                  <plugins>
                      <plugin>
                          <groupId>org.apache.other.plugins</groupId>
                          <artifactId>maven-resources-plugin</artifactId>
                      </plugin>
                  </plugins>
              </build>
          </project>
          """
          )
        );
    }
}
