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

import com.facebook.airlift.log.Logger;
import com.facebook.presto.release.CommandException;
import com.facebook.presto.release.git.Git;
import com.facebook.presto.release.git.GitRepository;
import com.facebook.presto.release.maven.Maven;
import com.facebook.presto.release.maven.MavenVersion;
import com.facebook.presto.release.maven.MavenVersionFactory;
import com.facebook.presto.release.maven.PrestoVersion;

import java.io.File;
import java.util.Optional;

import static com.facebook.presto.release.ReleaseUtil.checkReleaseCut;
import static com.facebook.presto.release.ReleaseUtil.checkTags;
import static com.facebook.presto.release.ReleaseUtil.checkVersion;
import static com.facebook.presto.release.ReleaseUtil.getPomFile;
import static com.facebook.presto.release.ReleaseUtil.getReleaseBranch;
import static com.facebook.presto.release.ReleaseUtil.sanitizeRepository;
import static com.facebook.presto.release.git.Git.RemoteType.UPSTREAM;
import static com.facebook.presto.release.maven.MavenVersionUtil.getVersionFromPom;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public abstract class AbstractFinalizeReleaseTask<T extends MavenVersion>
        implements ReleaseTask
{
    private static final Logger log = Logger.get(AbstractCutReleaseTask.class);

    private final Git git;
    private final GitRepository repository;
    private final Maven maven;
    private final MavenVersionFactory<T> versionFactory;

    private final Optional<T> releaseVersion;

    public AbstractFinalizeReleaseTask(Git git, Maven maven, MavenVersionFactory<T> versionFactory, VersionConfig config)
    {
        this.git = requireNonNull(git, "git is null");
        this.repository = requireNonNull(git.getRepository(), "repository is null");
        this.maven = requireNonNull(maven, "maven is null");
        this.versionFactory = requireNonNull(versionFactory, "versionFactory is null");
        this.releaseVersion = config.getReleaseVersion().map(versionFactory::create);
    }

    protected Git getGit()
    {
        return git;
    }

    /**
     * Perform a custom update to the pom file before mvn release:prepare.
     */
    protected abstract void updatePomBeforeReleasePrepare(File pomFile, T releaseVersion);

    /**
     * Perform a custom update to the pom file after mvn release:prepare.
     */
    protected abstract void updatePomAfterReleasePrepare(File pomFile, T releaseVersion);

    @Override
    public void run()
    {
        sanitizeRepository(git);
        @SuppressWarnings("unchecked")
        T masterReleaseVersion = (T) versionFactory.create(getVersionFromPom(repository.getDirectory())).getLastMajorVersion();
        if (releaseVersion.isPresent() && !releaseVersion.get().isHotFixVersion()) {
            checkVersion(releaseVersion.get(), masterReleaseVersion);
        }
        T version = releaseVersion.orElse(masterReleaseVersion);
        checkTags(git, version);
        checkReleaseCut(git, version);

        String releaseBranch = getReleaseBranch(version);
        try {
            git.deleteBranch(releaseBranch);
        }
        catch (CommandException e) {
            // ignore
        }
        git.checkout(Optional.of(format("%s/%s", repository.getUpstreamName(), releaseBranch)), Optional.of(releaseBranch));
        if (version instanceof PrestoVersion && version.isHotFixVersion()) {
            MavenVersion branchReleaseVersion = versionFactory.create(getVersionFromPom(repository.getDirectory()));
            checkVersion(version, branchReleaseVersion);
        }

        updatePomBeforeReleasePrepare(getPomFile(repository.getDirectory()), version);
        maven.releasePrepare(version.getVersion(), version.getNextMinorVersion().getSnapshotVersion(), version.getVersion());
        maven.releaseClean();
        updatePomAfterReleasePrepare(getPomFile(repository.getDirectory()), version);

        git.push(UPSTREAM, releaseBranch, true);
        log.info("Release finalized: %s", version.getVersion());
    }
}
