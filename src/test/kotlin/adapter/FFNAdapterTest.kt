package adapter

import com.dhyey.fanfic.adapter.FFNAdapter
import org.junit.jupiter.api.Test
import util.loadResource
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FFNAdapterTest {

    @Test
    fun `parse FFN title, author, and numeric metadata`() {
        val html = loadResource("ffn/story.html")
        val adapter = FFNAdapter()

        val metadata = adapter.parseMetadata(
            html,
            "https://www.fanfiction.net/s/8277618/1"
        )

        assertEquals(
            "Twenty One Nights of Paradise",
            metadata.title
        )

        assertEquals(
            "red-jacobson",
            metadata.author
        )

        assertEquals(9, metadata.chapters)
        assertEquals(104153, metadata.words)

        assertNotNull(metadata.published)
        assertNotNull(metadata.updated)
    }
}
