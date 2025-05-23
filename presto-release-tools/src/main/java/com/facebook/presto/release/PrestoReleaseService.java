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

import com.facebook.presto.release.tasks.CheckReleaseNotesCommand;
import com.facebook.presto.release.tasks.CutReleaseCommand;
import com.facebook.presto.release.tasks.FinalizeReleaseCommand;
import com.facebook.presto.release.tasks.GenerateReleaseNotesCommand;
import io.airlift.airline.Cli;
import io.airlift.airline.Help;
import org.eclipse.jetty.util.ssl.SslContextFactory;

public class PrestoReleaseService
{
    private PrestoReleaseService()
    {
    }

    public static void main(String[] args)
    {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setExcludeProtocols("TLSv1.3");
        Cli<Runnable> parser = Cli.<Runnable>builder("release")
                .withDescription("Presto Release")
                .withDefaultCommand(Help.class)
                .withCommand(Help.class)
                .withCommand(GenerateReleaseNotesCommand.class)
                .withCommand(CheckReleaseNotesCommand.class)
                .withCommand(CutReleaseCommand.class)
                .withCommand(FinalizeReleaseCommand.class)
                .build();
        parser.parse(args).run();
    }
}
