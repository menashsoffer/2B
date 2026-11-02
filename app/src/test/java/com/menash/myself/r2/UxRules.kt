package com.menash.myself.r2

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import java.io.File

/**
 * UX check layer (docs/ux/UX-CHECKS.md, layer 4). Runs on every screen a golden test captures and
 * records measurable problems: tap targets crowded below 48dp by a neighbour, clickable elements with no label for a screen
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
        val clickables = mutableListOf<SemanticsNode>()
        fun visit(node: SemanticsNode, insideHorizontalScroll: Boolean) {
            val config = node.config
            val scrollsSideways = config.contains(SemanticsProperties.HorizontalScrollAxisRange)
            // A 0x0 node was not laid out (scrolled out of the captured viewport); nothing to measure.
            val laidOut = node.boundsInRoot.width > 0f && node.boundsInRoot.height > 0f
            if (config.contains(SemanticsActions.OnClick) && laidOut) {
                clickables += node
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
        findings += crowdedTargets(clickables, density)
        return findings.distinct()
    }

    /**
     * Compose already extends the touch area of a small clickable to 48x48dp (ViewConfiguration
     * minimumTouchTargetSize), so a 32dp chip alone is fine. It is a real problem only when another
     * clickable sits inside that 48dp area: there the neighbour wins the tap, and the small target
     * is effectively smaller than 48dp. That is what this rule reports.
     */
    private fun crowdedTargets(clickables: List<SemanticsNode>, density: Float): List<Finding> {
        val minPx = MinTouchDp * density
        return clickables.mapNotNull { node ->
            val b = node.boundsInRoot
            if (b.width + Tolerance * density >= minPx && b.height + Tolerance * density >= minPx) return@mapNotNull null
            val dx = maxOf(0f, (minPx - b.width) / 2)
            val dy = maxOf(0f, (minPx - b.height) / 2)
            val reach = Rect(b.left - dx, b.top - dy, b.right + dx, b.bottom + dy)
            val neighbour = clickables.firstOrNull { other ->
                other !== node && !related(node, other) && other.boundsInRoot.overlaps(reach) && !other.boundsInRoot.overlaps(b)
            } ?: return@mapNotNull null
            Finding(
                "touch-target",
                describe(node),
                "%.0fx%.0fdp, '%s' inside its 48dp area".format(b.width / density, b.height / density, describe(neighbour)),
            )
        }
    }

    private fun related(a: SemanticsNode, b: SemanticsNode): Boolean {
        fun SemanticsNode.isAncestorOf(other: SemanticsNode): Boolean {
            var p = other.parent
            while (p != null) { if (p.id == id) return true; p = p.parent }
            return false
        }
        return a.isAncestorOf(b) || b.isAncestorOf(a)
    }

    /** Appends this capture's findings, keyed by the calling test, to the build report. */
    fun record(root: SemanticsNode, variant: String) = write(check(root), variant)

    private fun write(findings: List<Finding>, variant: String) {
        if (findings.isEmpty()) return
        val caller = Throwable().stackTrace.firstOrNull { it.className.endsWith("Test") }
        val test = caller?.let { "${it.className.substringAfterLast('.')}.${it.methodName}" } ?: "unknown"
        val dir = File(buildDir(), "ux-findings").apply { mkdirs() }
        File(dir, "findings.tsv").appendText(
            findings.joinToString("") { "$test\t$variant\t${it.rule}\t${it.element.replace('\t', ' ')}\t${it.detail.replace('\t', ' ')}\n" }
        )
    }

    private fun label(node: SemanticsNode): String {
        // collectTexts/collectDescriptions already include the node's own values.
        return (node.collectTexts() + node.collectDescriptions()).map { it.trim() }.filter { it.isNotEmpty() }
            .distinct().joinToString(" ")
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
