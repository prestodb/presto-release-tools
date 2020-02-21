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
public class TestFinalizeReleaseTask
{
    private static class MockGit
            extends NoOpGit
    {
        public MockGit(GitRepository repository, CommandLogger commandLogger)
        {
            super(repository, commandLogger);
        }

        @Override
        public List<String> listUpstreamHeads(String branch)
        {
            super.listUpstreamHeads(branch);
            return ImmutableList.of("release-0.231");
        }

        @Override
        public List<String> tag()
        {
            super.tag();
            return ImmutableList.of("0.230");
        }
    }

    private final File workingDirectory;
    private final FinalizeReleaseTask task;
    private final CommandLogger commandLogger = new CommandLogger();

    public TestFinalizeReleaseTask()
            throws IOException
    {
        this.workingDirectory = createTempDir();
        this.task = new FinalizeReleaseTask(
                new MockGit(
                        GitRepository.create(
                                workingDirectory.getName(),
                                new GitRepositoryConfig().setDirectory(workingDirectory.getAbsolutePath()),
                                new GitConfig()),
                        commandLogger),
                new NoOpMaven(workingDirectory, commandLogger));
        copy(new File(getResource("pom.xml").getFile()), workingDirectory.toPath().resolve("pom.xml").toFile());
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
        task.run();
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
                        "git release:prepare -T1C -DreleaseVersion=0.231 -DdevelopmentVersion=0.231.1-SNAPSHOT -Dtag=0.231",
                        "git release:clean",
                        "git push upstream -u release-0.231:release-0.231 --tags"));
    }
}
