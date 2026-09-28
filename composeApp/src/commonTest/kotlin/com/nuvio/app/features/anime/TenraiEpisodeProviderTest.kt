package com.nuvio.app.features.anime

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TenraiEpisodeProviderTest {

    @Test
    fun mapsFillerRecapNormalFromDto() {
        assertEquals(AnimeEpisodeType.FILLER, episodeDto(filler = true, recap = false).toDomain().type)
        assertEquals(AnimeEpisodeType.RECAP, episodeDto(filler = false, recap = true).toDomain().type)
        assertEquals(AnimeEpisodeType.NORMAL, episodeDto(filler = false, recap = false).toDomain().type)
    }

    @Test
    fun recapWinsWhenBothFlagsAreTrue() {
        assertEquals(AnimeEpisodeType.RECAP, episodeDto(filler = true, recap = true).toDomain().type)
    }

    @Test
    fun fetchesAllPagesWhenHasNextPageIsTrue() = runTest {
        val page1 = """
            {"pagination":{"last_visible_page":2,"has_next_page":true},
             "data":[{"mal_id":1,"title":"Ep 1","filler":false,"recap":false}]}
        """.trimIndent()
        val page2 = """
            {"pagination":{"last_visible_page":2,"has_next_page":false},
             "data":[{"mal_id":2,"title":"Ep 2","filler":true,"recap":false}]}
        """.trimIndent()
        val requestedPages = mutableListOf<Int>()

        val provider = TenraiEpisodeProvider(fetchPageText = { _, page ->
            requestedPages += page
            if (page == 1) page1 else page2
        })

        val episodes = provider.getEpisodes(malId = 20)

        assertEquals(listOf(1, 2), requestedPages)
        assertEquals(2, episodes.size)
        assertEquals(1, episodes[0].episodeNumber)
        assertEquals(AnimeEpisodeType.NORMAL, episodes[0].type)
        assertEquals(2, episodes[1].episodeNumber)
        assertEquals(AnimeEpisodeType.FILLER, episodes[1].type)
    }

    @Test
    fun stopsAtSinglePageWhenHasNextPageIsFalse() = runTest {
        val page1 = """
            {"pagination":{"last_visible_page":1,"has_next_page":false},
             "data":[{"mal_id":1,"title":"Ep 1","filler":false,"recap":false}]}
        """.trimIndent()
        var requestCount = 0

        val provider = TenraiEpisodeProvider(fetchPageText = { _, _ ->
            requestCount += 1
            page1
        })

        val episodes = provider.getEpisodes(malId = 1)

        assertEquals(1, requestCount)
        assertEquals(1, episodes.size)
    }

    @Test
    fun keepsPartialResultsWhenALaterPageFails() = runTest {
        val page1 = """
            {"pagination":{"last_visible_page":2,"has_next_page":true},
             "data":[{"mal_id":1,"title":"Ep 1","filler":false,"recap":false}]}
        """.trimIndent()

        val provider = TenraiEpisodeProvider(fetchPageText = { _, page ->
            if (page == 1) page1 else error("network failure")
        })

        val episodes = provider.getEpisodes(malId = 1)

        assertTrue(episodes.size == 1)
        assertEquals(1, episodes.first().episodeNumber)
    }

    private fun episodeDto(filler: Boolean, recap: Boolean) = TenraiEpisodeDto(
        episodeNumber = 1,
        title = "Episode",
        filler = filler,
        recap = recap,
    )
}
