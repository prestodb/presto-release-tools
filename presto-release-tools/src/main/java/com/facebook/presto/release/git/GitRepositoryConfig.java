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

import com.facebook.airlift.configuration.Config;
import com.facebook.airlift.configuration.ConfigDescription;

import javax.validation.constraints.NotNull;

import java.util.Optional;

import static com.facebook.presto.release.git.GitRepositoryConfig.Protocol.HTTPS;
import static java.util.Locale.ENGLISH;

public class GitRepositoryConfig
{
    public enum Protocol
    {
        HTTPS,
        SSH
    }

    private String upstreamName = "upstream";
    private String originName = "origin";
    private Optional<String> directory = Optional.empty();
    private boolean checkDirectoryName = true;

    private boolean initializeFromRemote;
    private Optional<String> upstreamRepository = Optional.empty();
    private Optional<String> originRepository = Optional.empty();
    private Protocol protocol = HTTPS;
    private Optional<String> accessToken = Optional.empty();

    @NotNull
    public String getUpstreamName()
    {
        return upstreamName;
    }

    @Config("git.upstream-name")
    public GitRepositoryConfig setUpstreamName(String upstreamName)
    {
        this.upstreamName = upstreamName;
        return this;
    }

    @NotNull
    public String getOriginName()
    {
        return originName;
    }

    @Config("git.origin-name")
    public GitRepositoryConfig setOriginName(String originName)
    {
        this.originName = originName;
        return this;
    }

    @NotNull
    public Optional<String> getDirectory()
    {
        return directory;
    }

    @Config("git.directory")
    public GitRepositoryConfig setDirectory(String gitDirectory)
    {
        this.directory = Optional.ofNullable(gitDirectory);
        return this;
    }

    public boolean isCheckDirectoryName()
    {
        return checkDirectoryName;
    }

    @Config("git.check-directory-name")
    public GitRepositoryConfig setCheckDirectoryName(boolean checkDirectoryName)
    {
        this.checkDirectoryName = checkDirectoryName;
        return this;
    }

    public boolean isInitializeFromRemote()
    {
        return initializeFromRemote;
    }

    @Config("git.initialize-from-remote")
    public GitRepositoryConfig setInitializeFromRemote(boolean initializeFromRemote)
    {
        this.initializeFromRemote = initializeFromRemote;
        return this;
    }

    @NotNull
    public Optional<String> getUpstreamRepository()
    {
        return upstreamRepository;
    }

    @Config("git.upstream-repository")
    @ConfigDescription("Repository name for upstream in the format of <USER_OR_ORGANIZATION>/<REPO>, e.g. prestodb/presto.")
    public GitRepositoryConfig setUpstreamRepository(String upstreamRepository)
    {
        this.upstreamRepository = Optional.ofNullable(upstreamRepository);
        return this;
    }

    @NotNull
    public Optional<String> getOriginRepository()
    {
        return originRepository;
    }

    @Config("git.origin-repository")
    @ConfigDescription("Repository name for origin in the format of <USER_OR_ORGANIZATION>/<REPO>, e.g. user/presto.")
    public GitRepositoryConfig setOriginRepository(String originRepository)
    {
        this.originRepository = Optional.ofNullable(originRepository);
        return this;
    }

    @NotNull
    public Protocol getProtocol()
    {
        return protocol;
    }

    @Config("git.protocol")
    public GitRepositoryConfig setProtocol(String protocol)
    {
        try {
            this.protocol = Protocol.valueOf(protocol.toUpperCase(ENGLISH));
        }
        catch (RuntimeException e) {
            // ignore
        }
        return this;
    }

    @NotNull
    public Optional<String> getAccessToken()
    {
        return accessToken;
    }

    @Config("git.access-token")
    public GitRepositoryConfig setAccessToken(String accessToken)
    {
        this.accessToken = Optional.ofNullable(accessToken);
        return this;
    }
}
