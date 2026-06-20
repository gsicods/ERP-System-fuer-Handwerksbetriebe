package org.example.kalkulationsprogramm.util

import java.util.Locale

object EmailAiPostProcessor {
    @JvmStatic
    fun sanitizePlainText(input: String?): String? {
        if (input == null) return null
        var text = input.replace("\r\n", "\n").replace("\r", "\n").trim()
        if (text.isEmpty()) return ""

        text = stripCodeFences(text).trim()
        text = removeLeadingMarkdownHeadings(text).trim()
        text = removeLeadingPrefaceLines(text).trim()
        text = unwrapSymmetricQuotes(text).trim()
        text = text.replace(Regex("\n{3,}"), "\n\n")
        return stripTrailingClosings(text)
    }

    @JvmStatic
    fun stripCodeFences(value: String): String =
        value
            .replaceFirst(Regex("(?s)^\\n*```[a-zA-Z0-9_-]*\\n?"), "")
            .replaceFirst(Regex("(?s)\\n?```\\n*$"), "")

    @JvmStatic
    fun removeLeadingMarkdownHeadings(value: String): String {
        val lines = value.split("\n").toTypedArray()
        var index = 0
        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.isEmpty() || line.startsWith("#")) {
                index++
                continue
            }
            break
        }
        return joinFrom(lines, index)
    }

    @JvmStatic
    fun removeLeadingPrefaceLines(value: String): String {
        val lines = value.split("\n").toTypedArray()
        var index = 0
        while (index < lines.size) {
            val line = lines[index].trim()
            if (line.isEmpty()) {
                index++
                continue
            }
            val lower = line.lowercase(Locale.ROOT)
            val isHeadingLike = line.endsWith(":")
            val containsIntroKeyword =
                lower.contains("hier ist") ||
                    lower.contains("ueberarbeitet") ||
                    lower.contains("verbessert") ||
                    lower.contains("optimiert") ||
                    lower.contains("version:") ||
                    lower.contains("text:") ||
                    lower.contains("e-mail:") ||
                    lower.contains("email:") ||
                    lower.contains("mail:") ||
                    lower.startsWith("verbesserte version") ||
                    lower.startsWith("ueberarbeitete version") ||
                    lower.startsWith("ueberarbeiteter text") ||
                    lower.startsWith("optimierte e-mail") ||
                    lower.startsWith("optimierte email") ||
                    lower.startsWith("antwort:") ||
                    lower == "antwort"

            val politeIntroThenColon =
                (lower.startsWith("gerne") ||
                    lower.startsWith("gern ") ||
                    lower.startsWith("natuerlich") ||
                    lower.startsWith("selbstverstaendlich") ||
                    lower.startsWith("klar") ||
                    lower.startsWith("sicher")) &&
                    (line.contains(":") || lower.contains("hier ist"))

            if (isHeadingLike && containsIntroKeyword || politeIntroThenColon) {
                index++
                continue
            }
            break
        }
        return joinFrom(lines, index)
    }

    @JvmStatic
    fun unwrapSymmetricQuotes(value: String): String {
        if (value.length >= 2) {
            val first = value.first()
            val last = value.last()
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length - 1).trim()
            }
        }
        return value
    }

    @JvmStatic
    fun stripTrailingClosings(value: String?): String? {
        if (value.isNullOrEmpty()) return value
        val lines = value.split("\n").toTypedArray()
        var end = lines.size - 1
        while (end >= 0 && lines[end].trim().isEmpty()) end--
        if (end < 0) return ""

        var lastClosing = -1
        for (index in 0..end) {
            if (isClosingLine(lines[index].trim().lowercase(Locale.ROOT))) {
                lastClosing = index
            }
        }
        if (lastClosing >= 0 && end - lastClosing <= 4) {
            end = lastClosing - 1
            while (end >= 0 && lines[end].trim().isEmpty()) end--
        }
        if (end < 0) return ""
        return lines.take(end + 1).joinToString("\n").trim()
    }

    private fun isClosingLine(lower: String?): Boolean {
        val line = lower?.trim().orEmpty()
        if (line.isEmpty()) return false
        return line.startsWith("mit freundlichen gr") ||
            line.startsWith("viele gr") ||
            line.startsWith("freundliche gr") ||
            line.startsWith("beste gr") ||
            line.startsWith("herzliche gr") ||
            line == "gruss" ||
            line == "gruesse" ||
            line == "vg" ||
            line == "lg" ||
            line == "mfg" ||
            line.startsWith("best regards") ||
            line.startsWith("kind regards")
    }

    private fun joinFrom(lines: Array<String>, start: Int): String =
        if (start <= 0) lines.joinToString("\n") else lines.drop(start).joinToString("\n")
}
