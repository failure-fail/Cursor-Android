package fail.failure.grok.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import fail.failure.grok.ui.theme.GrokAccent
import fail.failure.grok.ui.theme.GrokSurfaceRaised
import fail.failure.grok.ui.theme.GrokTextSecondary

/**
 * A small hand-rolled renderer for the subset of markdown an agent transcript actually produces
 * (headings, bold/italic/inline code, fenced code blocks, bullet/numbered lists) - not a full
 * CommonMark implementation, just enough that assistant responses don't show up as raw asterisks
 * and backticks the way plain [Text] left them.
 */
@Composable
fun MarkdownText(markdown: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var codeBuffer: MutableList<String>? = null

        @Composable
        fun flushCode() {
            val lines = codeBuffer ?: return
            codeBuffer = null
            Text(
                lines.joinToString("\n"),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GrokSurfaceRaised, RoundedCornerShape(8.dp))
                    .padding(10.dp),
            )
        }

        markdown.lineSequence().forEach { rawLine ->
            val line = rawLine.trimEnd()
            if (line.trimStart().startsWith("```")) {
                if (codeBuffer == null) codeBuffer = mutableListOf() else flushCode()
                return@forEach
            }
            if (codeBuffer != null) {
                codeBuffer!!.add(rawLine)
                return@forEach
            }

            when {
                line.isBlank() -> Unit
                line.startsWith("### ") -> Text(line.removePrefix("### "), style = MaterialTheme.typography.titleMedium)
                line.startsWith("## ") -> Text(line.removePrefix("## "), style = MaterialTheme.typography.titleMedium)
                line.startsWith("# ") -> Text(line.removePrefix("# "), style = MaterialTheme.typography.titleLarge)
                line.trimStart().let { it.startsWith("- ") || it.startsWith("* ") } -> {
                    Row {
                        Text("•  ", color = GrokAccent, style = MaterialTheme.typography.bodyLarge)
                        Text(inlineMarkdown(line.trimStart().removePrefix("- ").removePrefix("* ")))
                    }
                }
                Regex("^\\d+\\.\\s").containsMatchIn(line.trimStart()) -> {
                    val trimmed = line.trimStart()
                    val marker = trimmed.substringBefore(" ") + " "
                    Row {
                        Text(marker, color = GrokAccent, style = MaterialTheme.typography.bodyLarge)
                        Text(inlineMarkdown(trimmed.removePrefix(marker)))
                    }
                }
                else -> Text(inlineMarkdown(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
        flushCode()
    }
}

/** Converts `**bold**`, `*italic*`, and `` `code` `` spans within one line into an [androidx.compose.ui.text.AnnotatedString]. */
@Composable
private fun inlineMarkdown(text: String) = buildAnnotatedString {
    val pattern = Regex("(\\*\\*[^*]+\\*\\*|`[^`]+`|\\*[^*]+\\*)")
    var lastIndex = 0
    val codeColor = LocalContentColor.current
    pattern.findAll(text).forEach { match ->
        append(text.substring(lastIndex, match.range.first))
        val token = match.value
        when {
            token.startsWith("**") -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(token.removePrefix("**").removeSuffix("**"))
            }
            token.startsWith("`") -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = GrokSurfaceRaised,
                    color = codeColor,
                ),
            ) {
                append(" ${token.removePrefix("`").removeSuffix("`")} ")
            }
            token.startsWith("*") -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(token.removePrefix("*").removeSuffix("*"))
            }
            else -> append(token)
        }
        lastIndex = match.range.last + 1
    }
    append(text.substring(lastIndex))
}
