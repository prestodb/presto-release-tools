# Presto Release Tools
[![Maven Central](https://img.shields.io/maven-central/v/com.facebook.presto/presto-release-tools.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.facebook.presto%20a:presto-release-tools)
[![Build Status](https://travis-ci.org/prestodb/presto-release-tools.svg?branch=master)](https://travis-ci.org/prestodb/presto-release-tools)

Tools to prepare Presto releases.

To download the release tools executable.
```
curl -L -o /tmp/presto_release "https://oss.sonatype.org/service/local/artifact/maven/redirect?g=com.facebook.presto&a=presto-release-tools&v=LATEST&r=snapshots&c=executable&e=jar"
chmod 755 /tmp/presto_release
```

## Cut Release

The ``cut-release`` command performs the following actions:
- Check that there is no uncommitted local changes.
- Checkout and fast-forward ``master`` branch.
- Fetch ``upstream``, including tags.
- Determine the release version.
- Check that the git tag of last version exists.
- Check that the git tag of the release version not exist.
- Check that the release has not been cut, that is, the release branch does not exist.
- Perform custom updates to the root ``pom.xml`` file.
- Bump version in ``pom.xml`` files.
- Push changes to ``master`` branch on ``upstream``.
- Create a release branch from the ``master`` branch.
- Push changes to the release branch on ``upstream``.

To cut a release from a local repo:
```
/tmp/presto_release cut-release --directory /path/to/presto
```

To cut a release with a new clone:
```
/tmp/presto_release cut-release --directory /path/to/new/presto \
--git-initialize-from-remote true --upstream-repo prestodb/presto
```

## Generate Release Notes
To collect and generate release notes:
```
/tmp/presto_release release-notes --github-user <GITHUB_USER> --github-access-token <GITHUB_ACCESS_TOKEN>
```

The commands will create a local branch containing the collected release notes, and a pull request
with description populated with missing release notes, release notes summary, and a list of
commits within the release.