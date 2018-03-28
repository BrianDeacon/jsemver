package com.navelplace.jsemver

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrictVersionRequirementTest {

    @Test
    fun `Should include and exclude versions appropriately`() {
        val requirements = arrayOf(StrictVersionRequirement("1.0.0-2.0.0"),
                StrictVersionRequirement("  1.0.0  -2.0.0 "))

        val inside = arrayOf(
                "1.0.0",
                "1.0.1",
                "1.2.0",
                "2.0.0",
                "1.1.1-beta+foo",
                "2.0.0-SNAPSHOT"
        )

        val outside = arrayOf(
                "0.0.1",
                "0.9.99",
                "2.0.1",
                "1.0.0-SNAPSHOT",
                "11.1.1"
        )

        inside.forEach {version ->
            requirements.forEach { requirement ->
                assertTrue("$version should satisfy requirement $requirement", requirement.isSatisfiedBy(Version(version)) )
                assertTrue("$version should satisfy requirement $requirement", Version(version).satisfies(requirement) )
                assertTrue("$version should satisfy requirement $requirement", Version(version).satisfies(requirement.toString()) )
            }
        }
        outside.forEach {version ->
            requirements.forEach { requirement ->
                assertFalse("$version should not satisfy requirement $requirement", requirement.isSatisfiedBy(Version(version)) )
                assertFalse("$version should not satisfy requirement $requirement", Version(version).satisfies(requirement))
                assertFalse("$version should not satisfy requirement $requirement", Version(version).satisfies(requirement.toString()))
            }

        }
    }
}