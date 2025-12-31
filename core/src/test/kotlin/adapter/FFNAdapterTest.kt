package adapter

import com.dhyey.fanfic.adapter.FFNAdapter
import org.junit.jupiter.api.Test
import util.loadResource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FFNAdapterTest {

    @Test
    fun `parse FFN one-shot chapter correctly`() {
        val html = loadResource("ffn/story_os.html")
        val adapter = FFNAdapter()

        val chapters = adapter.parseChapters(html)

        assertEquals(1, chapters.size)
        assertEquals(1, chapters.first().number)
        assertEquals("A Quest for Europa", chapters.first().title)
    }

    @Test
    fun `parse FFN multi-chapter story correctly`() {
        val html = loadResource("ffn/story.html")
        val adapter = FFNAdapter()

        val chapters = adapter.parseChapters(html)

        // chapter count should match dropdown
        assertTrue(chapters.size > 1)

        // first chapter
        assertEquals(1, chapters.first().number)
        assertTrue(chapters.first().title.isNotBlank())

        // last chapter
        val last = chapters.last()
        assertEquals(chapters.size, last.number)
        assertTrue(last.title.isNotBlank())
    }
}
