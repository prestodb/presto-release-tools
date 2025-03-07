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

import com.facebook.airlift.http.client.HttpClient;
import com.facebook.airlift.json.JsonCodec;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.inject.Inject;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.facebook.airlift.http.client.JsonBodyGenerator.jsonBodyGenerator;
import static com.facebook.airlift.http.client.Request.Builder.preparePost;
import static com.facebook.airlift.http.client.StringResponseHandler.createStringResponseHandler;
import static com.facebook.airlift.json.JsonCodec.jsonCodec;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static java.util.UUID.randomUUID;
import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

public class GithubGraphQlAction
        implements GithubAction
{
    private static final URI GRAPHQL_API_URI = URI.create("https://api.github.com/graphql");
    private static final String LIST_COMMITS_QUERY = "{\n" +
            "    repository(owner: \"%s\", name: \"%s\") {\n" +
            "        ref(qualifiedName: \"%s\") {\n" +
            "            target {\n" +
            "                ... on Commit {\n" +
            "                    history(first: 30, after: %s) {\n" +
            "                        pageInfo {\n" +
            "                            hasNextPage\n" +
            "                            endCursor\n" +
            "                        }\n" +
            "                        edges {\n" +
            "                            node {\n" +
            "                                oid\n" +
            "                                message\n" +
            "                                author {\n" +
            "                                    name\n" +
            "                                }\n" +
            "                                associatedPullRequests(first: 10) {\n" +
            "                                    nodes {\n" +
            "                                      number\n" +
            "                                      title\n" +
            "                                      url\n" +
            "                                      bodyText\n" +
            "                                      author {\n" +
            "                                          login\n" +
            "                                      }\n" +
            "                                      mergedBy {\n" +
            "                                          ... on User {\n" +
            "                                              login\n" +
            "                                              name\n" +
            "                                          }\n" +
            "                                      }\n" +
            "                                    }\n" +
            "                                }\n" +
            "                            }\n" +
            "                        }\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";

    private static final String CREATE_PULL_REQUEST_QUERY = "mutation($pr:CreatePullRequestInput!) {\n" +
            "    createPullRequest(input:$pr) {\n" +
            "        pullRequest {\n" +
            "            number\n" +
            "            title\n" +
            "            url\n" +
            "            bodyText\n" +
            "            author {\n" +
            "                login\n" +
            "            }\n" +
            "            mergedBy {\n" +
            "                ... on User {\n" +
            "                    name\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}";

    private static final String GET_REPOSITORY_ID_QUERY = "{\n" +
            "    repository(owner: \"%s\", name: \"%s\") {\n" +
            "        id\n" +
            "    }\n" +
            "}\n";

    private final HttpClient httpClient;
    private final String user;
    private final String accessToken;

    @Inject
    public GithubGraphQlAction(
            @ForGithub HttpClient httpClient,
            GithubConfig githubConfig)
    {
        this.httpClient = requireNonNull(httpClient, "httpClient is null");
        this.user = requireNonNull(githubConfig.getUser(), "githubUser is null");
        this.accessToken = requireNonNull(githubConfig.getAccessToken(), "accessToken is null");
    }

    @Override
    public List<Commit> listCommits(String repository, String branch, String earliest)
    {
        String[] parts = repository.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository must be in format 'owner/name'");
        }

        String current = null;
        ImmutableList.Builder<Commit> commits = ImmutableList.builder();
        TypeReference<Map<String, Map<String, Map<String, Map<String, Map<String, CommitHistory>>>>>> returnType = new TypeReference<Map<String, Map<String, Map<String, Map<String, Map<String, CommitHistory>>>>>>() {};

        while (true) {
            CommitHistory history = githubApi(format(LIST_COMMITS_QUERY, parts[0], parts[1], branch, current == null ? "null" : format("\"%s\"", current)), Optional.empty(), returnType)
                    .get("data")
                    .get("repository")
                    .get("ref")
                    .get("target")
                    .get("history");
            for (Commit commit : history.getCommits()) {
                commits.add(commit);
                if (commit.getId().equals(earliest)) {
                    return commits.build();
                }
            }
            if (!history.getPageInfo().isHasNextPage()) {
                return commits.build();
            }
            current = history.getPageInfo().getEndCursor();
        }
    }

    @Override
    public PullRequest createPullRequest(String repository, String baseRef, String headRef, String title, String body)
    {
        String[] parts = repository.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Repository must be in format 'owner/name'");
        }

        TypeReference<Map<String, Map<String, Map<String, String>>>> repoIdType =
                new TypeReference<Map<String, Map<String, Map<String, String>>>>() {};

        String repoId = githubApi(
                String.format(GET_REPOSITORY_ID_QUERY, parts[0], parts[1]),
                Optional.empty(),
                repoIdType)
                .get("data")
                .get("repository")
                .get("id");

        Map<String, Object> pullRequestVariable = ImmutableMap.<String, Object>builder()
                .put("repositoryId", repoId)
                .put("baseRefName", baseRef)
                .put("headRefName", headRef)
                .put("clientMutationId", randomUUID())
                .put("maintainerCanModify", false)
                .put("title", title)
                .put("body", body)
                .build();
        String variables;
        try {
            variables = new ObjectMapper().writeValueAsString(ImmutableMap.of("pr", pullRequestVariable));
        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return githubApi(CREATE_PULL_REQUEST_QUERY, Optional.of(variables), new TypeReference<Map<String, Map<String, Map<String, PullRequest>>>>() {})
                .get("data")
                .get("createPullRequest")
                .get("pullRequest");
    }

    private <T> T githubApi(String query, Optional<String> variables, TypeReference<T> typeReference)
    {
        String body = httpClient.execute(
                preparePost()
                        .setUri(GRAPHQL_API_URI)
                        .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .addHeader(ACCEPT, APPLICATION_JSON)
                        .addHeader(AUTHORIZATION, "token " + accessToken)
                        .addHeader(USER_AGENT, "Presto")
                        .setBodyGenerator(jsonBodyGenerator(GraphQlQuery.CODEC, new GraphQlQuery(query, variables)))
                        .build(),
                createStringResponseHandler()).getBody();

        try {
            return new ObjectMapper().readValue(body, typeReference);
        }
        catch (MismatchedInputException e) {
            try {
                Map<String, Object> resp = new ObjectMapper().readValue(body, new TypeReference<Map<String, Object>>() {});
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errors = resp.get("errors") == null ? null : (List<Map<String, Object>>) resp.get("errors");
                if (errors != null) {
                    throw new RuntimeException("GraphQL error: " + errors);
                }
                else {
                    throw new RuntimeException(e);
                }
            }
            catch (IOException errorInner) {
                throw new RuntimeException(errorInner);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class GraphQlQuery
    {
        private static final JsonCodec<GraphQlQuery> CODEC = jsonCodec(GraphQlQuery.class);

        private final String query;
        private final Optional<String> variables;

        @JsonCreator
        public GraphQlQuery(
                @JsonProperty("query") String query,
                @JsonProperty("variables") Optional<String> variables)
        {
            this.query = requireNonNull(query, "query is null");
            this.variables = requireNonNull(variables, "variables is null");
        }

        @JsonProperty
        public String getQuery()
        {
            return query;
        }

        @JsonProperty
        public Optional<String> getVariables()
        {
            return variables;
        }
    }
}
