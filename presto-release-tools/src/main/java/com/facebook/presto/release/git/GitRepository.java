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

import com.facebook.presto.release.git.GitRepositoryConfig.Protocol;
import com.google.common.collect.ImmutableList;

import javax.annotation.PostConstruct;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.facebook.presto.release.AbstractCommands.command;
import static com.facebook.presto.release.git.GitRepositoryConfig.Protocol.HTTPS;
import static com.facebook.presto.release.git.GitRepositoryConfig.Protocol.SSH;
import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class GitRepository
{
    private static final Pattern REPOSITORY_PATTERN = Pattern.compile("\\w+/[\\w-.]+");

    private final String upstreamName;
    private final String originName;
    private final File directory;
    private final Optional<Runnable> initialization;

    private GitRepository(String upstreamName, String originName, File directory, Optional<Runnable> initialization)
    {
        this.upstreamName = requireNonNull(upstreamName, "upstreamName is null");
        this.originName = requireNonNull(originName, "originName is null");
        this.directory = requireNonNull(directory, "directory is null");
        this.initialization = requireNonNull(initialization, "initialization is null");
    }

    @PostConstruct
    public void initialize()
    {
        initialization.ifPresent(Runnable::run);
    }

    public static GitRepository create(String repositoryName, GitRepositoryConfig repositoryConfig, GitConfig gitConfig)
    {
        return repositoryConfig.isInitializeFromRemote() ? fromRemote(repositoryName, repositoryConfig, gitConfig) : fromFile(repositoryName, repositoryConfig);
    }

    private static GitRepository fromFile(String repositoryName, GitRepositoryConfig repositoryConfig)
    {
        return new GitRepository(
                repositoryConfig.getUpstreamName(),
                repositoryConfig.getOriginName(),
                getValidatedDirectoryForFileRepository(repositoryConfig, repositoryName),
                Optional.empty());
    }

    private static GitRepository fromRemote(String repositoryName, GitRepositoryConfig repositoryConfig, GitConfig gitConfig)
    {
        checkArgument(repositoryConfig.getUpstreamRepository().isPresent(), "upstreamRepository is not specified");
        String upstreamUrl = getRemoteUrl(
                repositoryConfig.getUpstreamRepository().get(),
                repositoryConfig.getProtocol(),
                repositoryConfig.getAccessToken());
        String originUrl = getRemoteUrl(
                repositoryConfig.getOriginRepository().orElse(repositoryConfig.getUpstreamRepository().get()),
                repositoryConfig.getProtocol(),
                repositoryConfig.getAccessToken());
        File gitDirectory = getValidatedDirectoryForRemoteRepository(repositoryConfig, repositoryName);
        Map<String, String> environment = GitCommands.getEnvironment(gitConfig.getSshKeyFile());

        Runnable initialization = () -> {
            command(ImmutableList.of(gitConfig.getExecutable(), "clone", originUrl, gitDirectory.getAbsolutePath()), environment, new File(System.getProperty("user.dir")));
            command(ImmutableList.of(gitConfig.getExecutable(), "remote", "add", repositoryConfig.getUpstreamName(), upstreamUrl), environment, gitDirectory);
            if (!repositoryConfig.getOriginName().equals("origin")) {
                command(ImmutableList.of(gitConfig.getExecutable(), "remote", "add", repositoryConfig.getOriginName(), originUrl), environment, gitDirectory);
            }
        };

        return new GitRepository(
                repositoryConfig.getUpstreamName(),
                repositoryConfig.getOriginName(),
                gitDirectory,
                Optional.of(initialization));
    }

    private static String getRemoteUrl(String repository, Protocol protocol, Optional<String> accessToken)
    {
        checkArgument(REPOSITORY_PATTERN.matcher(repository).matches(), "Invalid repository name: %s, expect format <USER>/<REPO>");
        checkArgument(protocol == HTTPS || protocol == SSH, "Invalid protocol: %s", protocol);

        if (protocol == HTTPS) {
            return format("https://%sgithub.com/%s.git", accessToken.map(token -> token + "@").orElse(""), repository);
        }
        return format("git@github.com:%s.git", repository);
    }

    public String getUpstreamName()
    {
        return upstreamName;
    }

    public String getOriginName()
    {
        return originName;
    }

    public File getDirectory()
    {
        return directory;
    }

    private static File getValidatedDirectoryForFileRepository(GitRepositoryConfig config, String repositoryName)
    {
        File gitDirectory = new File(config.getDirectory().orElse(System.getProperty("user.dir")));
        checkDirectoryName(config.isCheckDirectoryName(), gitDirectory, repositoryName);
        checkArgument(gitDirectory.exists(), "Does not exists: %s", gitDirectory.getAbsolutePath());
        checkArgument(gitDirectory.isDirectory(), "Not a directory: %s", gitDirectory.getAbsolutePath());
        return gitDirectory;
    }

    private static File getValidatedDirectoryForRemoteRepository(GitRepositoryConfig config, String repositoryName)
    {
        checkArgument(config.getDirectory().isPresent(), "Directory path is absent");
        File gitDirectory = new File(config.getDirectory().get());
        checkDirectoryName(config.isCheckDirectoryName(), gitDirectory, repositoryName);
        return gitDirectory;
    }

    private static void checkDirectoryName(boolean checkDirectoryName, File directory, String repositoryName)
    {
        if (checkDirectoryName) {
            checkArgument(directory.getName().equals(repositoryName), "Directory name [%s] mismatches repository name [%s]", directory.getName(), repositoryName);
        }
    }
}
