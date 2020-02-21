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

import com.facebook.presto.release.CommandLogger;

import java.io.File;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class NoOpMaven
        extends MavenCommands
{
    private final CommandLogger commandLogger;

    public NoOpMaven(File directory, CommandLogger commandLogger)
    {
        super(new MavenConfig(), directory);
        this.commandLogger = requireNonNull(commandLogger, "commandLogger is null");
    }

    @Override
    protected String command(List<String> arguments)
    {
        commandLogger.log("git", arguments);
        return "";
    }
}
