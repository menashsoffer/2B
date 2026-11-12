package com.menash.myself.feature.money

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorDirtyTest {
    @Test
    fun dirtyWhenAFieldChanges() {
        val opened = ItemDraft(EditKind.INCOME, name = "שכר", amount = "100")
        val state = EditorState(draft = opened, isNew = false)
        assertFalse(state.isDirty())
        assertTrue(state.copy(draft = opened.copy(amount = "120")).isDirty())
    }
}
