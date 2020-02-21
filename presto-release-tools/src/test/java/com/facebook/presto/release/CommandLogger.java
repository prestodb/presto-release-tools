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
package com.facebook.presto.release;

import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import static com.facebook.presto.release.AbstractCommands.formatCommand;

public class CommandLogger
{
    private final List<String> commands = new ArrayList<>();

    public void log(String executable, List<String> arguments)
    {
        commands.add(formatCommand(ImmutableList.<String>builder()
                .add(executable)
                .addAll(arguments)
                .build()));
    }

    public List<String> getCommands()
    {
        return ImmutableList.copyOf(commands);
    }
}
