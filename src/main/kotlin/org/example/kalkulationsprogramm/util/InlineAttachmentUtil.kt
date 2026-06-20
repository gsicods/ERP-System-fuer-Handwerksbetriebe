package org.example.kalkulationsprogramm.util

import java.util.Locale
import java.util.function.Function
import java.util.function.Predicate
import org.jsoup.Jsoup
import org.jsoup.parser.Parser

object InlineAttachmentUtil {
    @JvmStatic
    fun <T> rewriteCidSources(
        html: String?,
        attachments: List<T>?,
        inlinePredicate: Predicate<T>,
        contentIdExtractor: Function<T, String>,
        urlResolver: Function<T, String>,
    ): String? {
        if (html.isNullOrBlank() || attachments.isNullOrEmpty()) {
            return html
        }

        val inlineByCid = linkedMapOf<String, T>()
        attachments.filterNotNull().forEach { attachment ->
            if (!inlinePredicate.test(attachment)) return@forEach
            val cid = contentIdExtractor.apply(attachment)
            if (cid.isNullOrBlank()) return@forEach
            val normalized = normalizeContentId(cid)
            inlineByCid.putIfAbsent(normalized.lowercase(Locale.ROOT), attachment)
        }
        if (inlineByCid.isEmpty()) {
            return html
        }

        val doc = Jsoup.parse(html, "", Parser.htmlParser())
        doc.outputSettings().prettyPrint(false)
        doc.select("img[src^=cid:]").forEach { image ->
            val raw = image.attr("src")
            if (raw.length <= 4) return@forEach
            val normalized = normalizeContentId(raw.substring(4))
            val attachment = inlineByCid[normalized.lowercase(Locale.ROOT)]
            if (attachment != null) {
                val url = urlResolver.apply(attachment)
                if (!url.isNullOrBlank()) {
                    image.attr("src", url)
                    image.attr("data-inline-cid", normalized)
                }
            }
        }
        return doc.body().html()
    }

    private fun normalizeContentId(raw: String?): String {
        if (raw == null) return ""
        var trimmed = raw.trim()
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length > 2) {
            trimmed = trimmed.substring(1, trimmed.length - 1)
        }
        if (trimmed.lowercase(Locale.ROOT).startsWith("cid:")) {
            trimmed = trimmed.substring(4)
        }
        return trimmed.trim()
    }
}
