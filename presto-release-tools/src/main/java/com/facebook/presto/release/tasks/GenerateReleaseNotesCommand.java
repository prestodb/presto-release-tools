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
import com.facebook.presto.release.git.GithubActionModule;
import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.airlift.airline.Command;
import io.airlift.airline.Option;

import javax.inject.Inject;

import java.util.List;

@Command(name = "release-notes", description = "Presto release notes generator")
public class GenerateReleaseNotesCommand
        extends AbstractReleaseCommand
{
    @Option(name = "--version", title = "Release version")
    @ConfigProperty("release-notes.version")
    public String version;

    @Inject
    public PrestoRepositoryOptions repositoryOptions = new PrestoRepositoryOptions();

    @Inject
    public GitOptions gitOptions = new GitOptions();

    @Inject
    public MavenOptions mavenOptions = new MavenOptions();

    @Inject
    public GithubOptions githubOptions = new GithubOptions();

    @Override
    protected List<Module> getModules()
    {
        return ImmutableList.of(
                new GitModule(),
                new GitRepositoryModule(ForPresto.class, "presto"),
                new GithubActionModule(),
                new GenerateReleaseNotesModule());
    }

    @Override
    protected Class<? extends ReleaseTask> getReleaseTask()
    {
        return GenerateReleaseNotesTask.class;
    }
}
