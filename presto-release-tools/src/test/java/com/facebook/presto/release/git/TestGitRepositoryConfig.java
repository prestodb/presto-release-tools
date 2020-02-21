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

import com.google.common.collect.ImmutableMap;
import org.testng.annotations.Test;

import java.util.Map;

import static com.facebook.airlift.configuration.testing.ConfigAssertions.assertFullMapping;
import static com.facebook.airlift.configuration.testing.ConfigAssertions.assertRecordedDefaults;
import static com.facebook.airlift.configuration.testing.ConfigAssertions.recordDefaults;

public class TestGitRepositoryConfig
{
    @Test
    public void testDefault()
    {
        assertRecordedDefaults(recordDefaults(GitRepositoryConfig.class)
                .setUpstreamName("upstream")
                .setOriginName("origin")
                .setDirectory(null)
                .setCheckDirectoryName(true)
                .setInitializeFromRemote(false)
                .setUpstreamRepository(null)
                .setOriginRepository(null)
                .setProtocol("https")
                .setAccessToken(null));
    }

    @Test
    public void testExplicitPropertyMappings()
    {
        Map<String, String> properties = new ImmutableMap.Builder<String, String>()
                .put("git.upstream-name", "u")
                .put("git.origin-name", "o")
                .put("git.directory", "/tmp/presto")
                .put("git.check-directory-name", "false")
                .put("git.initialize-from-remote", "true")
                .put("git.upstream-repository", "prestodb/presto")
                .put("git.origin-repository", "user/presto")
                .put("git.protocol", "ssh")
                .put("git.access-token", "abc")
                .build();
        GitRepositoryConfig expected = new GitRepositoryConfig()
                .setUpstreamName("u")
                .setOriginName("o")
                .setDirectory("/tmp/presto")
                .setCheckDirectoryName(false)
                .setInitializeFromRemote(true)
                .setUpstreamRepository("prestodb/presto")
                .setOriginRepository("user/presto")
                .setProtocol("ssh")
                .setAccessToken("abc");

        assertFullMapping(properties, expected);
    }
}
