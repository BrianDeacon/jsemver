package com.navelplace.jsemver

/**
 * [VersionRequirement] implementation for a simple range of full versions in the form "1.0.0-2.0.0"
 *
 * @constructor Parses the simple [versionRequirement]
 */
class SimpleVersionRequirement(versionRequirement: String): VersionRequirement(versionRequirement, RequirementType.SIMPLE) {

    /**
     * @suppress
     */
    override fun calculate(rawRequirement: String): Array<VersionRange> {
        val elements = rawRequirement.split("-")
        if (elements.size != 2) {
            throw InvalidSimpleRequirementFormatException(rawRequirement)
        }

        val min = Version(elements[0].trim())
        val max = Version(elements[1].trim())

        return arrayOf(VersionRange(min=min, max=max))
    }
}

/**
 * A string not conforming to the specification was used
 */
class InvalidSimpleRequirementFormatException(format: String): InvalidRequirementFormatException("Invalid format for SIMPLE requirement: $format")
