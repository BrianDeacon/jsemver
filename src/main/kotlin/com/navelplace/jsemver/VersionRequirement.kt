package com.navelplace.jsemver

/**
 * Indicates a specific [VersionRequirement] implementation
 */
enum class RequirementType {
    STRICT,
    MAVEN
}

/**
 * Specifies restrictions against which a given [Version](Version) can be tested.
 *
 * Specific implementations per standard specified by [RequirementType]
 *
 * @see StrictVersionRequirement
 * @see MavenVersionRequirement
 */
abstract class VersionRequirement protected constructor(requirement: String,
                                                        val type: RequirementType) {
    private val rawRequirement: String = requirement.trim()
    private val validRanges: Array<VersionRange>

    /**
     * @suppress
     */
    companion object {
        /**
         * Parses the [VersionRequirement] as defined by the given [RequirementType]. Defaults to [RequirementType.STRICT]
         * @param versionRequirement The string representation of the requirement
         * @param requirementType The specific implementation to use
         * @return The parsed [VersionRequirement]
         */
        @JvmStatic @JvmOverloads fun fromString(versionRequirement: String, requirementType: RequirementType = RequirementType.STRICT): VersionRequirement {
            return when (requirementType) {
                RequirementType.STRICT -> StrictVersionRequirement(versionRequirement)
                RequirementType.MAVEN -> MavenVersionRequirement(versionRequirement)
            }
        }
    }

    protected abstract fun calculate(rawRequirement: String): Array<VersionRange>

    /**
     * Returns true if the requirement is met by the given [version]
     * @param version The version to test
     * @return True if the requirement is met
     */
    fun isSatisfiedBy(version: String) = isSatisfiedBy(Version(version))

    /**
     * Returns true if the requirement is met by the given [version]
     * @param version The version to test
     * @return True if the requirement is met
     */
    open fun isSatisfiedBy(version: Version): Boolean {
        return validRanges.any { it.contains(version) }
    }

    /**
     * The raw string representation of the requirement
     * @return The raw string representation of the requirement
     */
    override fun toString() = rawRequirement

    /**
     * Returns individual [VersionRange] instances that satisfy this requirement
     * @return An individual [VersionRange]
     * @param index The index of the requested range
     */
    operator fun get(index: Int) = validRanges[index]

    /**
     * The number of individual [VersionRange](Version Ranges) that satisfy this requirement
     */
    fun size() = validRanges.size

    init {
        validRanges = calculate(rawRequirement)
    }
}

open class InvalidRequirementFormatException(message: String): RuntimeException(message)