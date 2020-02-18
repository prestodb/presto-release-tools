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

import com.facebook.airlift.configuration.Config;

import javax.validation.constraints.NotNull;

import java.util.Optional;

public class GenerateReleaseNotesConfig
{
    private Optional<String> version = Optional.empty();

    @NotNull
    public Optional<String> getVersion()
    {
        return version;
    }

    @Config("release-notes.version")
    public GenerateReleaseNotesConfig setVersion(String version)
    {
        this.version = Optional.ofNullable(version);
        return this;
    }
}
