package com.navelplace.jsemver.ruby

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RubyGemsVersionRequirementTest {
    @Test
    fun `lt_space_1dot1_space should be valid`() {
        assertTrue("lt_space_1.1_space should be valid", {RubyGemsVersionRequirement.isValid("< 1.1 ")})
    }
    @Test
    fun `Correctly distinguishes valid and invalid requirements`() {
        val validVersions = arrayOf("1.1", "1.1.1", "1.2.34", "1")
        val operators = arrayOf("=", "<", ">", "<=", ">=", "~>")
        validVersions.forEach { version ->
            operators.forEach { operator ->
                assertTrue ( "${operator}_space_${version}_space should be valid", { RubyGemsVersionRequirement.isValid("$operator $version") } )
                assertTrue ( "_space${operator}_space_${version}_space should be valid", { RubyGemsVersionRequirement.isValid(" $operator $version ") } )
                assertTrue ( "$operator$version should be valid", { RubyGemsVersionRequirement.isValid("$operator$version") } )
                assertTrue ( "_space${operator}${version}_space should be valid", { RubyGemsVersionRequirement.isValid(" $operator$version ") } )

                assertTrue ( "_space${operator}${version}_space should be valid", { RubyGemsVersionRequirement.isValid(" $operator$version ") } )

            }
            assertTrue("$version should be valid", { RubyGemsVersionRequirement.isValid(version)})
        }

        val invalidVersions = arrayOf("s=1.1", "~ > 1.1", "1.1.1x", "<<1.1.1", "<>1.1.1", ">==1.1.1", "~>>1.1.1", "1.1 1.1")

        invalidVersions.forEach {version ->
            assertFalse("$version should not be valid", {RubyGemsVersionRequirement.isValid(version)})
        }

    }
}
