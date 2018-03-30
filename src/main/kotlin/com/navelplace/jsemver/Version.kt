package com.navelplace.jsemver

import com.navelplace.jsemver.RequirementType.*

/**
 * Represents a semver pattern `major.minor.patch[-preRelease[+metadata]]`
 *
 * @author Brian Deacon (bdeacon@navelplace.com)
 *
 * @property major The major version
 * @property minor The minor version
 * @property patch The patch version (third element)
 * @property preRelease The pre-release qualifier
 * @property metadata The build metadata
 */
class Version : Comparable<Version> {

    private val raw: String
    val major: Int
    val minor: Int
    val patch: Int
    val preRelease: String
    val metadata: String

    /**
     * @suppress
     */
    companion object {
        private val SEMVER_REGEX = Regex("""
        ^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-((0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(\+([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*))?$
        """.trim())

        private fun makeMatch(version: String): MatchResult? {
            return SEMVER_REGEX.find(version)
        }

        /**
         * No [Version](Version) is greater than [MAX_VERSION]
         */
        @JvmField val MAX_VERSION = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        /**
         * No [Version](Version) is less than [MIN_VERSION]
         */
        @JvmField val MIN_VERSION = Version(Int.MIN_VALUE,Int.MIN_VALUE,Int.MIN_VALUE)

        /**
         * Checks [version] for correct formatting
         * @param version The string to be tested
         * @return True if the version string is correctly formatted
         */
        @JvmStatic fun isValid(version: String): Boolean {
            return makeMatch(version) != null
        }

        /**
         * Constructs a [Version](Version) based on the value of [version]
         * @param version The string to be converted to a [Version](Version) instance
         * @return The constructed [Version]
         * @throws [InvalidVersionFormatException]
         */
        @JvmStatic fun fromString(version: String) = Version(version)
    }

    /**
     * Parses [version] as a Semver-compliant string
     * @param version The string representation of the [Version](Version)
     */
    constructor(version: String) {
        raw = version.trim()
        val match = makeMatch(raw) ?: throw InvalidVersionFormatException(version)
        major = match.groups[1]!!.value.toInt()
        minor = match.groups[2]!!.value.toInt()
        patch = match.groups[3]!!.value.toInt()
        preRelease = match.groups[5]?.value?: ""
        metadata = match.groups[10]?.value?: ""
    }

    private constructor(major: Int, minor: Int, patch: Int, prerelease: String = "", build: String = "") {
        val buildString = if (build.isNotEmpty()) "+$build" else ""
        val tail = if(prerelease.isNotEmpty()) "-$prerelease$buildString" else ""
        raw = "$major.$minor.$patch$tail"
        this.major = major
        this.minor = minor
        this.patch = patch
        this.preRelease = prerelease
        this.metadata = build
    }

    /**
     * The raw string representation of the [Version](Version).
     */
    override fun toString() = raw

    /**
     * Tests for literal object equivalence. For semantic equivalence see [equivalentTo]
     * @param other The other [Version](Version) to be compared
     * @see equivalentTo
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (raw != other.raw) return false

        return true
    }

    /**
     * @suppress
     */
    override fun hashCode(): Int {
        return raw.hashCode()
    }

    /**
     * Tests if another [Version](Version) is greater
     * @param other The other [Version](Version) to be compared
     * @return True if [other] is semantically newer.
     */
    fun greaterThan(other: Version): Boolean {
        return compareTo(other) > 0
    }

    /**
     * Tests if another [Version](Version) is greater
     * @param other The other [Version] to be compared
     * @return True if [other](other) is semantically older.
     */
    fun lessThan(other: Version): Boolean {
        return compareTo(other) < 0
    }

    /**
     * Tests [other] against this instance for version equivalence.
     *
     * For example, `1.1.1-alpha+beta` is semantically equivalent to `1.1.1-alpha+gamma`
     *
     * @param other The other [Version](Version) to be compared
     * @return True if [other] is semantically equivalent.
     */
    fun equivalentTo(other: Version): Boolean {
        return compareTo(other) == 0
    }

    /**
     * Parses [versionRequirement] as a [VersionRequirement](VersionRequirement) and tests if this [Version](Version) satisfies the
     * requirement
     *
     * @param versionRequirement The string representation of the [VersionRequirement]
     * @param type The specific [VersionRequirement](VersionRequirement) implementation as defined by the [RequirementType]
     * @return True if the requirement is satisfied
     * @see VersionRequirement
     * @see RequirementType
     */
    fun satisfies(versionRequirement: String, type: RequirementType = STRICT): Boolean {
        return satisfies(VersionRequirement.fromString(versionRequirement, type))
    }

    /**
     * Tests if this [Version](Version) satisfies [versionRequirement]
     * @param versionRequirement The [VersionRequirement] to test
     * @return True if the requirement is satisfied
     * @see RequirementType
     */
    fun satisfies(versionRequirement: VersionRequirement): Boolean {
        return versionRequirement.isSatisfiedBy(this)
    }

    /**
     * @suppress
     */
    override fun compareTo(other: Version): Int {
        var compare = major.compareTo(other.major)
        if (compare != 0) return compare
        compare = minor.compareTo(other.minor)
        if (compare != 0) return compare
        compare = patch.compareTo(other.patch)
        if (compare != 0) return compare
        /*
        From http://semver.org :
        When major, minor, and patch are equal, a pre-release version has lower
        precedence than a normal version. Example: 1.0.0-alpha < 1.0.0

         */
        if (preRelease.isBlank() && other.preRelease.isNotBlank()) {
            return 1
        }
        if (preRelease.isNotBlank() && other.preRelease.isBlank()) {
            return -1
        }
        if (preRelease.isBlank() && other.preRelease.isBlank()) {
            return 0
        }


        /*
        "Precedence for two pre-release versions with the same major, minor, and patch
        version MUST be determined by comparing each dot separated identifier from
        left to right until a difference is found as follows: identifiers consisting
        of only digits are compared numerically and identifiers with letters or
        hyphens are compared lexically in ASCII sort order. Numeric identifiers always
        have lower precedence than non-numeric identifiers. A larger set of pre-release
        fields has a higher precedence than a smaller set, if all of the preceding
        identifiers are equal.
        Example: 1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-alpha.beta < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-beta.11 < 1.0.0-rc.1 < 1.0.0.
         */
        val myItems = preRelease.split(".")
        val otherItems = other.preRelease.split(".")

        //If one array is longer, we initially disregard the extra elements
        for (i in 0..Math.min(myItems.size - 1, otherItems.size - 1)) {
            compare = elementCompare(myItems[i], otherItems[i])
            if (compare != 0) return compare
        }

        // If we got here, the versions are identical except that one preRelease may be longer than the other
        // The shorter one comes first, so just compare the number of elements in preRelease
        return myItems.size.compareTo(otherItems.size)
    }

    private fun elementCompare(one: String, two: String): Int {
        //Two numbers compare numerically
        if (one.isNumber() && two.isNumber()) {
            return one.toInt().compareTo(two.toInt())
        }

        //"alpha1" comes before "1"
        if (one.isNumber() && !two.isNumber()) {
            return -1
        }
        if (!one.isNumber() && two.isNumber()) {
            return 1
        }

        //Just compare strings
        return one.compareTo(two)
    }

    private fun String.isNumber() : Boolean =
        try {
            this.toInt()
            true
        } catch(e: NumberFormatException) {
            false
        }
}

/**
 * Indicates a requested version format was invalid.
 *
 * @property format The invalid string
 */
class InvalidVersionFormatException(val format: String): RuntimeException("Invalid version format: $format")