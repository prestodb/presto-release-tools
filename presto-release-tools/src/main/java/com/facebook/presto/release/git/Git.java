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

import java.util.List;
import java.util.Optional;

public interface Git
{
    enum RemoteType
    {
        ORIGIN,
        UPSTREAM,
    }

    GitRepository getRepository();

    void add(String path);

    void checkout(Optional<String> ref, Optional<String> createBranch);

    void commit(String commitTitle);

    void fastForwardUpstream(String ref);

    void fetchUpstream(Optional<String> ref);

    List<String> listUpstreamHeads(String branch);

    String log(String revisionRange, String... options);

    void push(RemoteType remoteType, String branch);

    String status(String... options);

    List<String> tag();
}
