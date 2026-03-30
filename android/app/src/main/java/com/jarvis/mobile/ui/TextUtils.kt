package com.jarvis.mobile.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle

private val RICH_PATTERN = Regex("""\*\*\*(.+?)\*\*\*|\*\*(.+?)\*\*|\*(.+?)\*|\[(.+?)\]\((.+?)\)|(https?://[^\s,;'")\]]+)""")
private val SOURCES_PATTERN = Regex("""<!--SOURCES:[\s\S]+?-->""")
private val IMAGE_PATTERN = Regex("""!\[([^\]]*)\]\(([^)]+)\)""")

/** Extract inline image URLs from markdown text (![alt](url)). */
fun extractImages(text: String): List<String> =
    IMAGE_PATTERN.findAll(text).map { it.groupValues[2] }.toList()

/** Strip inline images from text before rendering as rich text. */
fun stripImages(text: String): String =
    IMAGE_PATTERN.replace(text, "").replace(Regex("""\n{3,}"""), "\n\n").trim()

/**
 * Builds an AnnotatedString that renders **bold**, *italic*, and [links](url) from markdown.
 * Strips <!--SOURCES:--> blocks so they never appear in the bubble.
 */
fun buildRichAnnotatedString(text: String, linkColor: Color): AnnotatedString {
    val clean = SOURCES_PATTERN.replace(stripImages(text), "").trim()
    return buildAnnotatedString {
        var last = 0
        for (match in RICH_PATTERN.findAll(clean)) {
            append(clean.substring(last, match.range.first))
            val (boldItalic, bold, italic, linkText, url, bareUrl) = match.destructured
            when {
                boldItalic.isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic)) { append(boldItalic) }
                bold.isNotEmpty()       -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bold) }
                italic.isNotEmpty()     -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(italic) }
                linkText.isNotEmpty()   -> withLink(
                    LinkAnnotation.Url(
                        url = url,
                        styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    )
                ) { append(linkText) }
                bareUrl.isNotEmpty()    -> withLink(
                    LinkAnnotation.Url(
                        url = bareUrl,
                        styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    )
                ) { append(bareUrl) }
            }
            last = match.range.last + 1
        }
        append(clean.substring(last))
    }
}

/** Strip markdown formatting for display in chat bubbles. */
fun stripMarkdown(text: String): String = text
    .replace(Regex("""\*\*\*(.+?)\*\*\*"""), "$1")
    .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
    .replace(Regex("""__(.+?)__"""), "$1")
    .replace(Regex("""\*(.+?)\*"""), "$1")
    .replace(Regex("""_(.+?)_"""), "$1")
    .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
    .replace(Regex("""```[\s\S]*?```"""), "")
    .replace(Regex("""`([^`]+)`"""), "$1")
    .replace(Regex("""\[(.+?)\]\(.+?\)"""), "$1")
    .replace(Regex("""^[-*+]\s+""", RegexOption.MULTILINE), "• ")
    .replace(Regex("""^>\s*""", RegexOption.MULTILINE), "")
    .replace(Regex("""^-{3,}$""", RegexOption.MULTILINE), "")
    .replace(Regex("""\n{3,}"""), "\n\n")
    .trim()

/** Strip markdown for TTS — produces clean spoken text with no symbols or code. */
fun stripForSpeech(text: String): String = text
    .replace(Regex("""<!--[\s\S]*?-->"""), "")          // remove HTML comments (SOURCES blocks etc.)
    .replace(Regex("""```[\s\S]*?```"""), "")           // remove code blocks entirely
    .replace(Regex("""`[^`]+`"""), "")                  // remove inline code entirely
    .replace(Regex("""~~(.+?)~~"""), "$1")              // strikethrough → text
    .replace(Regex("""\*\*\*(.+?)\*\*\*"""), "$1")
    .replace(Regex("""\*\*(.+?)\*\*"""), "$1")
    .replace(Regex("""__(.+?)__"""), "$1")
    .replace(Regex("""\*(.+?)\*"""), "$1")
    .replace(Regex("""_(.+?)_"""), "$1")
    .replace(Regex("""^#{1,6}\s+""", RegexOption.MULTILINE), "")
    .replace(Regex("""\[(.+?)\]\(.+?\)"""), "$1")      // links → label only
    .replace(Regex("""https?://\S+"""), "")             // bare URLs → silent
    .replace(Regex("""^[-*+]\s+""", RegexOption.MULTILINE), "")
    .replace(Regex("""^>\s*""", RegexOption.MULTILINE), "")
    .replace(Regex("""[—–]"""), " ")                    // em/en dash → space
    .replace(Regex("""[^a-zA-Z0-9\s.!?,']"""), " ")    // whitelist: only speakable chars
    .replace(Regex("""\n+"""), " ")                    // newlines → space for natural flow
    .replace(Regex("""\s{2,}"""), " ")
    .trim()
