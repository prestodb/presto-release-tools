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
import com.facebook.presto.release.git.Actor;
import com.facebook.presto.release.git.PullRequest;
import com.facebook.presto.release.git.User;
import com.facebook.presto.release.tasks.GenerateReleaseNotesTask.ReleaseNoteItem;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static com.facebook.presto.release.tasks.GenerateReleaseNotesTask.NO_RELEASE_NOTE_PATTERN;
import static com.facebook.presto.release.tasks.GenerateReleaseNotesTask.RELEASE_NOTE_PATTERN;
import static com.facebook.presto.release.tasks.GenerateReleaseNotesTask.VALID_SECTION_HEADERS;
import static com.facebook.presto.release.tasks.GenerateReleaseNotesTask.extractReleaseNotes;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.Resources.getResource;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class CheckReleaseNotesTask
        implements ReleaseTask
{
    private static final Logger log = Logger.get(CheckReleaseNotesTask.class);

    private static final List<String> VALID_STARTING_VERBS = ImmutableList.of(
            "Fix",
            "Improve",
            "Add",
            "Replace",
            "Rename",
            "Remove",
            "Upgrade",
            "Downgrade",
            "Update",
            "Deprecate");

    private final String releaseNoteSectionTemplate;

    public CheckReleaseNotesTask()
    {
        try {
            this.releaseNoteSectionTemplate = Resources.toString(getResource("note_template.md"), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void run()
    {
        try {
            String prDescription = new String(toByteArray(System.in), StandardCharsets.UTF_8);
            checkReleaseNotes(prDescription);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @VisibleForTesting
    protected void checkReleaseNotes(String prDescription)
    {
        ImmutableList.Builder<Exception> errorTracker = ImmutableList.builder();
        errorTracker.addAll(verifyReleaseNoteSections(prDescription));

        PullRequest pullRequest = new PullRequest(
                0,
                "pull request release note check",
                "",
                prDescription,
                new Actor("prestodb-ci"),
                new User("prestodb-ci", "prestodb-ci"));
        Optional<List<ReleaseNoteItem>> notes = extractReleaseNotes(pullRequest);
        notes.ifPresent(releaseNotes -> releaseNotes.forEach(note -> {
            errorTracker.addAll(verifyReleaseNoteItem(note));
        }));
        if (!notes.isPresent()) {
            errorTracker.add(new Exception("Release notes not found"));
        }

        List<Exception> errors = errorTracker.build();

        if (!errors.isEmpty()) {
            errors.forEach(exception -> log.error(exception.getMessage()));
            throw new RuntimeException("Errors encountered while parsing release notes");
        }
        else {
            log.info("release notes found:%n%s", Joiner.on("\n")
                    .join(notes.get().stream()
                            .map(note -> note.getFormatted("*", 1))
                            .iterator()));
        }
    }

    public List<Exception> verifyReleaseNoteItem(ReleaseNoteItem item)
    {
        return ImmutableList.<Optional<Exception>>builder()
                .add(verify(VALID_SECTION_HEADERS.stream().anyMatch(pattern -> pattern.matcher(item.getSection()).find()),
                        format("The release note section '%s' must match one of the valid regex patterns: %s", item.getSection(), Joiner.on(",").join(VALID_SECTION_HEADERS))))
                .add(verify(VALID_STARTING_VERBS.stream().anyMatch(verb -> item.getLine().toLowerCase(ENGLISH).startsWith(verb.toLowerCase(ENGLISH))),
                        format("The release note line '%s' must start with one of the valid verbs: %s", item.getLine(), Joiner.on(",").join(VALID_STARTING_VERBS))))
                .build()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList());
    }

    public List<Exception> verifyReleaseNoteSections(String prDescription)
    {
        return ImmutableList.<Optional<Exception>>builder()
                .add(verify(!prDescription.contains(releaseNoteSectionTemplate), "The PR description may not contain the release note template."))
                .add(verify(!(RELEASE_NOTE_PATTERN.matcher(prDescription).find() && NO_RELEASE_NOTE_PATTERN.matcher(prDescription).find()),
                        "The PR description must contain only one instance of == RELEASE NOTES == or == NO RELEASE NOTE =="))
                .build()
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(toImmutableList());
    }

    public Optional<Exception> verify(boolean check, String message)
    {
        return Optional.of(check)
                .filter(b -> !b)
                .map(b -> new Exception(message));
    }
}
