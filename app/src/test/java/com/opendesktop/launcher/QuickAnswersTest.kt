package com.opendesktop.launcher

import com.opendesktop.launcher.domain.QuickAnswers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickAnswersTest {
    private fun calc(q: String) = QuickAnswers.calculate(q)?.result

    @Test
    fun arithmetic() {
        assertEquals("14", calc("2+3*4"))
        assertEquals("20", calc("(2+3)*4"))
        assertEquals("2,5", calc("5/2"))
        assertEquals("1,5", calc("1,2 + 0,3"))
        assertEquals("1024", calc("2^10"))
        assertEquals("-6", calc("-2*3"))
        assertEquals("20", calc("200*10%"))
        assertEquals("4", calc("12:3="))
    }

    @Test
    fun notACalculation() {
        assertNull(calc("spotify"))
        assertNull(calc("42"))
        assertNull(calc("-42"))
        assertNull(calc("5/0"))
        assertNull(calc("2+"))
        assertNull(calc("(2+3"))
    }

    @Test
    fun units() {
        assertEquals("6,213711922 mi", QuickAnswers.convert("10 km na mi")?.result)
        assertEquals("100 °C", QuickAnswers.convert("212 f to c")?.result)
        assertEquals("1500 ml", QuickAnswers.convert("1,5 l w ml")?.result)
        assertEquals("1024 MB", QuickAnswers.convert("1 GB to MB")?.result)
        assertEquals("2,204622622 lb", QuickAnswers.convert("1 kg na funty")?.result)
        assertNull(QuickAnswers.convert("10 km na kg"))
        assertNull(QuickAnswers.convert("muzyka na spacer"))
    }

    @Test
    fun answerPrefersConversion() {
        assertEquals("100 cm", QuickAnswers.answer("1 m na cm")?.result)
        assertEquals("3", QuickAnswers.answer("1+2")?.result)
        assertNull(QuickAnswers.answer("mapy"))
    }

    @Test
    fun badgeCounts() {
        assertEquals(
            mapOf("a" to 2, "b" to 1),
            com.opendesktop.launcher.data.NotificationBadges.countByPackage(listOf("a", "b", "a")),
        )
    }
}
