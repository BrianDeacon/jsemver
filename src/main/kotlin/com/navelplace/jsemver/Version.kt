package com.navelplace.jsemver

import com.navelplace.jsemver.RequirementType.*
import com.navelplace.jsemver.exceptions.InvalidVersionFormatException

/**
 * A Semver class matching the pattern major.minor.patch
 */
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

        val MAX_VERSION = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)
        val MIN_VERSION = Version(Int.MIN_VALUE,Int.MIN_VALUE,Int.MIN_VALUE)

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

    private constructor(major: Int, minor: Int, patch: Int, prerelease: String = "", build: String = "") {
        val buildString = if (build.isNotEmpty()) "+$build" else ""
        val tail = if(prerelease.isNotEmpty()) "-$prerelease$buildString" else ""
        raw = "$major.$minor.$patch$tail"
        this.major = major
        this.minor = minor
        this.patch = patch
        this.prerelease = prerelease
        this.build = build
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

    fun equivalentTo(other: Version): Boolean {
        return compareTo(other) == 0
    }

    fun satisfies(versionRequirement: String, type: RequirementType = STRICT): Boolean {
        return satisfies(VersionRequirement.build(versionRequirement, type))
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

data class VersionRange(val min: Version, val minInclusive: Boolean = true, val max: Version, val maxInclusive: Boolean = true) {
    fun contains(version: Version): Boolean {
        if (minInclusive && version.equivalentTo(min)) return true
        if (maxInclusive && version.equivalentTo(max)) return true
        return version.greaterThan(min) && version.lessThan(max)
    }
}

abstract class VersionRequirement {

    protected val rawRequirement: String
    private val validRanges: Array<VersionRange>
    val type: RequirementType

    companion object {
        fun build(versionRequirement: String, requirementType: RequirementType = STRICT): VersionRequirement {
            return when (requirementType) {
                STRICT -> StrictVersionRequirement(versionRequirement)
                MAVEN -> MavenVersionRequirement(versionRequirement)
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

enum class RequirementType {
    STRICT,
    MAVEN
}


class MavenVersionRequirement(rawRequirement: String) : VersionRequirement(rawRequirement, MAVEN) {

    companion object {
        /*
        Any comma preceded by a ] or ) is the delimiter for:
        [1.1,2.2],[3.3,4.4),(5.5,6.6.6],(7.7,8.8)
         */
        val VERSION_GROUPS_REGEX = """
        (?<=[\]\)])\s*\,
        """.trim().toRegex()

        private val open = """
            [\[\(]
            """.trim()
        private val close = """
            [\]\)]
            """.trim()
        // 0 or 1 or 10 but not 01
        private val versionNumber = """
            (?:0|[1-9]\d*)
            """.trim()
        // x.y.z
        private val version = """
            $versionNumber\.$versionNumber(?:\.$versionNumber)?
            """.trim()

        /*
         1.1,2.2
         3.3.3,4.4
         5.5,6.6.6
         ,8.8
         9.9,
         */
        private val versions = """
            ($version)?\s*\,\s*($version)?
            """.trim()

        /*
        [1.1,2.2],[3.3,4.4),(5.5,6.6],(7.7,8.8)
         */
        val VERSION_REQUIREMENT_REGEX = """
        ($open)\s*($versions)\s*($close)
        """.trim().toRegex()

        val SINGLE_VERSION_REQUIREMENT_REGEX = """
            $open\s*($version)\s*$close
            """.trim().toRegex()

        // [1.0,2.0] -> [1.0.0,2.0.0]
        private fun normalize(value: String): String {
            return when {
                value.isBlank() -> ""
                value.split(".").size != 3 -> "$value.0"
                else -> value
            }

        }
    }

    override fun calculate(rawRequirement: String): Array<VersionRange> {
        val elements: List<String> = rawRequirement.trim().split(VERSION_GROUPS_REGEX)
        return elements.map {
            when {
                //"1.5"
                !it.startsWith("[") && !it.startsWith("(") -> {
                    VersionRange(min=Version(normalize(it)), max = Version.MAX_VERSION)
                }

                //"[1.5]"
                SINGLE_VERSION_REQUIREMENT_REGEX.find(it) != null -> {
                    val version = normalize(SINGLE_VERSION_REQUIREMENT_REGEX.find(it)?.groups?.get(1)?.value?: "")
                    VersionRange(min = Version(version), max = Version(version))
                }

                //"[1.5,1.6]"
                else -> {
                    val groups = VERSION_REQUIREMENT_REGEX.find(it.trim())?.groups ?: throw InvalidVersionFormatException(it)
                    val minInclusive = "[" == groups[1]?.value
                    val maxInclusive = "]" == groups[5]?.value
                    var minString = normalize(groups[3]?.value?: "")
                    var maxString = normalize(groups[4]?.value?: "")
                    val min = if (minString.isNotBlank()) Version(minString) else (Version.MIN_VERSION)
                    val max = if (maxString.isNotBlank()) Version(maxString) else (Version.MAX_VERSION)
                    VersionRange(min = min, minInclusive = minInclusive, max = max, maxInclusive = maxInclusive)
                }
            }
        }.toTypedArray()
    }

}

class StrictVersionRequirement(versionRequirement: String): VersionRequirement(versionRequirement, STRICT) {

    override fun calculate(rawRequirement: String): Array<VersionRange> {
        val elements = rawRequirement.split("-")
        if (elements.size != 2) {
            throw InvalidStrictRequirementFormatException(rawRequirement)
        }

        val min = Version(elements[0].trim())
        val max = Version(elements[1].trim())

        return arrayOf(VersionRange(min=min, max=max))
    }

    override fun toString() = rawRequirement


}

class InvalidStrictRequirementFormatException(format: String): RuntimeException("Invalid format for STRICT requirement: $format")