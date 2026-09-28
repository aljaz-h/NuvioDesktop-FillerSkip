package com.nuvio.app.features.anime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnimeIdResolverTest {

    @Test
    fun normalizesCaseWhitespaceAndPunctuation() {
        assertEquals("attack on titan", normalizeTitle("  Attack on Titan  "))
        assertEquals("attack on titan", normalizeTitle("Attack, on: Titan!"))
        assertEquals("attack on titan", normalizeTitle("Attack   on\tTitan"))
    }

    @Test
    fun identicalNormalizedTitlesScoreAsExactMatch() {
        assertEquals(1.0, titleSimilarity(normalizeTitle("Naruto"), normalizeTitle("naruto")))
    }

    @Test
    fun doesNotConfuseNarutoWithNarutoShippuden() {
        val score = titleSimilarity(normalizeTitle("Naruto"), normalizeTitle("Naruto Shippuden"))
        assertTrue(score < 0.72, "expected low similarity for distinct sequel, got $score")
    }

    @Test
    fun doesNotConfuseNarutoWithBoruto() {
        val score = titleSimilarity(normalizeTitle("Naruto"), normalizeTitle("Boruto: Naruto Next Generations"))
        assertTrue(score < 0.72, "expected low similarity for distinct spin-off, got $score")
    }

    @Test
    fun minorPunctuationDifferenceStillMatchesConfidently() {
        val score = titleSimilarity(normalizeTitle("Cowboy Bebop"), normalizeTitle("Cowboy Bebop:"))
        assertTrue(score >= 0.72, "expected high similarity for trivial punctuation diff, got $score")
    }

    @Test
    fun unrelatedTitlesScoreLow() {
        val score = titleSimilarity(normalizeTitle("Naruto"), normalizeTitle("Cowboy Bebop"))
        assertTrue(score < 0.5, "expected very low similarity for unrelated titles, got $score")
    }
}
