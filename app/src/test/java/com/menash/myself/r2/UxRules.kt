package com.menash.myself.r2

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import java.io.File

/**
 * UX check layer (docs/ux/UX-CHECKS.md, layer 4). Runs on every screen a golden test captures and
 * records measurable problems: tap targets under 48dp, clickable elements with no label for a screen
 * reader, and elements that stick out of the screen sideways. Report-only for now: findings go to
 * app/build/ux-findings/findings.tsv and the CI summary; thresholds come after the first measurement.
 */
object UxRules {
    private const val MinTouchDp = 48f
    private const val Tolerance = 0.5f

    data class Finding(val rule: String, val element: String, val detail: String)

    fun check(root: SemanticsNode): List<Finding> {
        val density = root.layoutInfo.density.density
        val screen = root.boundsInRoot
        val findings = mutableListOf<Finding>()
        fun visit(node: SemanticsNode, insideHorizontalScroll: Boolean) {
            val config = node.config
            val scrollsSideways = config.contains(SemanticsProperties.HorizontalScrollAxisRange)
            val clickable = config.contains(SemanticsActions.OnClick)
            if (clickable) {
                val wDp = node.boundsInRoot.width / density
                val hDp = node.boundsInRoot.height / density
                if (wDp + Tolerance < MinTouchDp || hDp + Tolerance < MinTouchDp) {
                    findings += Finding("touch-target", describe(node), "%.0fx%.0fdp".format(wDp, hDp))
                }
                if (label(node).isBlank()) {
                    findings += Finding("no-label", describe(node), "clickable without text or contentDescription")
                }
            }
            if (!insideHorizontalScroll && sticksOut(node.boundsInRoot, screen)) {
                findings += Finding("off-screen", describe(node), node.boundsInRoot.toString())
            }
            node.children.forEach { visit(it, insideHorizontalScroll || scrollsSideways) }
        }
        root.children.forEach { visit(it, false) }
        return findings.distinct()
    }

    /** Appends this capture's findings, keyed by the calling test, to the build report. */
    fun record(root: SemanticsNode, variant: String) {
        val caller = Throwable().stackTrace.firstOrNull { it.className.endsWith("Test") }
        val test = caller?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "unknown"
        val findings = check(root)
        if (findings.isEmpty()) return
        val dir = File(buildDir(), "ux-findings").apply { mkdirs() }
        File(dir, "findings.tsv").appendText(
            findings.joinToString("") { "$test\t$variant\t${it.rule}\t${it.element}\t${it.detail}\n" }
        )
    }

    private fun label(node: SemanticsNode): String {
        val text = node.config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString(" ") { it.text }
        val description = node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString(" ")
        val nested = node.collectTexts().joinToString(" ") + node.collectDescriptions().joinToString(" ")
        return "$text $description $nested".trim()
    }

    private fun describe(node: SemanticsNode): String {
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        return tag ?: label(node).take(30).ifBlank { "node#${node.id}" }
    }

    private fun sticksOut(bounds: Rect, screen: Rect): Boolean =
        bounds.width > 0 && (bounds.left < screen.left - Tolerance || bounds.right > screen.right + Tolerance)

    private fun buildDir(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        return if (File(cwd, "src/test").isDirectory) File(cwd, "build") else File(cwd, "app/build")
    }
}
