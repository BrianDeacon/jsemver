package com.navelplace.jsemver

import com.navelplace.jsemver.RequirementType.SIMPLE

/**
 * [VersionRequirement] implementation for a simple range of full versions in the form "1.0.0-2.0.0"
 *
 */
class SimpleVersionRequirement: VersionRequirement {
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
