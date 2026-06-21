package org.example.kalkulationsprogramm.service.pdf

import com.lowagie.text.pdf.BaseFont

class TextSplitter(
    private var baseFont: BaseFont,
    private var fontSize: Float,
) {
    fun splitTextToFitHeight(fullText: String, width: Float, availableHeight: Float): Array<String> {
        val leading = fontSize * 1.2f
        val maxLines = (availableHeight / leading).toInt()

        if (maxLines <= 0) {
            return arrayOf("", fullText)
        }

        val words = fullText.split("\\s+".toRegex()).toTypedArray()
        val part1 = StringBuilder()
        var currentLine = StringBuilder()
        var linesCount = 1
        var wordIndex = 0

        for (i in words.indices) {
            val word = words[i]
            val currentLineWidth = baseFont.getWidthPoint("$currentLine $word", fontSize)

            if (currentLineWidth < width) {
                if (currentLine.isNotEmpty()) currentLine.append(" ")
                currentLine.append(word)
            } else {
                linesCount++

                if (linesCount > maxLines) {
                    wordIndex = i
                    break
                }

                part1.append(currentLine).append("\n")
                currentLine = StringBuilder(word)
            }
            wordIndex = i + 1
        }

        if (linesCount <= maxLines) {
            part1.append(currentLine)
        }

        val part2 = StringBuilder()
        if (wordIndex < words.size) {
            part2.append(currentLine)

            for (j in wordIndex until words.size) {
                if (part2.isNotEmpty()) part2.append(" ")
                part2.append(words[j])
            }
        }

        return arrayOf(part1.toString(), part2.toString())
    }
}
