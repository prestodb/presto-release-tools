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

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.facebook.presto.release.tasks.TestGenerateReleaseNotesTask.getTestResourceContent;
import static java.lang.String.format;
import static org.testng.Assert.fail;

public class TestCheckReleaseNotesTask
{
    private static final Map<String, Boolean> releaseNoteStatus = ImmutableMap.<String, Boolean>builder()
            .put("missing_asterisk.txt", false)
            .put("missing_release_note.txt", false)
            .put("missing_section_header_1.txt", false)
            .put("missing_section_header_2.txt", false)
            .put("missing_section_header_3.txt", false)
            .put("prestissimo_header.txt", true)
            .put("no_release_note.txt", true)
            .put("no_release_notes.txt", true)
            .put("release_notes_1.txt", true)
            .put("release_notes_2.txt", true)
            .build();

    private CheckReleaseNotesTask checkReleaseNotesTask = new CheckReleaseNotesTask();

    @DataProvider(name = "releaseNotes")
    private Object[][] examplesProvider()
    {
        return releaseNoteStatus.entrySet().stream()
                .map(entry -> new Object[] {entry.getKey(), entry.getValue()})
                .toArray(Object[][]::new);
    }

    @Test(dataProvider = "releaseNotes")
    public void testCheckReleaseNotes(String resourceName, boolean expectedReleaseNoteStatus)
            throws IOException
    {
        String content = getTestResourceContent("pr", resourceName);
        try {
            checkReleaseNotesTask.checkReleaseNotes(content);
            if (!expectedReleaseNoteStatus) {
                fail("Expected failure to resolve release notes");
            }
        }
        catch (Exception e) {
            if (expectedReleaseNoteStatus) {
                fail("Expected success to resolve release notes");
            }
        }
    }

    @DataProvider(name = "validSectionHeaders")
    public Object[][] sectionHeaders()
    {
        return new Object[][] {
                {"General"},
                {"Prestissimo (Native Execution)"},
                {"Security"},
                {"Router"},
                {"JDBC Driver"},
                {"Web UI"},
                {"Sample Connector"},
                {"Verifier"},
                {"Resource Groups"},
                {"SPI"},
                {"Sample Plugin"},
                {"Documentation"},
        };
    }

    @Test(dataProvider = "validSectionHeaders")
    public void testValidReleaseNoteSections(String header)
    {
        String prDescription = format("== RELEASE NOTES ==\n\n%s\n* Add a change", header);
        checkReleaseNotesTask.checkReleaseNotes(prDescription);
    }

    @DataProvider(name = "releaseNotesFailures")
    public Object[][] releaseNotesFailures()
            throws IOException
    {
        return new Object[][] {
                // no edit to template
                {Resources.toString(Resources.getResource("note_template.md"), StandardCharsets.UTF_8)},
                // ambiguous, double release note blocks
                {"== RELEASE NOTES ==\n changes\n* Fix a thing\n\n\n== NO RELEASE NOTES =="},
                // release note vs notes
                {"== RELEASE NOTES ==\ngeneral\n* Fix a thing\n\n== RELEASE NOTE ==\ngeneral\n* Fix a thing"},
                {"== RELEASE NOTES ==\ngeneral\n* Fix a thing\n\n== RELEASE NOTES ==\ngeneral\n* Fix a thing"},
                // invalid section titles
                {"== RELEASE NOTES ==\n\ngenerel chang\n* Fix a thing"},
                {"== RELEASE NOTES ==\n\ncustom changes\n* Fix a thing"},
                // invalid note starts
                {"== RELEASE NOTES ==\n\nGeneral Changes\n* test a thing"},
                {"== RELEASE NOTES ==\n\nGeneral Changes\nFix a thing"},
                {"== RELEASE NOTES ==\n\nGeneral Changes\n* Fix a thing\n* enhance a thing"},
                // multiple sections, one invalid section title
                {"== RELEASE NOTES ==\n\nGeneral Changes\n* Fix a thing\n\nSNI changes\n* Add an SPI thing"},
                // multiple sections one invalid release note verb
                {"== RELEASE NOTES ==\n\nGeneral Changes\n* Fix a thing\n\nSPI changes\n* bump version of an SPI thing"},
                {"== RELEASE NOTES ==\n\nPrestissimo Native Execution Changes\n* Fix a thing\n\nSPI changes\n* bump version of an SPI thing"},
        };
    }

    @Test(expectedExceptions = RuntimeException.class, dataProvider = "releaseNotesFailures")
    public void testInvalidReleaseNotes(String notes)
    {
        checkReleaseNotesTask.checkReleaseNotes(notes);
    }
}
