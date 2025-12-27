package adapter

import com.dhyey.fanfic.adapter.FFNAdapter
import org.junit.jupiter.api.Test
import util.loadResource
import kotlin.test.assertEquals

class FFNAdapterTest {

    @Test
    fun `parse FFN title and author from saved HTML`() {
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
    }
}
