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

import com.facebook.presto.release.ForPresto;
import com.facebook.presto.release.git.GitModule;
import com.facebook.presto.release.git.GitRepositoryModule;
import com.facebook.presto.release.maven.MavenModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.airlift.airline.Command;

import javax.inject.Inject;

import java.util.List;

@Command(name = "finalize-release", description = "Finalize presto release")
public class FinalizeReleaseCommand
        extends AbstractReleaseCommand
{
    @Inject
    public PrestoRepositoryOptions repositoryOptions = new PrestoRepositoryOptions();

    @Inject
    public GitOptions gitOptions = new GitOptions();

    @Inject
    public MavenOptions mavenOptions = new MavenOptions();

    @Override
    protected List<Module> getModules()
    {
        return ImmutableList.of(
                new GitModule(),
                new GitRepositoryModule(ForPresto.class, "presto"),
                new MavenModule(ForPresto.class),
                new FinalizeReleaseModule());
    }

    @Override
    protected Class<? extends ReleaseTask> getReleaseTask()
    {
        return FinalizeReleaseTask.class;
    }
}
