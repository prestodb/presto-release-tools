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

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkState;

public class MockGithubAction
        implements GithubAction
{
    private final List<Commit> commits;
    private String pullRequestRepository;
    private String listCommitsRepository;
    private PullRequest pullRequest;

    public MockGithubAction(List<Commit> commits)
    {
        this.commits = ImmutableList.copyOf(commits);
    }

    @Override
    public List<Commit> listCommits(String repository, String latest, String earliest)
    {
        this.listCommitsRepository = repository;
        return commits;
    }

    @Override
    public PullRequest createPullRequest(String repository, String baseRef, String headRef, String title, String body)
    {
        this.pullRequestRepository = repository;
        checkState(pullRequest == null, "Can only create one pull request");
        pullRequest = new PullRequest(0, title, "", body, new Actor("test"), null);
        return pullRequest;
    }

    public PullRequest getCreatedPullRequest()
    {
        return pullRequest;
    }

    public String getPullRequestRepository()
    {
        return pullRequestRepository;
    }

    public String getListCommitsRepository()
    {
        return listCommitsRepository;
    }
}
