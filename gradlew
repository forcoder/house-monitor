#!/bin/bash

#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
#
#   Gradle startup script for Linux/Unix
#
##############################################################################

set -e

SCRIPT_DIR="$(cd -P "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Add default JVM options here.
DEFAULT_JVM_OPTS="-Xmx64m -Xms64m"

# Set Gradle options
export GRADLE_OPTS="-Xmx4096m -Xms1024m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8"

# Use Windows Java via cmd.exe
JAVA="cmd.exe /c \"C:\\Program Files\\Eclipse Adoptium\\jdk-8.0.482.8-hotspot\\bin\\java.exe\""

# Execute Gradle
exec $JAVA $DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS -Dorg.gradle.appname=gradlew -classpath "$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"