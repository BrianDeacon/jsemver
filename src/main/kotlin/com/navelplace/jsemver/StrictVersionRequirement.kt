package com.navelplace.jsemver

class StrictVersionRequirement(versionRequirement: String): VersionRequirement(versionRequirement, RequirementType.STRICT) {
    override fun calculate(rawRequirement: String): Array<VersionRange> {
        val elements = rawRequirement.split("-")
        if (elements.size != 2) {
            throw InvalidStrictRequirementFormatException(rawRequirement)
        }

        val min = Version(elements[0].trim())
        val max = Version(elements[1].trim())

        return arrayOf(VersionRange(min=min, max=max))
    }
}

class InvalidStrictRequirementFormatException(format: String): RuntimeException("Invalid format for STRICT requirement: $format")
