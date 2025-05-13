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

import com.facebook.airlift.http.client.HttpStatus;
import com.facebook.airlift.http.client.testing.TestingHttpClient;
import com.facebook.airlift.http.client.testing.TestingResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;

public class TestGithubGraphQlAction
{
    public static String getTestResourceContent(String... path) throws IOException
    {
        return Resources.toString(Resources.getResource(Paths.get("git", path).toString()), UTF_8);
    }

    private GithubGraphQlAction createMockAction(TestingResponse... responses)
    {
        final AtomicInteger responseIndex = new AtomicInteger(0);
        TestingHttpClient httpClient = new TestingHttpClient(request -> {
            int index = responseIndex.getAndIncrement();
            return responses[index % responses.length];
        });
        GithubConfig githubConfig = new GithubConfig()
                .setUser("testUser")
                .setAccessToken("testToken");
        GithubGraphQlAction githubAction = new GithubGraphQlAction(httpClient, githubConfig);
        return githubAction;
    }

    private GithubGraphQlAction createMockAction(String...responseBodyItems)
    {
        TestingResponse[] responses = new TestingResponse[responseBodyItems.length];
        for (int i = 0; i < responseBodyItems.length; i++) {
            responses[i] = new TestingResponse(
                HttpStatus.OK,
                ImmutableListMultimap.of("Content-Type", "application/json"),
                responseBodyItems[i].getBytes());
        }
        return createMockAction(responses);
    }

    private GithubGraphQlAction createMockActionWithResources(String... responseFiles) throws IOException
    {
        String[] responses = new String[responseFiles.length];
        for (int i = 0; i < responseFiles.length; i++) {
            responses[i] = getTestResourceContent(responseFiles[i]);
        }
        return createMockAction(responses);
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "GraphQL request failed: 429 Too Many Requests")
    public void testHttpError429()
    {
        TestingResponse response = new TestingResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                ImmutableListMultimap.of("Content-Type", "text/plain"),
                "".getBytes());
        GithubGraphQlAction action = createMockAction(response);
        action.githubApi("query { test }", Optional.empty(), new TypeReference<String>() {});
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "GraphQL request failed: 400 Bad Request")
    public void testHttpError400()
    {
        TestingResponse response = new TestingResponse(
                HttpStatus.BAD_REQUEST,
                ImmutableListMultimap.of("Content-Type", "text/plain"),
                "".getBytes());
        GithubGraphQlAction action = createMockAction(response);
        action.githubApi("{ }", Optional.empty(), new TypeReference<String>() {});
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "GraphQL request failed: 500 Internal Server Error")
    public void testHttpError500()
    {
        TestingResponse response = new TestingResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ImmutableListMultimap.of("Content-Type", "text/plain"),
                "".getBytes());
        GithubGraphQlAction action = createMockAction(response);
        action.githubApi("{ }", Optional.empty(), new TypeReference<String>() {});
    }

    @Test
    public void testGithubApiSuccess()
    {
        String successResponse = "{\"data\": {\"test\": \"value\"}}";
        GithubGraphQlAction action = createMockAction(successResponse);

        Map<String, Map<String, String>> result = action.githubApi(
                "query { test }",
                Optional.empty(),
                new TypeReference<Map<String, Map<String, String>>>() {});

        assertEquals(result.get("data"), ImmutableMap.of("test", "value"));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "GraphQL error: .*")
    public void testGithubApiError()
    {
        String errorResponse = "{\"errors\": [{\"message\": \"Test error\"}]}";
        GithubGraphQlAction action = createMockAction(errorResponse);

        action.githubApi(
                "query { test }",
                Optional.empty(),
                new TypeReference<Map<String, String>>() {});
    }

    @Test
    public void testGithubApiWarning()
    {
        String warningResponse = "{\"data\": {\"test\": \"value\"}, \"extensions\": {\"warnings\": [\"Test warning\"]}}";
        GithubGraphQlAction action = createMockAction(warningResponse);

        Map<String, Map<String, String>> result = action.githubApi(
                "query { test }",
                Optional.empty(),
                new TypeReference<Map<String, Map<String, String>>>() {});

        assertEquals(result.get("data"), ImmutableMap.of("test", "value"));
    }

    @Test(expectedExceptions = RuntimeException.class, expectedExceptionsMessageRegExp = "GraphQL no data: .*")
    public void testGithubApiNoData()
    {
        String noDataResponse = "{\"extensions\": {\"warnings\": []}}";
        GithubGraphQlAction action = createMockAction(noDataResponse);

        action.githubApi(
                "query { test }",
                Optional.empty(),
                new TypeReference<Map<String, String>>() {});
    }

    @Test
    public void testCreatePullRequestSuccess() throws IOException
    {
        GithubGraphQlAction action = createMockActionWithResources("github_repository_id.json", "github_create_pr_success.json");

        PullRequest pr = action.createPullRequest("test/repo", "main", "feature", "Test PR", "Test body");
        assertEquals(pr.getId(), 12345);
        assertEquals(pr.getTitle(), "Test PR Title");
    }

    @Test
    public void testCreatePullRequestWarning() throws IOException
    {
        GithubGraphQlAction action = createMockActionWithResources("github_repository_id.json", "github_create_pr_warning.json");

        PullRequest pr = action.createPullRequest("test/repo", "main", "feature", "Test PR", "Test body");
        assertEquals(pr.getId(), 12345);
        assertEquals(pr.getTitle(), "Test PR Title");
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void testCreatePullRequestError() throws IOException
    {
        GithubGraphQlAction action = createMockActionWithResources("github_repository_id.json", "github_create_pr_error.json");
        action.createPullRequest("test/repo", "main", "feature", "Test PR", "Test body");
    }
}
