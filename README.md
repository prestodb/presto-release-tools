# Presto Release Tools
[![Maven Central](https://img.shields.io/maven-central/v/com.facebook.presto/presto-release-tools.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:com.facebook.presto%20a:presto-release-tools)
[![Build Status](https://travis-ci.org/prestodb/presto-release-tools.svg?branch=master)](https://travis-ci.org/prestodb/presto-release-tools)

Tools to prepare Presto releases.

## Generate Release Notes
To collect and generate release notes, run the following commands in the presto repo.
```
curl -L -o /tmp/presto_release "https://oss.sonatype.org/service/local/artifact/maven/redirect?g=com.facebook.presto&a=presto-release-tools&v=LATEST&r=snapshots&c=executable&e=jar"
chmod 755 /tmp/presto_release
/tmp/presto_release release-notes --github-user <GITHUB_USER> --github-access-token <GITHUB_ACCESS_TOKEN>
```

The commands will create a local branch containing the collected release notes, and a pull request
with description populated with missing release notes, release notes summary, and a list of
commits within the release.