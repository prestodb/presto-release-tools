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
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.io.Files.asCharSink;
import static com.google.common.io.Files.asCharSource;
import static com.google.common.io.Files.copy;
import static com.google.common.io.Files.createTempDir;
import static com.google.common.io.MoreFiles.deleteRecursively;
import static com.google.common.io.RecursiveDeleteOption.ALLOW_INSECURE;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

@Test(singleThreaded = true)
public class TestFinalizeReleaseTask
{
    private static class MockGit
            extends NoOpGit
    {
        private Optional<Consumer<Optional<String>>> checkoutAction = Optional.empty();
        private List<String> tags = ImmutableList.of();

        public MockGit(GitRepository repository, CommandLogger commandLogger)
        {
            super(repository, commandLogger);
        }

        public MockGit setCheckoutAction(Consumer<Optional<String>> checkoutAction)
        {
            this.checkoutAction = Optional.of(checkoutAction);
            return this;
        }

        public MockGit setTags(String... tags)
        {
            this.tags = ImmutableList.copyOf(tags);
            return this;
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
            return tags;
        }

        @Override
        public void checkout(Optional<String> ref, Optional<String> createBranch)
        {
            super.checkout(ref, createBranch);
            checkoutAction.ifPresent(consumer -> consumer.accept(ref));
        }
    }

    private final File workingDirectory;
    private final File pomFile;

    private CommandLogger commandLogger;
    private MockGit git;

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
        this.git = new MockGit(
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
        git.setCheckoutAction(getGitCheckoutAction("0.231-SNAPSHOT")).setTags("0.230");
        createTask(new VersionConfig()).run();
        assertCommands(commandLogger);
    }

    @Test
    public void testFinalizeReleaseExplicit()
    {
        git.setCheckoutAction(getGitCheckoutAction("0.231-SNAPSHOT")).setTags("0.230");
        createTask(new VersionConfig().setReleaseVersion("0.231")).run();
        assertCommands(commandLogger);
    }

    @Test
    public void testFinalizeReleaseHotFix()
    {
        git.setCheckoutAction(getGitCheckoutAction("0.231.1-SNAPSHOT")).setTags("0.231");
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

    private Consumer<Optional<String>> getGitCheckoutAction(String pomVersion)
    {
        return ref -> {
            if (ref.equals(Optional.of("upstream/release-0.231"))) {
                try {
                    String newPom = asCharSource(pomFile, UTF_8).read().replaceAll(
                            "<version>0\\.232-SNAPSHOT</version>",
                            format("<version>%s</version>", pomVersion));
                    asCharSink(pomFile, UTF_8).write(newPom);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        };
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
