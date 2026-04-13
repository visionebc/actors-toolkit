package mx.visionebc.actorstoolkit.util

import mx.visionebc.actorstoolkit.data.entity.Character
import mx.visionebc.actorstoolkit.data.entity.ScriptLine

data class ParsedScript(
    val lines: List<ScriptLine>,
    val characters: List<Character>
)

object ScriptParser {

    // Scene heading patterns
    private val sceneHeadingRegex = Regex("^(INT\\.|EXT\\.|INT/EXT\\.|I/E\\.)\\s", RegexOption.IGNORE_CASE)

    // Transition patterns
    private val transitionRegex = Regex(
        "^(FADE IN:|FADE OUT[.:]|FADE TO:|CUT TO:|DISSOLVE TO:|SMASH CUT TO:|MATCH CUT TO:|JUMP CUT TO:|WIPE TO:|TITLE CARD:|THE END)",
        RegexOption.IGNORE_CASE
    )

    // Page numbers (standalone digits, possibly with a period)
    private val pageNumberRegex = Regex("^\\d+\\.?$")

    // Continued/more markers
    private val continuedRegex = Regex("^\\(?(CONTINUED|MORE|CONT'D)\\)?\\.?$", RegexOption.IGNORE_CASE)

    // Character name: all uppercase, 2-30 chars, letters/spaces/hyphens/apostrophes only (NO dots)
    private val charNamePattern = Regex("^([A-Z][A-Z \\-']{1,29})(?:\\s*\\(.*\\))?\\s*$")

    // Colon format: CHARACTER: dialogue on same line (NO dots in name)
    private val colonPattern = Regex("^([A-Z][A-Z \\-']{1,29}):\\s*(.*)")

    /**
     * Returns true if the line is a structural screenplay element (not dialogue or character name).
     * Used to skip scene headings, transitions, page numbers, etc.
     */
    private fun isStructuralLine(line: String): Boolean {
        if (sceneHeadingRegex.containsMatchIn(line)) return true
        if (transitionRegex.containsMatchIn(line)) return true
        if (pageNumberRegex.matches(line)) return true
        if (continuedRegex.matches(line)) return true
        return false
    }

    /**
     * Validates whether a candidate character name is plausible.
     * Filters out scene headings and other all-caps lines that aren't character names.
     */
    private fun isValidCharacterName(name: String): Boolean {
        // Scene headings contain periods (INT. EXT.) — character names typically don't
        val nameWithoutPrefix = name
            .replace(Regex("^(MR|MRS|MS|DR|PROF|REV|SGT|CPT|LT|GEN|COL)\\.\\s*"), "")
        if (nameWithoutPrefix.contains('.')) return false
        // Real character names are usually 1-3 words
        val wordCount = name.split("\\s+".toRegex()).size
        if (wordCount > 4) return false
        return true
    }

    /**
     * Parse script text into structured lines.
     * Supports common screenplay formats:
     * - CHARACTER NAME (all caps) followed by dialogue
     * - Stage directions in parentheses or brackets
     */
    fun parse(scriptId: Long, rawText: String): ParsedScript {
        val lines = mutableListOf<ScriptLine>()
        val characterCounts = mutableMapOf<String, Int>()

        val textLines = rawText.lines()
        var currentCharacter = ""
        var lineNumber = 0
        var i = 0

        while (i < textLines.size) {
            val line = textLines[i].trim()

            if (line.isBlank()) {
                i++
                continue
            }

            // Skip structural lines (scene headings, transitions, page numbers)
            if (isStructuralLine(line)) {
                currentCharacter = ""  // Reset character context
                i++
                continue
            }

            // Check for stage direction (lines in parentheses or brackets)
            if ((line.startsWith("(") && line.endsWith(")")) ||
                (line.startsWith("[") && line.endsWith("]"))) {
                if (currentCharacter.isNotEmpty()) {
                    lineNumber++
                    lines.add(
                        ScriptLine(
                            scriptId = scriptId,
                            lineNumber = lineNumber,
                            character = currentCharacter,
                            dialogue = "",
                            stageDirection = line.removeSurrounding("(", ")").removeSurrounding("[", "]")
                        )
                    )
                }
                i++
                continue
            }

            // Check format: "CHARACTER: dialogue" (colon format)
            val colonMatch = colonPattern.find(line)

            // Check if this is a standalone character name (traditional screenplay format)
            val charMatch = charNamePattern.find(line)

            if (colonMatch != null) {
                val candidateName = colonMatch.groupValues[1].trim()
                if (isValidCharacterName(candidateName)) {
                    currentCharacter = candidateName
                    val dialogue = colonMatch.groupValues[2].trim()
                    if (dialogue.isNotEmpty()) {
                        lineNumber++
                        characterCounts[currentCharacter] = (characterCounts[currentCharacter] ?: 0) + 1
                        lines.add(
                            ScriptLine(
                                scriptId = scriptId,
                                lineNumber = lineNumber,
                                character = currentCharacter,
                                dialogue = dialogue
                            )
                        )
                    }
                }
            } else if (charMatch != null && i + 1 < textLines.size) {
                val candidateName = charMatch.groupValues[1].trim()

                if (isValidCharacterName(candidateName)) {
                    // Traditional screenplay format: character name on own line, dialogue follows
                    currentCharacter = candidateName
                    i++
                    val dialogueBuilder = StringBuilder()
                    while (i < textLines.size && textLines[i].trim().isNotBlank()) {
                        val dLine = textLines[i].trim()
                        // Stop if we hit another character name or structural line
                        if (charNamePattern.matches(dLine) || isStructuralLine(dLine)) {
                            i--
                            break
                        }
                        if (dialogueBuilder.isNotEmpty()) dialogueBuilder.append(" ")
                        dialogueBuilder.append(dLine)
                        i++
                    }
                    if (dialogueBuilder.isNotEmpty()) {
                        lineNumber++
                        characterCounts[currentCharacter] = (characterCounts[currentCharacter] ?: 0) + 1
                        lines.add(
                            ScriptLine(
                                scriptId = scriptId,
                                lineNumber = lineNumber,
                                character = currentCharacter,
                                dialogue = dialogueBuilder.toString()
                            )
                        )
                    }
                }
            } else if (currentCharacter.isNotEmpty()) {
                // Continuation of dialogue for current character
                lineNumber++
                characterCounts[currentCharacter] = (characterCounts[currentCharacter] ?: 0) + 1
                lines.add(
                    ScriptLine(
                        scriptId = scriptId,
                        lineNumber = lineNumber,
                        character = currentCharacter,
                        dialogue = line
                    )
                )
            }

            i++
        }

        val characters = characterCounts.map { (name, count) ->
            Character(
                scriptId = scriptId,
                name = name,
                lineCount = count
            )
        }.sortedByDescending { it.lineCount }

        return ParsedScript(lines, characters)
    }

    /**
     * Parse FDX (Final Draft) format — simplified XML extraction
     */
    fun parseFdx(scriptId: Long, xmlContent: String): ParsedScript {
        val lines = mutableListOf<ScriptLine>()
        val characterCounts = mutableMapOf<String, Int>()
        var lineNumber = 0
        var currentCharacter = ""

        val paragraphRegex = Regex("<Paragraph Type=\"([^\"]+)\"[^>]*>(.*?)</Paragraph>", RegexOption.DOT_MATCHES_ALL)
        val textRegex = Regex("<Text[^>]*>(.*?)</Text>")

        for (match in paragraphRegex.findAll(xmlContent)) {
            val type = match.groupValues[1]
            val content = match.groupValues[2]
            val textContent = textRegex.findAll(content).map { it.groupValues[1] }.joinToString("")

            if (textContent.isBlank()) continue

            when (type) {
                "Character" -> {
                    currentCharacter = textContent.trim().uppercase()
                }
                "Dialogue" -> {
                    if (currentCharacter.isNotEmpty()) {
                        lineNumber++
                        characterCounts[currentCharacter] = (characterCounts[currentCharacter] ?: 0) + 1
                        lines.add(
                            ScriptLine(
                                scriptId = scriptId,
                                lineNumber = lineNumber,
                                character = currentCharacter,
                                dialogue = textContent.trim()
                            )
                        )
                    }
                }
                "Parenthetical" -> {
                    if (currentCharacter.isNotEmpty()) {
                        lineNumber++
                        lines.add(
                            ScriptLine(
                                scriptId = scriptId,
                                lineNumber = lineNumber,
                                character = currentCharacter,
                                dialogue = "",
                                stageDirection = textContent.trim()
                            )
                        )
                    }
                }
                "Action", "Scene Heading" -> {
                    currentCharacter = ""
                }
            }
        }

        val characters = characterCounts.map { (name, count) ->
            Character(scriptId = scriptId, name = name, lineCount = count)
        }.sortedByDescending { it.lineCount }

        return ParsedScript(lines, characters)
    }
}
