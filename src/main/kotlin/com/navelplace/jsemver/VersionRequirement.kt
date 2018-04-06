package com.navelplace.jsemver

/**
 * Indicates a specific [VersionRequirement] implementation
 */
enum class RequirementType {
    SIMPLE,
    MAVEN,
    NPM
}

/**
 * Specifies restrictions against which a given [Version] can be tested.
 *
 * Specific implementations per standard specified by [RequirementType]
 *
 * @property type The [RequirementType] that the instance implements
 *
 * @see SimpleVersionRequirement
 * @see MavenVersionRequirement
 */
abstract class VersionRequirement protected constructor(requirement: String,
                                                        val type: RequirementType) {
    protected val rawRequirement: String = requirement.trim()
    private val validRanges: Array<VersionRange>

    /**
     * @suppress
     */
    companion object {
        /**
         * Parses the [VersionRequirement] as defined by the given [RequirementType]. Defaults to [RequirementType.SIMPLE]
         * @param versionRequirement The string representation of the requirement
         * @param requirementType The specific implementation to use
         * @return The parsed [VersionRequirement]
         */
        @JvmStatic @JvmOverloads fun fromString(versionRequirement: String, requirementType: RequirementType = RequirementType.SIMPLE): VersionRequirement {
            return when (requirementType) {
                RequirementType.SIMPLE -> SimpleVersionRequirement(versionRequirement)
                RequirementType.MAVEN -> MavenVersionRequirement(versionRequirement)
                RequirementType.NPM -> NpmVersionRequirement(versionRequirement)
            }
        }
    }

    /**
     * @suppress
     */
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
     * The number of individual [VersionRange] instances that satisfy this requirement
     */
    fun size() = validRanges.size

    init {
        validRanges = calculate(rawRequirement)
    }
}

/**
 * @suppress
 */
open class InvalidRequirementFormatException(message: String): RuntimeException(message)