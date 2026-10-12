package com.example.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerTest {

    @Test
    fun testNumericVersionComparisonMultiDigit() {
        // Critical test: 1.10.0 is greater than 1.2.0 (standard string comparison would fail!)
        assertTrue(SemVer.isLessThan("1.2.0", "1.10.0"))
        assertFalse(SemVer.isLessThan("1.10.0", "1.2.0"))
        assertTrue(SemVer.isAtLeast("1.10.0", "1.2.0"))
    }

    @Test
    fun testMajorVersionBump() {
        assertTrue(SemVer.isLessThan("1.9.9", "2.0.0"))
        assertFalse(SemVer.isLessThan("2.0.0", "1.9.9"))
        assertTrue(SemVer.isAtLeast("2.0.0", "1.0.0"))
    }

    @Test
    fun testPatchVersionComparison() {
        assertTrue(SemVer.isLessThan("1.0.0", "1.0.1"))
        assertFalse(SemVer.isLessThan("1.0.1", "1.0.0"))
        assertTrue(SemVer.isAtLeast("1.0.1", "1.0.1"))
    }

    @Test
    fun testLeadingVPrefixHandled() {
        val v1 = SemVer.parse("v1.2.3")
        val v2 = SemVer.parse("1.2.3")
        assertEquals(v1, v2)
        assertFalse(SemVer.isLessThan("v1.2.3", "1.2.3"))
    }

    @Test
    fun testMissingPatchDefaultsToZero() {
        val vShort = SemVer.parse("1.0")
        val vFull = SemVer.parse("1.0.0")
        assertEquals(vShort.major, vFull.major)
        assertEquals(vShort.minor, vFull.minor)
        assertEquals(vShort.patch, vFull.patch)
        assertTrue(SemVer.isAtLeast("1.0", "1.0.0"))
    }

    @Test
    fun testPrereleaseComparison() {
        val release = SemVer.parse("1.0.0")
        val beta = SemVer.parse("1.0.0-beta.1")
        assertTrue(release > beta)
    }

    @Test
    fun testMalformedVersionStringsFallbackSafely() {
        val emptyVer = SemVer.parse("")
        assertEquals(0, emptyVer.major)
        assertEquals(0, emptyVer.minor)
        assertEquals(0, emptyVer.patch)

        val nullVer = SemVer.parse(null)
        assertEquals(0, nullVer.major)

        val textVer = SemVer.parse("release-candidate")
        assertEquals(0, textVer.major)
    }
}
