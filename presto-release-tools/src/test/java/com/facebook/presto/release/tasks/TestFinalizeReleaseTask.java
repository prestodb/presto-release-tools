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

import com.facebook.presto.release.CommandLogger;
import com.facebook.presto.release.git.GitConfig;
import com.facebook.presto.release.git.GitRepository;
import com.facebook.presto.release.git.GitRepositoryConfig;
import com.facebook.presto.release.git.TestingGit;
import com.facebook.presto.release.maven.NoOpMaven;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import static com.facebook.presto.release.git.TestingGitUtil.getCheckoutReleaseBranchAction;
import static com.facebook.presto.release.maven.PrestoVersion.fromReleaseVersion;
import static com.google.common.io.Files.copy;
import static com.google.common.io.Files.createTempDir;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.io.Resources.getResource;
import static org.testng.Assert.assertEquals;

@Test(singleThreaded = true)
public class TestFinalizeReleaseTask
{
    private final File workingDirectory;
    private final File pomFile;

    private CommandLogger commandLogger;
    private TestingGit git;

    public TestFinalizeReleaseTask()
    {
        this.workingDirectory = createTempDir();
        this.pomFile = workingDirectory.toPath().resolve("pom.xml").toFile();
    }

    @BeforeMethod
    private void setup()
            throws IOException
    {
        copy(new File(getResource("pom.xml").getFile()), pomFile);
        this.commandLogger = new CommandLogger();
        this.git = new TestingGit(
                GitRepository.create(
                        workingDirectory.getName(),
                        new GitRepositoryConfig().setDirectory(workingDirectory.getAbsolutePath()),
                        new GitConfig()),
                commandLogger);
    }

    @AfterClass
    public void teardown()
            throws IOException
    {
        deleteRecursively(workingDirectory.toPath(), ALLOW_INSECURE);
    }

    @Test
    public void testFinalizeRelease()
    {
        git.setCheckoutAction(getCheckoutReleaseBranchAction(pomFile, fromReleaseVersion("0.231"))).setUpstreamHeads("0.231").setTags("0.230");
        createTask(new VersionConfig()).run();
        assertCommands(commandLogger);
    }

    @Test
    public void testFinalizeReleaseExplicit()
    {
        git.setCheckoutAction(getCheckoutReleaseBranchAction(pomFile, fromReleaseVersion("0.231"))).setUpstreamHeads("0.231").setTags("0.230");
        createTask(new VersionConfig().setReleaseVersion("0.231")).run();
        assertCommands(commandLogger);
    }

    @Test
    public void testFinalizeReleaseHotFix()
    {
        git.setCheckoutAction(getCheckoutReleaseBranchAction(pomFile, fromReleaseVersion("0.231.1"))).setUpstreamHeads("0.231").setTags("0.231");
        createTask(new VersionConfig().setReleaseVersion("0.231.1")).run();
        assertCommandsHotFix(commandLogger);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Specified release version \\(0\\.232\\) mismatches pom version \\(0\\.231\\)")
    public void testVersionMismatch()
    {
        createTask(new VersionConfig().setReleaseVersion("0.232")).run();
    }

    public FinalizeReleaseTask createTask(VersionConfig versionConfig)
    {
        return new FinalizeReleaseTask(git, new NoOpMaven(workingDirectory, commandLogger), versionConfig);
    }

    private static void assertCommands(CommandLogger commandLogger)
    {
        assertEquals(
                commandLogger.getCommands(),
                ImmutableList.of(
                        "git status -s",
                        "git checkout master",
                        "git pull --ff-only upstream master",
                        "git fetch upstream",
                        "git tag",
                        "git ls-remote --heads upstream release-0.231",
                        "git branch -D release-0.231",
                        "git checkout -b release-0.231 upstream/release-0.231",
                        "mvn release:prepare -DreleaseVersion=0.231 -DdevelopmentVersion=0.231.1-SNAPSHOT -Dtag=0.231",
                        "mvn release:clean",
                        "git push upstream -u release-0.231:release-0.231 --tags"));
    }

    private static void assertCommandsHotFix(CommandLogger commandLogger)
    {
        assertEquals(
                commandLogger.getCommands(),
                ImmutableList.of(
                        "git status -s",
                        "git checkout master",
                        "git pull --ff-only upstream master",
                        "git fetch upstream",
                        "git tag",
                        "git ls-remote --heads upstream release-0.231",
                        "git branch -D release-0.231",
                        "git checkout -b release-0.231 upstream/release-0.231",
                        "mvn release:prepare -DreleaseVersion=0.231.1 -DdevelopmentVersion=0.231.2-SNAPSHOT -Dtag=0.231.1",
                        "mvn release:clean",
                        "git push upstream -u release-0.231:release-0.231 --tags"));
    }
}
