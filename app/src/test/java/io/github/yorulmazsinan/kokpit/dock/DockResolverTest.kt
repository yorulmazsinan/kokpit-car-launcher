package io.github.yorulmazsinan.kokpit.dock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DockResolverTest {
    @Test
    fun usesOverrideWhenInstalled() {
        val installed = setOf("com.zjinnova.zlink", "com.example.autokit")
        assertEquals("com.example.autokit", resolveDockTarget(DockSlot.CARPLAY, installed, override = "com.example.autokit"))
    }

    @Test
    fun fallsBackWhenOverrideNotInstalled() {
        val installed = setOf("ru.yandex.yandexnavi", "com.waze")
        assertEquals("ru.yandex.yandexnavi", resolveDockTarget(DockSlot.MAPS, installed, override = "com.removed.app"))
    }

    @Test
    fun nullWhenNothingInstalled() {
        assertNull(resolveDockTarget(DockSlot.CARPLAY, emptySet(), override = null))
    }

    @Test
    fun appsSlotAlwaysNull() {
        assertNull(resolveDockTarget(DockSlot.APPS, setOf("com.anything"), override = "com.anything"))
    }

    @Test
    fun assistantSlotHasNoCandidatesAndIsNotInDock() {
        assertNull(resolveDockTarget(DockSlot.ASSISTANT, setOf("com.openai.chatgpt"), override = null))
        assertEquals("com.openai.chatgpt", resolveDockTarget(DockSlot.ASSISTANT, setOf("com.openai.chatgpt"), override = "com.openai.chatgpt"))
        assertEquals(6, DockSlot.entries.count { it.inDock })
    }
}
