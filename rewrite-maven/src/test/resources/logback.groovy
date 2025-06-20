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
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import static ch.qos.logback.classic.Level.INFO


statusListener(ch.qos.logback.core.status.OnConsoleStatusListener)

appender("STDOUT", ch.qos.logback.core.ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    }
}

def loggerName = System.getProperty("loggerName") ?: "com.ff2.dummy.package"
def loggerLevel = Level.toLevel(System.getProperty("loggerLevel") ?: "INFO")

logger(loggerName, loggerLevel)
root(INFO, ["STDOUT"])