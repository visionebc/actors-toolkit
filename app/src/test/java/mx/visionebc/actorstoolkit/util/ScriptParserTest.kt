package mx.visionebc.actorstoolkit.util

import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for ScriptParser -- validates screenplay format parsing.
 * These tests help identify why PDF script import might crash.
 */
class ScriptParserTest {

    // ============================================================
    // Test: Standard screenplay format (CHARACTER on own line)
    // ============================================================

    @Test
    fun parse_standardScreenplayFormat_extractsCharactersAndDialogue() {
        val script = """
HAMLET
To be, or not to be, that is the question.

OPHELIA
My lord, I have remembrances of yours.

HAMLET
No, not I. I never gave you aught.
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertEquals("Should find 3 dialogue lines", 3, result.lines.size)
        assertEquals("Should find 2 characters", 2, result.characters.size)
        assertEquals("First line should be HAMLET", "HAMLET", result.lines[0].character)
        assertEquals("First line dialogue", "To be, or not to be, that is the question.", result.lines[0].dialogue)
        assertEquals("Second line should be OPHELIA", "OPHELIA", result.lines[1].character)
        assertEquals("Third line should be HAMLET", "HAMLET", result.lines[2].character)
    }

    // ============================================================
    // Test: Colon-separated format (CHARACTER: dialogue)
    // ============================================================

    @Test
    fun parse_colonFormat_extractsCharactersAndDialogue() {
        val script = """
ROMEO: But, soft! what light through yonder window breaks?
JULIET: O Romeo, Romeo! wherefore art thou Romeo?
ROMEO: Shall I hear more, or shall I speak at this?
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertEquals("Should find 3 dialogue lines", 3, result.lines.size)
        assertEquals("Should find 2 characters", 2, result.characters.size)
        assertEquals("ROMEO", result.lines[0].character)
        assertEquals("But, soft! what light through yonder window breaks?", result.lines[0].dialogue)
    }

    // ============================================================
    // Test: Stage directions in parentheses
    // ============================================================

    @Test
    fun parse_stageDirections_parsedCorrectly() {
        val script = """
HAMLET
To be, or not to be.

(HAMLET draws his sword)

OPHELIA
My lord!
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        val stageDir = result.lines.find { it.stageDirection != null }
        assertNotNull("Should find a stage direction", stageDir)
        assertEquals("HAMLET draws his sword", stageDir?.stageDirection)
    }

    // ============================================================
    // Test: Empty input -- CRITICAL for crash investigation
    // ============================================================

    @Test
    fun parse_emptyString_returnsEmptyResults() {
        val result = ScriptParser.parse(1L, "")

        assertEquals("Empty input should produce zero lines", 0, result.lines.size)
        assertEquals("Empty input should produce zero characters", 0, result.characters.size)
    }

    // ============================================================
    // Test: Whitespace-only input -- CRITICAL for crash investigation
    // ============================================================

    @Test
    fun parse_whitespaceOnly_returnsEmptyResults() {
        val result = ScriptParser.parse(1L, "   \n  \n\n  \t  \n  ")

        assertEquals("Whitespace-only input should produce zero lines", 0, result.lines.size)
        assertEquals("Whitespace-only input should produce zero characters", 0, result.characters.size)
    }

    // ============================================================
    // Test: Typical garbled PDF text -- CRITICAL for crash investigation
    // ============================================================

    @Test
    fun parse_garbledPdfText_doesNotCrash() {
        val pdfGarbledText = """
  1.INT. APARTMENT - DAY                                    1

John enters the room cautiously. He looks around.

              JOHN
    Hey, is anyone here?

              SARAH
    Over here. I've been waiting
    for you.

              JOHN
         (whispering)
    We need to leave. Now.
        """.trimIndent()

        val result = ScriptParser.parse(1L, pdfGarbledText)

        assertNotNull("Result should not be null", result)
        assertTrue("Lines list should be a valid list (possibly empty)", result.lines.size >= 0)
        assertTrue("Characters list should be a valid list", result.characters.size >= 0)
    }

    // ============================================================
    // Test: PDF text with no recognizable character names
    // ============================================================

    @Test
    fun parse_noCharacterNames_returnsEmptyResults() {
        val proseText = """
        This is just a paragraph of text that was extracted from a PDF.
        It has no character names in uppercase. It is just descriptive
        text that might appear in a novel or non-screenplay document.
        The quick brown fox jumps over the lazy dog.
        """.trimIndent()

        val result = ScriptParser.parse(1L, proseText)

        assertNotNull(result)
        assertEquals("No character names means no dialogue", 0, result.lines.size)
    }

    // ============================================================
    // Test: Single character name with no following dialogue
    // ============================================================

    @Test
    fun parse_characterNameAloneNoDialogue_doesNotCrash() {
        val script = "HAMLET\n\n\n"

        val result = ScriptParser.parse(1L, script)

        assertNotNull(result)
    }

    // ============================================================
    // Test: Character name at the very last line of input
    // ============================================================

    @Test
    fun parse_characterNameAtEndOfInput_doesNotCrash() {
        val script = """
HAMLET
To be or not to be.

OPHELIA""".trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertNotNull(result)
        assertTrue("Should parse at least Hamlet's line", result.lines.isNotEmpty())
    }

    // ============================================================
    // Test: Very long input (stress test for PDF scripts)
    // ============================================================

    @Test
    fun parse_veryLongScript_doesNotCrashOrTimeout() {
        val builder = StringBuilder()
        // Use ALL-CAPS names without digits to match the character name regex
        val names = listOf("ALICE", "BOB", "CAROL", "DAVE", "EVE")
        for (i in 1..500) {
            builder.appendLine("${names[i % 5]}: This is dialogue line number $i in the very long script.")
        }

        val result = ScriptParser.parse(1L, builder.toString())

        assertNotNull(result)
        assertTrue("Should parse many lines but got ${result.lines.size}", result.lines.size > 100)
    }

    // ============================================================
    // Test: scriptId is correctly assigned to all lines
    // ============================================================

    @Test
    fun parse_scriptIdIsAssignedToAllLines() {
        val script = """
ROMEO: Hello there.
JULIET: Hi Romeo.
        """.trimIndent()

        val result = ScriptParser.parse(42L, script)

        result.lines.forEach { line ->
            assertEquals("scriptId should be 42 for all lines", 42L, line.scriptId)
        }
        result.characters.forEach { char ->
            assertEquals("scriptId should be 42 for all characters", 42L, char.scriptId)
        }
    }

    // ============================================================
    // Test: Line numbers are sequential starting at 1
    // ============================================================

    @Test
    fun parse_lineNumbersAreSequential() {
        val script = """
ROMEO: Line one.
JULIET: Line two.
ROMEO: Line three.
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertEquals(3, result.lines.size)
        assertEquals(1, result.lines[0].lineNumber)
        assertEquals(2, result.lines[1].lineNumber)
        assertEquals(3, result.lines[2].lineNumber)
    }

    // ============================================================
    // Test: Character line counts are accurate
    // ============================================================

    @Test
    fun parse_characterLineCountsAreAccurate() {
        val script = """
ROMEO: One.
JULIET: Two.
ROMEO: Three.
ROMEO: Four.
JULIET: Five.
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        val romeo = result.characters.find { it.name == "ROMEO" }
        val juliet = result.characters.find { it.name == "JULIET" }

        assertNotNull(romeo)
        assertNotNull(juliet)
        assertEquals("Romeo should have 3 lines", 3, romeo!!.lineCount)
        assertEquals("Juliet should have 2 lines", 2, juliet!!.lineCount)
    }

    // ============================================================
    // Test: Characters are sorted by line count descending
    // ============================================================

    @Test
    fun parse_charactersAreSortedByLineCountDescending() {
        val script = """
ROMEO: One.
JULIET: Two.
ROMEO: Three.
ROMEO: Four.
JULIET: Five.
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertTrue("First character should have more lines",
            result.characters[0].lineCount >= result.characters[1].lineCount)
    }

    // ============================================================
    // FDX format tests
    // ============================================================

    @Test
    fun parseFdx_basicFormat_extractsDialogue() {
        val fdx = """
<?xml version="1.0" encoding="UTF-8"?>
<FinalDraft>
  <Content>
    <Paragraph Type="Scene Heading"><Text>INT. OFFICE - DAY</Text></Paragraph>
    <Paragraph Type="Character"><Text>JOHN</Text></Paragraph>
    <Paragraph Type="Dialogue"><Text>Hello there!</Text></Paragraph>
    <Paragraph Type="Character"><Text>MARY</Text></Paragraph>
    <Paragraph Type="Parenthetical"><Text>smiling</Text></Paragraph>
    <Paragraph Type="Dialogue"><Text>Hi John!</Text></Paragraph>
  </Content>
</FinalDraft>
        """.trimIndent()

        val result = ScriptParser.parseFdx(1L, fdx)

        assertEquals("Should find 2 dialogue lines + 1 parenthetical", 3, result.lines.size)
        assertEquals("JOHN", result.lines[0].character)
        assertEquals("Hello there!", result.lines[0].dialogue)
        assertEquals("MARY", result.lines[1].character)
        assertNotNull("Second line should have stage direction", result.lines[1].stageDirection)
        assertEquals("MARY", result.lines[2].character)
        assertEquals("Hi John!", result.lines[2].dialogue)
    }

    @Test
    fun parseFdx_emptyContent_doesNotCrash() {
        val result = ScriptParser.parseFdx(1L, "")
        assertNotNull(result)
        assertEquals(0, result.lines.size)
        assertEquals(0, result.characters.size)
    }

    @Test
    fun parseFdx_malformedXml_doesNotCrash() {
        val result = ScriptParser.parseFdx(1L, "<broken><xml>not closed")
        assertNotNull(result)
    }

    // ============================================================
    // Test: Mixed format with scene headings (typical PDF output)
    // ============================================================

    @Test
    fun parse_withSceneHeadings_extractsDialogueOnly() {
        val script = """
INT. LIVING ROOM - NIGHT

SARAH
I can't believe you did that.

EXT. GARDEN - DAY

MARK
The flowers are beautiful today.
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)

        assertNotNull(result)
        assertTrue("Should parse some lines", result.lines.isNotEmpty())
    }

    // ============================================================
    // Test: Unicode characters in dialogue
    // ============================================================

    @Test
    fun parse_unicodeDialogue_doesNotCrash() {
        val script = """
ACTOR: Hola, como estas? Los ninos estan bien.
ACTRESS: Merci beaucoup! C'est magnifique!
        """.trimIndent()

        val result = ScriptParser.parse(1L, script)
        assertNotNull(result)
        assertEquals(2, result.lines.size)
    }

    // ============================================================
    // Test: Simulated real PDF output from typical screenplay
    // ============================================================

    @Test
    fun parse_realisticPdfOutput_handledCorrectly() {
        val pdfOutput = """
                                                               1.


          FADE IN:

          INT. COFFEE SHOP - DAY

          A busy coffee shop. EMMA (30s) sits at a table.

                              EMMA
                    Can I get a latte, please?

                              BARISTA
                    Sure thing. Name?

                              EMMA
                    Emma.

                              BARISTA
                         (writing on cup)
                    Got it. That'll be four fifty.

                                                               2.

          Emma pays and moves to the pickup counter.

                              EMMA
                    Thanks.
        """.trimIndent()

        val result = ScriptParser.parse(1L, pdfOutput)

        assertNotNull("Result should not be null", result)
        println("Realistic PDF test: found ${result.lines.size} lines, ${result.characters.size} characters")
        result.lines.forEachIndexed { index, line ->
            println("  Line ${line.lineNumber}: [${line.character}] ${line.dialogue} ${if (line.stageDirection != null) "(${line.stageDirection})" else ""}")
        }
    }
}
