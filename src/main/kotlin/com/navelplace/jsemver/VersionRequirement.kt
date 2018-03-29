package com.navelplace.jsemver

enum class RequirementType {
    STRICT,
    MAVEN
}

abstract class VersionRequirement {
    private val rawRequirement: String
    private val validRanges: Array<VersionRange>
    val type: RequirementType

    companion object {
        fun fromString(versionRequirement: String, requirementType: RequirementType = RequirementType.STRICT): VersionRequirement {
            return when (requirementType) {
                RequirementType.STRICT -> StrictVersionRequirement(versionRequirement)
                RequirementType.MAVEN -> MavenVersionRequirement(versionRequirement)
            }
        }
    }

    constructor(requirement: String, type: RequirementType) {
        rawRequirement = requirement.trim()
        this.type = type
        validRanges = calculate(rawRequirement)
    }

    protected abstract fun calculate(rawRequirement: String): Array<VersionRange>

    fun isSatisfiedBy(version: String) = isSatisfiedBy(Version(version))

    open fun isSatisfiedBy(version: Version): Boolean {
        return validRanges.any { it.contains(version) }
    }

    override fun toString() = rawRequirement

    operator fun get(index: Int) = validRanges[index]
    fun size() = validRanges.size
}