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
import com.facebook.presto.release.maven.MavenVersion;

import javax.validation.constraints.NotNull;

import java.util.Optional;

public class VersionVerificationConfig
{
    private Optional<MavenVersion> expectedVersion = Optional.empty();

    @NotNull
    public Optional<MavenVersion> getExpectedVersion()
    {
        return expectedVersion;
    }

    @Config("expected-version")
    public VersionVerificationConfig setExpectedVersion(String expectedVersion)
    {
        if (expectedVersion != null) {
            try {
                this.expectedVersion = Optional.of(MavenVersion.fromReleaseVersion(expectedVersion));
            }
            catch (IllegalArgumentException e) {
                // ignore
            }
        }
        return this;
    }
}
