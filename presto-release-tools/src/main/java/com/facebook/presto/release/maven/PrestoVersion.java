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

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class PrestoVersion
        implements MavenVersion
{
    private static final Pattern VERSION_PATTERN = Pattern.compile("0\\.([1-9][0-9]*)(\\.([1-9][0-9]*))?(-SNAPSHOT)?");

    private final int versionNumber;
    private final Optional<Integer> minorVersionNumber;

    protected PrestoVersion(int versionNumber, Optional<Integer> minorVersionNumber)
    {
        checkArgument(versionNumber > 0, "Expect positive version number, found: %s", versionNumber);
        checkArgument(!minorVersionNumber.isPresent() || minorVersionNumber.get() > 0, "Expect positive minor version number, found: %s", minorVersionNumber.orElse(null));
        this.versionNumber = versionNumber;
        this.minorVersionNumber = minorVersionNumber;
    }

    public static PrestoVersion create(String version)
    {
        Matcher matcher = VERSION_PATTERN.matcher(version);
        checkArgument(matcher.matches(), "Invalid version: %s", version);
        return new PrestoVersion(parseInt(matcher.group(1)), Optional.ofNullable(matcher.group(3)).map(Integer::parseInt));
    }

    @Override
    public boolean isHotFixVersion()
    {
        return minorVersionNumber.isPresent();
    }

    @Override
    public MavenVersion getMajorVersion()
    {
        return new PrestoVersion(versionNumber, Optional.empty());
    }

    @Override
    public MavenVersion getLastMajorVersion()
    {
        return new PrestoVersion(versionNumber - 1, Optional.empty());
    }

    @Override
    public MavenVersion getNextMajorVersion()
    {
        return new PrestoVersion(versionNumber + 1, Optional.empty());
    }

    @Override
    public MavenVersion getLastMinorVersion()
    {
        checkArgument(minorVersionNumber.isPresent(), format("No last minor version for major version: %s", toString()));
        return new PrestoVersion(versionNumber, minorVersionNumber.get() == 1 ? Optional.empty() : Optional.of(minorVersionNumber.get() - 1));
    }

    @Override
    public MavenVersion getNextMinorVersion()
    {
        return new PrestoVersion(versionNumber, Optional.of(minorVersionNumber.orElse(0) + 1));
    }

    @Override
    public String getVersion()
    {
        return format("0.%s%s", versionNumber, minorVersionNumber.map(version -> "." + version).orElse(""));
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
        PrestoVersion o = (PrestoVersion) obj;
        return Objects.equals(versionNumber, o.versionNumber) &&
                Objects.equals(minorVersionNumber, o.minorVersionNumber);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(versionNumber, minorVersionNumber);
    }
}
