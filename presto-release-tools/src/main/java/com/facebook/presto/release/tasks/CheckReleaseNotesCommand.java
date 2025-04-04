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

import com.google.common.collect.ImmutableList;
import com.google.inject.Module;
import io.airlift.airline.Command;

import java.util.List;

@Command(name = "check-release-notes", description = "Checks the text of a PR description to ensure the release notes can be properly parsed.")
public class CheckReleaseNotesCommand
        extends AbstractReleaseCommand
{
    @Override
    protected List<Module> getModules()
    {
        return ImmutableList.of(new CheckReleaseNotesModule());
    }

    @Override
    protected Class<? extends ReleaseTask> getReleaseTask()
    {
        return CheckReleaseNotesTask.class;
    }
}
