package com.example.data.curriculum

import com.example.data.firebase.sha256Hex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeerahCurriculumTest {

    @Test
    fun curriculum_hasAllTwelveChapters_withOnlyFirstFree() {
        assertEquals(12, SeerahCurriculum.chapters.size)
        assertTrue(SeerahCurriculum.chapters.first().isFree)
        for (i in 1 until SeerahCurriculum.chapters.size) {
            assertFalse("Chapter ${i + 1} should be paid", SeerahCurriculum.chapters[i].isFree)
        }
        SeerahCurriculum.chapters.forEachIndexed { idx, ch ->
            assertEquals((idx + 1).toLong(), ch.chapterNumber)
            assertTrue(ch.content.isNotBlank())
            assertTrue(ch.sourceNote.isNotBlank())
            assertEquals(3, ch.questions.size)
            ch.questions.forEach { q ->
                assertEquals(4, q.options.size)
                assertTrue(q.correctIndex in 0L..3L)
            }
        }
    }

    @Test
    fun sha256Hex_producesConsistent64CharDigest() {
        val h1 = sha256Hex("sample-code")
        val h2 = sha256Hex("sample-code")
        val h3 = sha256Hex("other-code")
        assertEquals(64, h1.length)
        assertEquals(h1, h2)
        assertNotEquals(h1, h3)
    }

    @Test
    fun summaryData_containsAllSevenChronologicalStages() {
        assertEquals(7, SeerahSummaryData.sections.size)
        SeerahSummaryData.sections.forEach { section ->
            assertTrue(section.title.isNotBlank())
            assertTrue(section.periodLabel.isNotBlank())
            assertTrue(section.body.length > 100)
            assertTrue(section.relatedChapters.all { it in 1..12 })
        }
        assertTrue(SeerahSummaryData.fullSpokenText().contains("السيرة النبوية كاملة مختصرة"))
    }
}
