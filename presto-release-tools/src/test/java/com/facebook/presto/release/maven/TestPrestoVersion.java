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

import org.testng.annotations.Test;

import java.io.File;
import java.util.regex.Pattern;

import static com.facebook.presto.release.maven.PrestoVersion.fromDirectory;
import static com.facebook.presto.release.maven.PrestoVersion.fromPom;
import static com.facebook.presto.release.maven.PrestoVersion.fromReleaseVersion;
import static com.facebook.presto.release.maven.PrestoVersion.fromSnapshotVersion;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class TestPrestoVersion
{
    @Test
    public void testReleaseVersion()
    {
        assertEquals(fromReleaseVersion("0.230").getVersion(), "0.230");
        assertEquals(fromReleaseVersion("0.230.1").getVersion(), "0.230.1");
        assertEquals(fromReleaseVersion("0.230.2").getVersion(), "0.230.2");
    }

    @Test
    public void testInvalidReleaseVersion()
    {
        assertFailed(() -> fromReleaseVersion("0.230-SNAPSHOT"), "Invalid release version: 0\\.230-SNAPSHOT");
        assertFailed(() -> fromReleaseVersion("0.230.0"), "Invalid release version: 0\\.230\\.0");
        assertFailed(() -> fromReleaseVersion("230"), "Invalid release version: 230");
        assertFailed(() -> fromReleaseVersion("0.0"), "Invalid release version: 0.0");
    }

    @Test
    public void testSnapshotVersion()
    {
        assertEquals(fromSnapshotVersion("0.230-SNAPSHOT").getVersion(), "0.230");
        assertEquals(fromSnapshotVersion("0.230.1-SNAPSHOT").getVersion(), "0.230.1");
        assertEquals(fromSnapshotVersion("0.230.2-SNAPSHOT").getVersion(), "0.230.2");
    }

    @Test
    public void testInvalidSnapshotVersion()
    {
        assertFailed(() -> fromSnapshotVersion("0.230"), "Invalid snapshot version: 0\\.230");
        assertFailed(() -> fromSnapshotVersion("0.230.0-SNAPSHOT"), "Invalid snapshot version: 0\\.230\\.0-SNAPSHOT");
        assertFailed(() -> fromSnapshotVersion("230-SNAPSHOT"), "Invalid snapshot version: 230-SNAPSHOT");
    }

    @Test
    public void testFromPom()
    {
        File pomFile = new File(getResource("pom.xml").getFile());
        assertEquals(fromPom(pomFile).getVersion(), "0.232");
    }

    @Test
    public void testFromDirectory()
    {
        File pomFile = new File(getResource("pom.xml").getFile()).getParentFile();
        assertEquals(fromDirectory(pomFile).getVersion(), "0.232");
    }

    @Test
    public void testVersionChange()
    {
        MavenVersion version = fromReleaseVersion("0.230");

        MavenVersion lastMajor = version.getLastMajorVersion();
        assertEquals(lastMajor.getVersion(), "0.229");
        assertEquals(lastMajor.getSnapshotVersion(), "0.229-SNAPSHOT");

        String nextMajor = version.getNextMajorVersion().getVersion();
        assertEquals(nextMajor, "0.231");
        assertEquals(version.getNextMajorVersion().getSnapshotVersion(), "0.231-SNAPSHOT");

        MavenVersion nextMinor = version.getNextMinorVersion();
        assertEquals(nextMinor.getVersion(), "0.230.1");
        assertEquals(nextMinor.getSnapshotVersion(), "0.230.1-SNAPSHOT");

        assertEquals(nextMinor.getNextMajorVersion().getVersion(), "0.231");
        assertEquals(nextMinor.getNextMajorVersion().getSnapshotVersion(), "0.231-SNAPSHOT");
        assertEquals(nextMinor.getNextMinorVersion().getVersion(), "0.230.2");
        assertEquals(nextMinor.getNextMinorVersion().getSnapshotVersion(), "0.230.2-SNAPSHOT");
    }

    private static void assertFailed(Runnable runnable, String errorMessageRegexp)
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
