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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class MavenVersion
{
    private static final Pattern RELEASE_VERSION_PATTERN = Pattern.compile("0\\.([1-9][0-9]*)(\\.([1-9][0-9]*))?");
    private static final Pattern SNAPSHOT_VERSION_PATTERN = Pattern.compile("0\\.([1-9][0-9]*)(\\.([1-9][0-9]*))?-SNAPSHOT");

    private final int versionNumber;
    private final Optional<Integer> minorVersionNumber;

    private MavenVersion(int versionNumber, Optional<Integer> minorVersionNumber)
    {
        checkArgument(versionNumber > 0, "Expect positive version number, found: %s", versionNumber);
        checkArgument(!minorVersionNumber.isPresent() || minorVersionNumber.get() > 0, "Expect positive minor version number, found: %s", minorVersionNumber.orElse(null));
        this.versionNumber = versionNumber;
        this.minorVersionNumber = minorVersionNumber;
    }

    public static MavenVersion fromDirectory(File directory)
    {
        checkArgument(directory.exists(), "Does not exists: %s", directory.getAbsolutePath());
        checkArgument(directory.isDirectory(), "Not a directory: %s", directory.getAbsolutePath());
        return fromPom(Paths.get(directory.getAbsolutePath(), "pom.xml").toFile());
    }

    public static MavenVersion fromPom(File file)
    {
        try {
            Map<String, Object> elements = new XmlMapper().readValue(file, new TypeReference<Map<String, Object>>() {});
            checkArgument(elements.containsKey("version"), "No version tag found in %s", file.getAbsolutePath());
            return fromSnapshotVersion((String) elements.get("version"));
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MavenVersion fromReleaseVersion(String version)
    {
        Matcher matcher = RELEASE_VERSION_PATTERN.matcher(version);
        checkArgument(matcher.matches(), "Invalid release version: %s", version);
        return new MavenVersion(parseInt(matcher.group(1)), Optional.ofNullable(matcher.group(3)).map(Integer::parseInt));
    }

    public static MavenVersion fromSnapshotVersion(String version)
    {
        Matcher matcher = SNAPSHOT_VERSION_PATTERN.matcher(version);
        checkArgument(matcher.matches(), "Invalid snapshot version: %s", version);
        return new MavenVersion(parseInt(matcher.group(1)), Optional.ofNullable(matcher.group(3)).map(Integer::parseInt));
    }

    public boolean isHotFixVersion()
    {
        return minorVersionNumber.isPresent();
    }

    public MavenVersion getMajorVersion()
    {
        return new MavenVersion(versionNumber, Optional.empty());
    }

    public MavenVersion getLastMajorVersion()
    {
        return new MavenVersion(versionNumber - 1, Optional.empty());
    }

    public MavenVersion getNextMajorVersion()
    {
        return new MavenVersion(versionNumber + 1, Optional.empty());
    }

    public MavenVersion getLastMinorVersion()
    {
        checkArgument(minorVersionNumber.isPresent(), format("No last minor version for major version: %s", toString()));
        return new MavenVersion(versionNumber, minorVersionNumber.get() == 1 ? Optional.empty() : Optional.of(minorVersionNumber.get() - 1));
    }

    public MavenVersion getNextMinorVersion()
    {
        return new MavenVersion(versionNumber, Optional.of(minorVersionNumber.orElse(0) + 1));
    }

    public String getVersion()
    {
        return format("0.%s%s", versionNumber, getMinorSuffix());
    }

    public String getSnapshotVersion()
    {
        return format("0.%s%s-SNAPSHOT", versionNumber, getMinorSuffix());
    }

    private String getMinorSuffix()
    {
        return minorVersionNumber.map(version -> "." + version).orElse("");
    }

    @Override
    public String toString()
    {
        return getVersion();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if ((obj == null) || (getClass() != obj.getClass())) {
            return false;
        }
        MavenVersion o = (MavenVersion) obj;
        return Objects.equals(versionNumber, o.versionNumber) &&
                Objects.equals(minorVersionNumber, o.minorVersionNumber);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(versionNumber, minorVersionNumber);
    }
}
