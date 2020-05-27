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

import static com.facebook.presto.release.maven.MavenVersionUtil.getVersionFromPom;
import static com.google.common.io.Resources.getResource;
import static org.testng.Assert.assertEquals;

public class TestPrestoVersion
        extends AbstractMavenVersionTest
{
    public TestPrestoVersion()
    {
        super(PrestoVersion::create);
    }

    @Test
    public void testVersion()
    {
        assertVersion("0.230");
        assertVersion("0.230.1");
        assertVersion("0.230.2");
    }

    @Test
    public void testInvalidVersion()
    {
        assertFailed(() -> PrestoVersion.create("0.230.0"), "Invalid version: 0\\.230\\.0");
        assertFailed(() -> PrestoVersion.create("230"), "Invalid version: 230");
        assertFailed(() -> PrestoVersion.create("0.0"), "Invalid version: 0.0");

        assertFailed(() -> PrestoVersion.create("0.230.0-SNAPSHOT"), "Invalid version: 0\\.230\\.0-SNAPSHOT");
        assertFailed(() -> PrestoVersion.create("230-SNAPSHOT"), "Invalid version: 230-SNAPSHOT");
    }

    @Test
    public void testFromDirectory()
    {
        File directory = new File(getResource("pom.xml").getFile()).getParentFile();
        assertEquals(PrestoVersion.create(getVersionFromPom(directory)).getVersion(), "0.232");
    }

    @Test
    public void testVersionChange()
    {
        MavenVersion version = PrestoVersion.create("0.230");

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
}
