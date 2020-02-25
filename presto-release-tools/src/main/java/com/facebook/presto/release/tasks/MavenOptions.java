/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.release.tasks;

import io.airlift.airline.Option;

public class MavenOptions
{
    @Option(name = "--maven-executable", title = "executable", description = "Maven executable")
    @ConfigProperty("maven.executable")
    public String executable;

    @Option(name = "--maven-options", title = "options", description = "Maven options")
    @ConfigProperty("maven.options")
    public String options;
}
