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
package com.facebook.presto.release.git;

import com.facebook.presto.release.AbstractCommands;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.facebook.presto.release.git.Git.RemoteType.ORIGIN;
import static com.facebook.presto.release.git.Git.RemoteType.UPSTREAM;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public class GitCommands
        extends AbstractCommands
        implements Git
{
    private static final Splitter LINE_SPLITTER = Splitter.on(lineSeparator()).trimResults().omitEmptyStrings();

    private final GitRepository repository;

    public GitCommands(GitRepository repository, GitConfig gitConfig)
    {
        super(
                gitConfig.getExecutable(),
                getEnvironment(gitConfig.getSshKeyFile()),
                repository.getDirectory());

        this.repository = requireNonNull(repository, "repository is null");
    }

    @Override
    public GitRepository getRepository()
    {
        return repository;
    }

    @Override
    public void add(String path)
    {
        command("add", path);
    }

    @Override
    public void checkout(Optional<String> ref, Optional<String> createBranch)
    {
        ImmutableList.Builder<String> arguments = ImmutableList.<String>builder().add("checkout");
        createBranch.ifPresent(branch -> arguments.add("-b").add(branch));
        ref.ifPresent(arguments::add);
        command(arguments.build());
    }

    @Override
    public void commit(String commitTitle)
    {
        command("commit", "-m", commitTitle);
    }

    @Override
    public void deleteBranch(String branch)
    {
        command("branch", "-D", branch);
    }

    @Override
    public void fastForwardUpstream(String ref)
    {
        command("pull", "--ff-only", repository.getUpstreamName(), ref);
    }

    @Override
    public void fetchUpstream(Optional<String> ref)
    {
        ImmutableList.Builder<String> arguments = ImmutableList.<String>builder()
                .add("fetch")
                .add(repository.getUpstreamName());
        ref.ifPresent(arguments::add);
        command(arguments.build());
    }

    @Override
    public List<String> listUpstreamHeads(String branch)
    {
        return LINE_SPLITTER.splitToList(command("ls-remote", "--heads", repository.getUpstreamName(), branch));
    }

    @Override
    public String log(String revisionRange, String... options)
    {
        return command(ImmutableList.<String>builder()
                .add("log")
                .add(revisionRange)
                .addAll(asList(options))
                .build());
    }

    @Override
    public void push(RemoteType remoteType, String branch, boolean tags)
    {
        checkArgument(remoteType == ORIGIN || remoteType == UPSTREAM, "Unsupported remote type: %s", remoteType);
        ImmutableList.Builder<String> arguments = ImmutableList.<String>builder()
                .add("push")
                .add(remoteType == ORIGIN ? repository.getOriginName() : repository.getUpstreamName())
                .add("-u")
                .add(format("%s:%s", branch, branch));
        if (tags) {
            arguments.add("--tags");
        }
        command(arguments.build());
    }

    @Override
    public String status(String... options)
    {
        return command(ImmutableList.<String>builder()
                .add("status")
                .addAll(asList(options))
                .build());
    }

    @Override
    public List<String> tag()
    {
        return LINE_SPLITTER.splitToList(command("tag"));
    }

    public static Map<String, String> getEnvironment(Optional<File> sshKeyFile)
    {
        if (sshKeyFile.isPresent()) {
            checkArgument(sshKeyFile.get().exists(), "ssh key file does not exists: %s", sshKeyFile.get().getAbsolutePath());
        }
        return sshKeyFile.map(s -> ImmutableMap.of("GIT_SSH_COMMAND", format("ssh -i %s", s))).orElseGet(ImmutableMap::of);
    }
}
