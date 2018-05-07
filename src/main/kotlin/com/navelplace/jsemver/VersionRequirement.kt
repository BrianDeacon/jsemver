package com.navelplace.jsemver

import com.navelplace.jsemver.cocoapods.CocoaPodsVersionRequirement
import com.navelplace.jsemver.maven.MavenVersionRequirement
import com.navelplace.jsemver.npm.NpmVersionRequirement
import com.navelplace.jsemver.ruby.RubyGemsVersionRequirement

/**
 * Indicates a specific [VersionRequirement] implementation
 */
enum class RequirementType {
    SIMPLE,
    MAVEN,
    NPM,
    COCOAPODS,
    RUBY
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
 * @see NpmVersionRequirement
 */
abstract class VersionRequirement protected constructor(requirement: String,
                                                        /** @suppress */val type: RequirementType) {
    /**
     * @suppress
     */
    protected val rawRequirement: String = requirement.trim()

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
                RequirementType.SIMPLE -> SimpleVersionRequirement.fromString(versionRequirement)
                RequirementType.MAVEN -> MavenVersionRequirement.fromString(versionRequirement)
                RequirementType.NPM -> NpmVersionRequirement.fromString(versionRequirement)
                RequirementType.COCOAPODS -> CocoaPodsVersionRequirement.fromString(versionRequirement)
                RequirementType.RUBY -> RubyGemsVersionRequirement.fromString(versionRequirement)
            }
        }

        /**
         * Returns true if the [versionRequirement] supplied as defined by the standard referenced by the [requirementType]
         * @param versionRequirement The requirement string
         * @param requirementType The [RequirementType] that the given version refers to
         * @return True if the requirement string is valid for the format specified
         */
        @JvmStatic @JvmOverloads fun isValid(versionRequirement: String, requirementType: RequirementType = RequirementType.SIMPLE): Boolean {
            return when (requirementType) {
                RequirementType.SIMPLE -> SimpleVersionRequirement.isValid(versionRequirement)
                RequirementType.MAVEN -> MavenVersionRequirement.isValid(versionRequirement)
                RequirementType.NPM -> NpmVersionRequirement.isValid(versionRequirement)
                RequirementType.COCOAPODS -> CocoaPodsVersionRequirement.isValid(versionRequirement)
                RequirementType.RUBY -> RubyGemsVersionRequirement.isValid(versionRequirement)
            }
        }
    }

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
    abstract fun isSatisfiedBy(version: Version): Boolean

    /**
     * The raw string representation of the requirement
     * @return The raw string representation of the requirement
     */
    override fun toString() = rawRequirement
}

