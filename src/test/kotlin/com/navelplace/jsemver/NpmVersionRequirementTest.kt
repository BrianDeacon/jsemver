package com.navelplace.jsemver

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NpmVersionRequirementTest {

    @Test
    fun `can parse a raw version`() {
        val vString = "99.88.77"
        val requirement = NpmVersionRequirement("v$vString")
        assertTrue { requirement.isSatisfiedBy(vString) }
        assertFalse { requirement.isSatisfiedBy("1.2.3") }
    }

    @Test
    fun `can parse a raw version with two Xs`() {
        val vString = "99.x.x"
        val requirement = NpmVersionRequirement("v$vString")
        assertTrue { requirement.isSatisfiedBy("99.0.0") }
        assertTrue { requirement.isSatisfiedBy("99.1.1") }
    }

    @Test
    fun `can parse a raw version with one X`() {
        val vString = "99.99.x"
        val requirement = NpmVersionRequirement("v$vString")
        assertTrue { requirement.isSatisfiedBy("99.99.0") }
        assertTrue { requirement.isSatisfiedBy("99.99.1") }
    }

    @Test
    fun `various ranges`() {
        val map = mapOf(
                "1.2.X" to arrayOf("1.2.0", "1.2.1","1.2.10"),
                "1.x.x" to arrayOf("1.0.0", "1.9.1","1.0.1", "1.2.3"),
                "1.2.3" to arrayOf("1.2.3"),
                "v1.2.3" to arrayOf("1.2.3"),
                "V1.x.x" to arrayOf("1.0.0", "1.9.1","1.0.1", "1.2.3"),
                "v1.2.X" to arrayOf("1.2.0", "1.2.1")
        )
        map.forEach {key, versions ->
            val requirement = NpmVersionRequirement(key)
            assertTrue(versions.all { requirement.isSatisfiedBy(it) })
        }
    }

    @Test
    fun `Simple AND with operators`() {
        val req = NpmVersionRequirement(">1.2.3 <4.5.6")
        assertTrue { req.isSatisfiedBy("2.0.0") }
    }

    @Test
    fun `Can parse a simple AND`() {
        val requirement = NpmVersionRequirement("1.2.3 4.5.6 || 3.3.3-SNAPSHOT 2.2.2-foo.bar+baz.blif")
        //It's an impossible requirement
        assertFalse { requirement.isSatisfiedBy("1.2.3") }

    }
}