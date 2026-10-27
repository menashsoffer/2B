package com.menash.myself.r2

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult

/**
 * ASCII digits, Arabic-Indic (U+0660–U+0669) and Eastern Arabic-Indic (U+06F0–U+06F9).
 * Hebrew numerals are letters, so they are not scanned: a letter scan would flag ordinary words.
 */
val NumeralDigits = Regex("[0-9\\u0660-\\u0669\\u06F0-\\u06F9]")

fun String.hasNumeralDigit(): Boolean = NumeralDigits.containsMatchIn(this)

fun SemanticsNode.collectTexts(): List<String> {
    val own = config.getOrNull(SemanticsProperties.Text).orEmpty().map { text -> text.text }
    return own + children.flatMap { child -> child.collectTexts() }
}

fun SemanticsNode.collectDescriptions(): List<String> {
    val own = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()
    return own + children.flatMap { child -> child.collectDescriptions() }
}

fun SemanticsNode.collectTestTags(): List<String> {
    val own = config.getOrNull(SemanticsProperties.TestTag)
    val tags = if (own == null) emptyList() else listOf(own)
    return tags + children.flatMap { child -> child.collectTestTags() }
}

fun SemanticsNode.isHeading(): Boolean = config.contains(SemanticsProperties.Heading)

fun SemanticsNode.collectHeadings(): List<SemanticsNode> {
    val own = if (isHeading()) listOf(this) else emptyList()
    return own + children.flatMap { child -> child.collectHeadings() }
}

fun SemanticsNodeInteraction.textLayout(): TextLayoutResult {
    val layouts = mutableListOf<TextLayoutResult>()
    performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getLayout ->
        val accepted = getLayout(layouts)
        check(accepted) { "GetTextLayoutResult was rejected" }
    }
    check(layouts.size == 1) { "Expected one TextLayoutResult, found ${layouts.size}" }
    return layouts.first()
}

fun Rect.containsBounds(other: Rect, tolerance: Float = 0.5f): Boolean {
    return other.left >= left - tolerance &&
        other.top >= top - tolerance &&
        other.right <= right + tolerance &&
        other.bottom <= bottom + tolerance
}
