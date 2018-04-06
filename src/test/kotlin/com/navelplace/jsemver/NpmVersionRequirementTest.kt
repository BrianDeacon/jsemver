package com.navelplace.jsemver

import org.junit.Test
import kotlin.test.assertEquals

class NpmVersionRequirementTest {

    @Test
    fun `can parse a raw version`() {
        val vString = "99.88.77"
        val requirement = NpmVersionRequirement("v$vString")
        assertEquals(1, requirement.size())
        val v = Version.fromString(vString)
        val range = VersionRange(min=v, max=v, minInclusive = true, maxInclusive = false)
        assertEquals(range, requirement[0])
    }

    @Test
    fun `can parse a raw version with two Xs`() {
        val vString = "99.x.x"
        val requirement = NpmVersionRequirement("v$vString")
        assertEquals(1, requirement.size())
        val min = Version.fromString("99.0.0")
        val max = Version.fromString("100.0.0")
        val range = VersionRange(min=min, max=max, minInclusive = true, maxInclusive = false)
        assertEquals(range, requirement[0])
    }

    @Test
    fun `can parse a raw version with one X`() {
        val vString = "99.99.x"
        val requirement = NpmVersionRequirement("v$vString")
        assertEquals(1, requirement.size())
        val min = Version.fromString("99.99.0")
        val max = Version.fromString("99.100.0")
        val range = VersionRange(min=min, max=max, minInclusive = true, maxInclusive = false)
        assertEquals(range, requirement[0])
    }

    @Test
    fun `various ranges`() {
        val map = mapOf(
                "1.2.X" to ranges("1.2.0", "1.3.0"),
                "1.x.x" to ranges("1.0.0", "2.0.0"),
                "1.2.3" to ranges("1.2.3", "1.2.3"),
                "v1.2.3" to ranges("1.2.3", "1.2.3"),
                "V1.x.x" to ranges("1.0.0", "2.0.0"),
                "v1.2.X" to ranges("1.2.0", "1.3.0")
        )
        map.forEach {key, range -> {
            val requirement = NpmVersionRequirement(key)
            assertEquals(1, requirement.size())
            assertEquals(range, requirement[0])
        } }
    }

    @Test
    fun `ands`() {
        val req = NpmVersionRequirement("1.2.3 4.5.6")
    }

    @Test
    fun `Can parse a simple AND`() {
        val requirement = NpmVersionRequirement("1.2.3 4.5.6 || 3.3.3-SNAPSHOT 2.2.2-foo.bar+baz.blif")

    }

    fun exactRange(version: String): VersionRange {
        val v = Version(version)
        return VersionRange(min = v, max = v, minInclusive = true, maxInclusive = false)
    }

    private fun ranges(min: String, max: String) = VersionRange(min=Version(min), max=Version(max), minInclusive = true, maxInclusive = false)
}