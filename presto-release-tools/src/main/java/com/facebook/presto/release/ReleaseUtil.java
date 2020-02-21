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
package com.facebook.presto.release;

import com.facebook.presto.release.git.Git;
import com.facebook.presto.release.maven.MavenVersion;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;

public class ReleaseUtil
{
    private static final String RELEASE_BRANCH_PREFIX = "release-";

    private ReleaseUtil() {}

    /**
     * Check for uncommitted changes, checkout and fast-forward master, and fetch upstream branches and tags.
     */
    public static void sanitizeRepository(Git git)
    {
        checkState(git.status("-s").isEmpty(), "Uncommitted local changes are not allowed.");
        git.checkout(Optional.of("master"), Optional.empty());
        git.fastForwardUpstream("master");
        git.fetchUpstream(Optional.empty());
    }

    /**
     * Check for tag validity for the given {@code releaseVersion}.
     */
    public static void checkTags(Git git, MavenVersion releaseVersion)
    {
        List<String> tags = git.tag();
        checkState(
                tags.contains(releaseVersion.getLastVersion().getVersion()),
                "Release version is [%s], but tag [%s] is not found.",
                releaseVersion.getVersion(),
                releaseVersion.getLastVersion().getVersion());
        checkState(
                !tags.contains(releaseVersion.getVersion()),
                "Release version is [%s], but tag [%s] already exists.",
                releaseVersion.getVersion(),
                releaseVersion.getVersion());
    }

    public static File getPomFile(File directory)
    {
        File pomFile = Paths.get(directory.getAbsolutePath(), "pom.xml").toFile();
        checkState(pomFile.exists(), "pom.xml does not exists: %s", pomFile.getAbsolutePath());
        checkState(!pomFile.isDirectory(), "pom.xml is not a file: %s", pomFile.getAbsolutePath());
        return pomFile;
    }

    public static void checkReleaseNotCut(Git git, MavenVersion version)
    {
        checkState(git.listUpstreamHeads(getReleaseBranch(version)).isEmpty(), "Release %s is already cut", version.getVersion());
    }

    public static String getReleaseBranch(MavenVersion version)
    {
        return RELEASE_BRANCH_PREFIX + version.getVersion();
    }
}
