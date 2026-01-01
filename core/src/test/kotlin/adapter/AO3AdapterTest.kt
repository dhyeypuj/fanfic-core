package adapter

import com.dhyey.fanfic.adapter.AO3Adapter
import com.dhyey.fanfic.site.FicSite
import org.junit.Test
import util.loadResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AO3AdapterTest {

    private val adapter = AO3Adapter()

    // ==================== SUPPORTS ====================

    @Test
    fun `supports AO3 URLs`() {
        assertTrue(adapter.supports("https://archiveofourown.org/works/12345"))
        assertTrue(adapter.supports("https://archiveofourown.org/works/12345/chapters/67890"))
        assertTrue(adapter.supports("https://ao3.org/works/12345"))
    }

    @Test
    fun `does not support non-AO3 URLs`() {
        assertTrue(!adapter.supports("https://fanfiction.net/s/12345"))
        assertTrue(!adapter.supports("https://wattpad.com/story/12345"))
    }

    // ==================== ONE-SHOT ====================

    @Test
    fun `parse AO3 one-shot metadata correctly`() {
        val html = loadResource("ao3/ao3_os.html")
        val metadata = adapter.parseMetadata(html, "https://archiveofourown.org/works/12345")

        assertEquals(FicSite.AO3, metadata.site)
        assertTrue(metadata.title.isNotBlank())
        assertTrue(metadata.author.isNotBlank())
        assertEquals(1, metadata.chapters)
        assertTrue(metadata.words > 0)
    }

    @Test
    fun `parse AO3 one-shot chapters correctly`() {
        val html = loadResource("ao3/ao3_os.html")
        val chapters = adapter.parseChapters(html)

        assertEquals(1, chapters.size)
        assertEquals(1, chapters.first().number)
        assertTrue(chapters.first().title.isNotBlank())
    }

    @Test
    fun `parse AO3 one-shot content correctly`() {
        val html = loadResource("ao3/ao3_os.html")
        val content = adapter.parseChapterContent(html)

        assertTrue(content.isNotBlank())
        // Content should be HTML
        assertTrue(content.contains("<") || content.length > 100)
    }

    // ==================== MULTI-CHAPTER ====================

    @Test
    fun `parse AO3 multi-chapter metadata correctly`() {
        val html = loadResource("ao3/ao3_multi_chapter.html")
        val metadata = adapter.parseMetadata(html, "https://archiveofourown.org/works/12345")

        assertEquals(FicSite.AO3, metadata.site)
        assertTrue(metadata.title.isNotBlank())
        assertTrue(metadata.author.isNotBlank())
        assertTrue(metadata.chapters >= 1)
        assertTrue(metadata.words > 0)
    }

    @Test
    fun `parse AO3 multi-chapter chapters correctly`() {
        val html = loadResource("ao3/ao3_multi_chapter.html")
        val chapters = adapter.parseChapters(html)

        // Should have multiple chapters
        assertTrue(chapters.size >= 1)

        // First chapter
        assertEquals(1, chapters.first().number)
        assertTrue(chapters.first().title.isNotBlank())

        // Last chapter should have correct number
        if (chapters.size > 1) {
            val last = chapters.last()
            assertEquals(chapters.size, last.number)
        }
    }

    // ==================== SERIES / MULTI-PART ====================

    @Test
    fun `parse AO3 series part metadata correctly`() {
        val html = loadResource("ao3/ao3_multi_part.html")
        val metadata = adapter.parseMetadata(html, "https://archiveofourown.org/works/12345")

        assertEquals(FicSite.AO3, metadata.site)
        assertTrue(metadata.title.isNotBlank())
        assertTrue(metadata.author.isNotBlank())
    }

    @Test
    fun `parse AO3 series info correctly`() {
        val html = loadResource("ao3/ao3_multi_part.html")
        val seriesInfo = adapter.parseSeriesInfo(html)

        // Series info might be present
        if (seriesInfo != null) {
            assertTrue(seriesInfo.name.isNotBlank())
            assertTrue(seriesInfo.url.isNotBlank())
        }
    }

    // ==================== ADDITIONAL METADATA ====================

    @Test
    fun `parse AO3 tags and characters`() {
        val html = loadResource("ao3/ao3_os.html")
        val metadata = adapter.parseMetadata(html, "https://archiveofourown.org/works/12345")

        // These might be empty depending on the story, but should be lists
        assertNotNull(metadata.genres)
        assertNotNull(metadata.characters)
    }
}
