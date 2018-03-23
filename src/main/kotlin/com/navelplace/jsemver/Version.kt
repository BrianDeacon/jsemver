package com.navelplace.jsemver

import com.navelplace.jsemver.exceptions.InvalidVersionFormatException

class Version : Comparable<Version> {

    val raw: String
    val major: Int
    val minor: Int
    val patch: Int
    val prerelease: String
    val build: String

    companion object {
        private val SEMVER_REGEX = Regex("""
        ^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)(-((0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*)(\.(0|[1-9]\d*|\d*[a-zA-Z-][0-9a-zA-Z-]*))*))?(\+([0-9a-zA-Z-]+(\.[0-9a-zA-Z-]+)*))?$
        """.trim())

        private fun makeMatch(version: String): MatchResult? {
            return SEMVER_REGEX.find(version)
        }

        fun isValid(version: String): Boolean {
            return makeMatch(version) != null
        }
    }

    constructor(version: String) {
        raw = version.trim()
        val match = makeMatch(version) ?: throw InvalidVersionFormatException(version)
        major = match.groups[1]!!.value.toInt()
        minor = match.groups[2]!!.value.toInt()
        patch = match.groups[3]!!.value.toInt()
        prerelease = match.groups[5]?.value?: ""
        build = match.groups[10]?.value?: ""
    }

    override fun toString() : String {
        return raw
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Version

        if (raw != other.raw) return false

        return true
    }

    override fun hashCode(): Int {
        return raw.hashCode()
    }

    fun greaterThan(other: Version): Boolean {
        return compareTo(other) > 0
    }

    fun lessThan(other: Version): Boolean {
        return compareTo(other) < 0
    }

    fun equivalent(other: Version): Boolean {
        return compareTo(other) == 0
    }

    fun satisfies(versionRequirement: String): Boolean {
        return satisfies(VersionRequirement(versionRequirement))
    }

    fun satisfies(versionRequirement: VersionRequirement): Boolean {
        return versionRequirement.isSatisfiedBy(this)
    }

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
        if (prerelease.isBlank() && other.prerelease.isNotBlank()) {
            return 1
        }
        if (prerelease.isNotBlank() && other.prerelease.isBlank()) {
            return -1
        }
        if (prerelease.isBlank() && other.prerelease.isBlank()) {
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
        val myItems = prerelease.split(".")
        val otherItems = other.prerelease.split(".")

        //If one array is longer, we initially disregard the extra elements
        for (i in 0..Math.min(myItems.size - 1, otherItems.size - 1)) {
            compare = elementCompare(myItems[i], otherItems[i])
            if (compare != 0) return compare
        }

        // If we got here, the versions are identical except that one prerelease may be longer than the other
        // The shorter one comes first, so just compare the number of elements in prerelease
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

data class VersionRange(val min: Version, val minInclusive: Boolean, val max: Version, val maxInclusive: Boolean) {
    fun contains(version: Version): Boolean {
        if (minInclusive && version.equivalent(min)) return true
        if (maxInclusive && version.equivalent(max)) return true
        return version.greaterThan(min) && version.lessThan(max)
    }
}

class VersionRequirement {
    private val rawRequirement: String
    private val validRanges: Array<VersionRange>

    constructor(requirement: String) {
        rawRequirement = requirement.trim()
        validRanges = calculate(rawRequirement)
    }

    private fun calculate(rawRequirement: String): Array<VersionRange> {
        return emptyArray()
    }

    fun isSatisfiedBy(version: Version): Boolean {
        return validRanges.any { it.contains(version) }
    }

}