package com.navelplace.jsemver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

/*
1.0	x >= 1.0 * The default Maven meaning for 1.0 is everything (,) but with 1.0 recommended. Obviously this doesn't work for enforcing versions here, so it has been redefined as a minimum version.
(,1.0]	x <= 1.0
(,1.0)	x < 1.0
[1.0]	x == 1.0
[1.0,)	x >= 1.0
(1.0,)	x > 1.0
(1.0,2.0)	1.0 < x < 2.0
[1.0,2.0]	1.0 <= x <= 2.0
(,1.0],[1.2,)	x <= 1.0 or x >= 1.2. Multiple sets are comma-separated
(,1.1),(1.1,)	x != 1.1
 */

class MavenVersionRequirementTest {

    @Test
    fun `MavenVersionRequirement correctly parses`() {
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("[1.1,2.2]"), "[", "1.1", "2.2", "]")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("[1.1,2.2)"), "[", "1.1", "2.2", ")")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("(1.1,2.2)"), "(", "1.1", "2.2", ")")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("(1.1,2.2]"), "(", "1.1", "2.2", "]")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("(1.1.1,2.2]"), "(", "1.1.1", "2.2", "]")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("[,2.2]"), "[", "", "2.2", "]")
        doesMatch(MavenVersionRequirement.VERSION_REQUIREMENT_REGEX.find("[1.1,]"), "[", "1.1", "", "]")

    }

    @Test
    fun `Calculates requirements`() {
        val req = MavenVersionRequirement("[1.1,2.2]")
        val expected = VersionRange(min = Version("1.1.0"), max = Version("2.2.0"))
        assertEquals(expected, req[0])
    }

    @Test
    fun `Calculates multiple requirements`() {
        val req = MavenVersionRequirement("[1.1,2.2],(3.3,4.4)")
        assertEquals(2, req.size())
        val firstExpected = VersionRange(min = Version("1.1.0"), max = Version("2.2.0"))
        assertEquals(firstExpected, req[0])
        val secondExpected = VersionRange(min = Version("3.3.0"), minInclusive = false, max = Version("4.4.0"), maxInclusive = false)
        assertEquals(secondExpected, req[1])

    }

    fun doesMatch(match: MatchResult?, vararg values: String) {
        var j=0
        for (i in arrayOf(1,3,4,5)) {
            val expected = values[j++]?: ""
            val actual = match!!.groups[i]?.value?: ""
            assertEquals(expected, actual)
        }
    }

    @Test
    fun `Can handle the single version format`() {
        //According to the Maven spec, an unenclosed version is a minimum
        val req = MavenVersionRequirement("1.5")
        assertEquals(1, req.size())
        assertEquals(VersionRange(min = Version("1.5.0"), max = Version.MAX_VERSION), req[0])
        assertTrue(arrayOf("1.5.0", "1.5.1", "2.4.0").all({ MavenVersionRequirement("1.5").isSatisfiedBy(it)}))
        assertTrue(arrayOf("0.1.1", "1.4.9", "1.5.0-SNAPSHOT").none({ MavenVersionRequirement("1.5").isSatisfiedBy(it)}))
    }

    @Test
    fun `Should parse a maven format for requirements`() {
        val areSatisfied = arrayOf(
                "(,2.0]",
                "(,2.0)",
                "[1.0,)",
                "(1.0,)",
                "(1.0,2.0)",
                "[1.0,2.0]",
                "(,1.1),(1.1,)"
        )

        val areNotSatisfied = arrayOf(
                "(,1.4)",
                "[1.6,)",
                "(1.6,)",
                "(1.5,)",
                "(1.5,2.0)",
                "[1.6,2.0]",
                "(,1.5),(1.5,)"

        )

        val version = "1.5.0"
        areSatisfied.map { MavenVersionRequirement(it) }.forEach {
            assertTrue("$version should satisfy $it", it.isSatisfiedBy(version))
        }


        areNotSatisfied.map { MavenVersionRequirement(it) }.forEach {
            assertFalse("$version should not satisfy $it", it.isSatisfiedBy(version))
        }

        assertTrue(MavenVersionRequirement("(,1.0],[1.2,)").isSatisfiedBy("0.1.1"))
        assertTrue(MavenVersionRequirement("(,1.0],[1.2,)").isSatisfiedBy("1.2.0"))
        assertFalse(MavenVersionRequirement("(,1.1),(1.1,)").isSatisfiedBy("1.1.0"))


    }

}