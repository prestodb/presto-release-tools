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

import io.airlift.airline.Option;

public class PrestoRepositoryOptions
{
    @Option(name = "--upstream-name", title = "upstream_name", description = "Remote name for upstream repository 'presto'.")
    @ConfigProperty("presto.git.upstream-name")
    public String upstreamName;

    @Option(name = "--origin-name", title = "origin_name", description = "Remote name for origin repository 'presto'.")
    @ConfigProperty("presto.git.origin-name")
    public String originName;

    @Option(name = "--directory", title = "dir", description = "Directory path for repository 'presto'.")
    @ConfigProperty("presto.git.directory")
    public String directory;

    @Option(name = "--check-directory-name", description = "Check whether the git directory name is 'presto'.")
    @ConfigProperty("presto.git.check-directory-name")
    public Boolean checkDirectoryName;

    @Option(name = "--git-init-from-remote", description = "Initialize the local repository from remote.")
    @ConfigProperty("presto.git.initialize-from-remote")
    public Boolean initializeFromRemote;

    @Option(name = "--upstream-repo", title = "upstream", description = "Repository name for upstream in the format of <USER_OR_ORGANIZATION>/<REPO>, e.g. prestodb/presto.")
    @ConfigProperty("presto.git.upstream-repository")
    public String upstreamRepository;

    @Option(name = "--origin-repo", title = "origin", description = "Repository name for origin in the format of <USER_OR_ORGANIZATION>/<REPO>, e.g. user/presto.")
    @ConfigProperty("presto.git.origin-repository")
    public String originRepository;

    @Option(name = "--protocol", title = "protocol", description = "Git protocol, either HTTPS or SSH.")
    @ConfigProperty("presto.git.protocol")
    public String protocol;

    @Option(name = "--access-token", title = "token", description = "Github personal access token.")
    @ConfigProperty("presto.git.access-token")
    public String accessToken;
}
