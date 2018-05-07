package com.navelplace.jsemver

import com.navelplace.jsemver.RequirementType.SIMPLE
import com.navelplace.jsemver.regex.RegexConstants.caret
import com.navelplace.jsemver.regex.RegexConstants.dash
import com.navelplace.jsemver.regex.RegexConstants.dollar
import com.navelplace.jsemver.regex.RegexConstants.fullVersion

/**
 * [VersionRequirement] implementation for a simple range of full versions in the form "1.0.0-2.0.0"
 *
 */
class SimpleVersionRequirement: VersionRequirement {

    /**
     * @suppress
     */
    companion object {
        private fun String.stripWhitespace(): String {
            return this.replace("""\s+""".toRegex(), "")
        }

        private val regex = """
            $caret
                $fullVersion
                $dash
                $fullVersion
            $dollar
            """.stripWhitespace().toRegex()



        /**
         * Verifies that [versionRequirement] is a valid requirement string
         * @param versionRequirement The requirement as a string
         * @return True if the provided string is valid
         */
        @JvmStatic fun isValid(versionRequirement: String) = regex.matches(versionRequirement)

        /**
         * Parses [versionRequirement] into an instance of [SimpleVersionRequirement]
         */
        @JvmStatic fun fromString(versionRequirement: String) = SimpleVersionRequirement(versionRequirement)
    }
    private val validVersions: VersionRange

    /**
     * Parses the simple [requirement]
     */
    constructor (requirement: String): super(requirement, SIMPLE) {
        val elements = rawRequirement.split("-")
        if (elements.size != 2) {
            throw InvalidSimpleRequirementFormatException(rawRequirement)
        }

        val min = Version(elements[0].trim())
        val max = Version(elements[1].trim())

        validVersions = VersionRange(min=min, max=max)
    }

    override fun isSatisfiedBy(version: Version) = validVersions.contains(version)
}

/**
 * @suppress
 */
class InvalidSimpleRequirementFormatException(format: String): InvalidRequirementFormatException("Invalid format for SIMPLE requirement: $format")
