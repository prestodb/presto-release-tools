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

import com.facebook.presto.release.maven.MavenVersion;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.google.common.io.Files.asCharSink;
import static com.google.common.io.Files.asCharSource;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;

public class TestingGitUtil
{
    private TestingGitUtil()
    {
    }

    /**
     * Perform the version update to pom file to mimic the Git behavior of checking out the release branch from the master branch.
     *
     * @param pomFile pom.xml file to be updated
     * @param releaseVersion the expected release version on the release branch
     */
    public static Consumer<Optional<String>> getCheckoutReleaseBranchAction(File pomFile, MavenVersion releaseVersion)
    {
        return ref -> {
            if (!ref.isPresent() || !ref.get().equals(format("upstream/release-%s", releaseVersion.getMajorVersion().getVersion()))) {
                return;
            }
            try {
                String newPom = asCharSource(pomFile, UTF_8).read().replaceAll(
                        Pattern.quote(format("<version>%s</version>", releaseVersion.getNextMajorVersion().getSnapshotVersion())),
                        format("<version>%s</version>", releaseVersion.getSnapshotVersion()));
                asCharSink(pomFile, UTF_8).write(newPom);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }
}
