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
import com.facebook.presto.release.git.NoOpGit;
import com.facebook.presto.release.maven.NoOpMaven;
import com.google.common.collect.ImmutableList;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.google.common.io.Files.copy;
import static com.google.common.io.Files.createTempDir;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.io.Resources.getResource;
import static org.testng.Assert.assertEquals;

@Test(singleThreaded = true)
public class TestCutReleaseTask
{
    private static class MockGit
            extends NoOpGit
    {
        public MockGit(GitRepository repository, CommandLogger commandLogger)
        {
            super(repository, commandLogger);
        }

        @Override
        public List<String> tag()
        {
            super.tag();
            return ImmutableList.of("0.231");
        }
    }

    private final File workingDirectory;
    private CommandLogger commandLogger;

    public TestCutReleaseTask()
            throws IOException
    {
        this.workingDirectory = createTempDir();
        copy(new File(getResource("pom.xml").getFile()), workingDirectory.toPath().resolve("pom.xml").toFile());
    }

    @BeforeMethod
    private void reset()
    {
        this.commandLogger = new CommandLogger();
    }

    @AfterClass
    public void teardown()
            throws IOException
    {
        deleteRecursively(workingDirectory.toPath(), ALLOW_INSECURE);
    }

    @Test
    public void testCutRelease()
    {
        createTask(new VersionConfig()).run();
        assertCommands(commandLogger);
    }

    @Test
    public void testCutReleaseExplicit()
    {
        createTask(new VersionConfig().setReleaseVersion("0.232")).run();
        assertCommands(commandLogger);
    }

    @Test(expectedExceptions = IllegalStateException.class, expectedExceptionsMessageRegExp = "Specified release version \\(0\\.233\\) mismatches pom version \\(0\\.232\\)")
    public void testVersionMismatch()
    {
        createTask(new VersionConfig().setReleaseVersion("0.233")).run();
    }

    private CutReleaseTask createTask(VersionConfig versionConfig)
    {
        return new CutReleaseTask(
                new MockGit(
                        GitRepository.create(
                                workingDirectory.getName(),
                                new GitRepositoryConfig().setDirectory(workingDirectory.getAbsolutePath()),
                                new GitConfig()),
                        commandLogger),
                new NoOpMaven(workingDirectory, commandLogger),
                versionConfig);
    }

    private static void assertCommands(CommandLogger commandLogger)
    {
        assertEquals(
                commandLogger.getCommands(),
                ImmutableList.of("git status -s",
                        "git checkout master",
                        "git pull --ff-only upstream master",
                        "git fetch upstream",
                        "git tag",
                        "git ls-remote --heads upstream release-0.232",
                        "mvn versions:set -DnewVersion=0.233-SNAPSHOT",
                        "git add .",
                        "git commit -m \"Prepare for next development iteration - 0.233-SNAPSHOT\"",
                        "git push upstream -u master:master",
                        "git checkout -b release-0.232 HEAD~1",
                        "git push upstream -u release-0.232:release-0.232"));
    }
}
