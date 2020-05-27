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
package com.facebook.presto.release.maven;

import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public abstract class AbstractMavenVersionTest
{
    private final MavenVersionFactory<?> versionFactory;

    public AbstractMavenVersionTest(MavenVersionFactory<?> versionFactory)
    {
        this.versionFactory = requireNonNull(versionFactory, "versionFactory is null");
    }

    protected void assertVersion(String version)
    {
        String snapshotVersion = version + "-SNAPSHOT";

        assertEquals(versionFactory.create(version).getVersion(), version);
        assertEquals(versionFactory.create(snapshotVersion).getVersion(), version);
        assertEquals(versionFactory.create(version).getSnapshotVersion(), snapshotVersion);
        assertEquals(versionFactory.create(snapshotVersion).getSnapshotVersion(), snapshotVersion);
    }

    protected static void assertFailed(Runnable runnable, String errorMessageRegexp)
    {
        try {
            runnable.run();
            fail("Expect exception but succeeded");
        }
        catch (RuntimeException e) {
            assertTrue(
                    Pattern.compile(errorMessageRegexp).matcher(e.getMessage()).matches(),
                    format("Error message '%s' does not match expected pattern '%s'",
                            e.getMessage(),
                            errorMessageRegexp));
        }
    }
}
