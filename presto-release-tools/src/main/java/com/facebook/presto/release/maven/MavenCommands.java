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
package com.facebook.presto.release.maven;

import com.facebook.presto.release.AbstractCommands;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.File;
import java.util.List;

import static java.util.Arrays.asList;

public class MavenCommands
        extends AbstractCommands
        implements Maven
{
    private final List<String> options;

    public MavenCommands(MavenConfig mavenConfig, File directory)
    {
        super(mavenConfig.getExecutable(), ImmutableMap.of(), directory);
        this.options = ImmutableList.copyOf(mavenConfig.getOptions());
    }

    @Override
    public void setVersions(String version)
    {
        commandWithOptions("versions:set", "-DnewVersion=" + version);
    }

    @Override
    public void releasePrepare(String releaseVersion, String developmentVersion, String tag)
    {
        commandWithOptions(
                "release:prepare",
                "-DreleaseVersion=" + releaseVersion,
                "-DdevelopmentVersion=" + developmentVersion,
                "-Dtag=" + tag);
    }

    @Override
    public void releaseClean()
    {
        commandWithOptions("release:clean");
    }

    private String commandWithOptions(String... arguments)
    {
        return command(ImmutableList.<String>builder()
                .addAll(options)
                .addAll(asList(arguments))
                .build());
    }
}
