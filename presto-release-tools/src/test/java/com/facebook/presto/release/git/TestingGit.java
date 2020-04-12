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

import com.facebook.presto.release.CommandLogger;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class TestingGit
        extends NoOpGit
{
    private Optional<Consumer<Optional<String>>> checkoutAction = Optional.empty();
    private List<String> tags = ImmutableList.of();
    private List<String> upstreamHeads = ImmutableList.of();

    public TestingGit(GitRepository repository, CommandLogger commandLogger)
    {
        super(repository, commandLogger);
    }

    public TestingGit setCheckoutAction(Consumer<Optional<String>> checkoutAction)
    {
        this.checkoutAction = Optional.of(checkoutAction);
        return this;
    }

    public TestingGit setTags(String... tags)
    {
        this.tags = ImmutableList.copyOf(tags);
        return this;
    }

    public TestingGit setUpstreamHeads(String... upstreamHeads)
    {
        this.upstreamHeads = ImmutableList.copyOf(upstreamHeads);
        return this;
    }

    @Override
    public List<String> listUpstreamHeads(String branch)
    {
        super.listUpstreamHeads(branch);
        return upstreamHeads;
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
