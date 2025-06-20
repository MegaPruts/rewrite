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
package org.openrewrite.xml;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.*;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.tree.Xml;

import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class ReplaceXmlElementTest implements RewriteTest {

    @DocumentExample
    @Test
    void replaceXmlElement() {
        rewriteRun(spec -> spec.recipe(new ReplaceXmlElement("genClient[text()='true']","//wsdlOptions", "<extraargs><extraarg>-client</extraarg></extraargs>")),
          xml("""
            <plugin>
                <execustions>
                    <configuration>
                        <wsdlOptions>
                            <wsdlOption>
                                <wsdl>${wsdl.resources}/wow-sync/WowSync.wsdl</wsdl>
                                <wsdlLocation>/wsdl/wow-sync/WowSync.wsdl</wsdlLocation>
                            </wsdlOption>
                        </wsdlOptions>
                        <genClient>true</genClient>
                        <genServer>false</genServer>
                    </configuration>
                </execustions>
            </plugin>
            """, """
            <plugin>
                <execustions>
                    <configuration>
                        <wsdlOptions>
                            <wsdlOption>
                                <wsdl>${wsdl.resources}/wow-sync/WowSync.wsdl</wsdl>
                                <wsdlLocation>/wsdl/wow-sync/WowSync.wsdl</wsdlLocation>
                            </wsdlOption>
                            <extraargs>
                                <extraarg>-client</extraarg>
                            </extraargs>
                        </wsdlOptions>
                        <genServer>false</genServer>
                    </configuration>
                </execustions>
            </plugin>
            """)
        );
    }


    @DocumentExample
    @Test
    void noReplacement_when_theValue_ofThe_tagToSearchFor_does_not_match() {
        rewriteRun(spec -> spec.recipe(new ReplaceXmlElement("genClient[text()='true']","//wsdlOption", "<extraargs><extraarg>-client</extraarg></extraargs>")),
          xml("""
            <plugin>
                <execustions>
                    <configuration>
                        <wsdlOptions>
                            <wsdlOption>
                                <genClient>false</genClient>
                                <genServer>false</genServer>
                            </wsdlOption>
                        </wsdlOptions>
                    </configuration>
                </execustions>
            </plugin>
            """)
        );
    }

    @DocumentExample
    @Test
    void only_deletion_when_no_replacement_is_specified() {
        rewriteRun(spec -> spec.recipe( ReplaceXmlElement.newInstance("wsdlOption/genServer[text()='false']")),
          xml("""
            <plugin>
                <execustions>
                    <configuration>
                        <wsdlOptions>
                            <wsdlOption>
                                <genClient>false</genClient>
                                <genServer>false</genServer>
                            </wsdlOption>
                        </wsdlOptions>
                    </configuration>
                </execustions>
            </plugin>
            """,
            """
            <plugin>
                <execustions>
                    <configuration>
                        <wsdlOptions>
                            <wsdlOption>
                                <genClient>false</genClient>
                            </wsdlOption>
                        </wsdlOptions>
                    </configuration>
                </execustions>
            </plugin>
            """
          )
        );
    }

}
