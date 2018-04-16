package com.navelplace.jsemver.cocoapods

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue


class CocoaPodsVersionRequirementTest {

    @Test
    fun `Can do an equals comparison`() {
        assertTrue { CocoaPodsVersionRequirement("=1.1.1").isSatisfiedBy("1.1.1") }
    }

    @Test
    fun `Wildcards the minor and patch correctly`() {
        assertTrue { CocoaPodsVersionRequirement("=1.1").isSatisfiedBy("1.1.0") }
        assertFalse { CocoaPodsVersionRequirement("=1.1").isSatisfiedBy("1.1.1") }
    }

    @Test
    fun `Can do a less than`() {
        assertTrue { CocoaPodsVersionRequirement("<1.1.0").isSatisfiedBy("1.0.9") }
        assertFalse { CocoaPodsVersionRequirement("<1.1.0").isSatisfiedBy("1.1.0") }
        assertFalse { CocoaPodsVersionRequirement("<1.1.0").isSatisfiedBy("1.1.1") }
    }

    @Test
    fun `Can do a greater than`() {
        assertTrue { CocoaPodsVersionRequirement(">0.1.0").isSatisfiedBy("1.0.9") }
        assertFalse { CocoaPodsVersionRequirement(">0.1.0").isSatisfiedBy("0.1.0") }
        assertFalse { CocoaPodsVersionRequirement(">0.1.0").isSatisfiedBy("0.0.1") }
    }

    @Test
    fun `Can do a greater than or equal to`() {
        assertTrue { CocoaPodsVersionRequirement(">=0.1.0").isSatisfiedBy("1.0.9") }
        assertTrue { CocoaPodsVersionRequirement(">=0.1.0").isSatisfiedBy("0.1.0") }
        assertFalse { CocoaPodsVersionRequirement(">=0.1.0").isSatisfiedBy("0.0.1") }
    }

    @Test
    fun `Correctly distinguishes valid and invalid requirements`() {
        val validVersions = arrayOf("1.1", "1.1.1", "1.2.34", "1")
        val operators = arrayOf("=", "<", ">", "<=", ">=", "~>")
        validVersions.forEach { version ->
            assertTrue("$version should be valid", { CocoaPodsVersionRequirement.isValid(version)})
            operators.forEach { operator ->
                assertTrue ( "$operator$version should be valid", { CocoaPodsVersionRequirement.isValid("$operator$version") } )
                assertTrue ( "_space${operator}${version}_space should be valid", { CocoaPodsVersionRequirement.isValid(" $operator$version ") } )
                assertTrue ( "_space${operator}_space_${version}_space should be valid", { CocoaPodsVersionRequirement.isValid(" $operator $version ") } )
                assertTrue ( "_space${operator}${version}_space should be valid", { CocoaPodsVersionRequirement.isValid(" $operator$version ") } )
                assertTrue ( "${operator}_space_${version}_space should be valid", { CocoaPodsVersionRequirement.isValid("$operator $version") } )
            }
        }

        val invalidVersions = arrayOf("s=1.1", "~ > 1.1", "1.1.1x", "<<1.1.1", "<>1.1.1", ">==1.1.1", "~>>1.1.1", "1.1 1.1")

        invalidVersions.forEach {version ->
            assertFalse("$version should not be valid", {CocoaPodsVersionRequirement.isValid(version)})
        }

    }
}
