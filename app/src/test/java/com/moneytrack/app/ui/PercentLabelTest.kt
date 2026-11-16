package com.moneytrack.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentLabelTest {

    @Test fun roundsToNearestNotDown() {
        // The November test data: 900 / 1410.50 = 63.8%, which used to show as 63%.
        assertEquals("64%", percentLabel(90_000, 141_050))
        assertEquals("18%", percentLabel(25_000, 141_050))   // 17.7%
        assertEquals("9%", percentLabel(12_000, 141_050))    // 8.5% rounds half up
        assertEquals("6%", percentLabel(8_000, 141_050))     // 5.67%
        assertEquals("4%", percentLabel(6_000, 141_050))     // 4.25%
    }

    @Test fun exactSharesAreUnchanged() {
        assertEquals("45%", percentLabel(90_000, 200_000))
        assertEquals("35%", percentLabel(70_000, 200_000))
        assertEquals("20%", percentLabel(40_000, 200_000))
        assertEquals("100%", percentLabel(6_430, 6_430))
    }

    @Test fun tinyShareShowsLessThanOnePercent() {
        assertEquals("<1%", percentLabel(50, 141_050))       // $0.50 of $1,410.50
        assertEquals("<1%", percentLabel(1, 1_000_000))
    }

    @Test fun halfPercentRoundsUpToOne() {
        assertEquals("1%", percentLabel(1, 200))             // exactly 0.5%
    }

    @Test fun ninetyNinePointFiveRoundsToHundred() {
        assertEquals("100%", percentLabel(995, 1_000))
    }

    @Test fun zeroOrInvalidTotalsDoNotCrash() {
        assertEquals("0%", percentLabel(0, 100))
        assertEquals("0%", percentLabel(50, 0))
        assertEquals("0%", percentLabel(0, 0))
        assertEquals("0%", percentLabel(-5, 100))
    }

    @Test fun largeAmountsDoNotOverflow() {
        assertEquals("50%", percentLabel(5_000_000_000_000L, 10_000_000_000_000L))
    }
}
