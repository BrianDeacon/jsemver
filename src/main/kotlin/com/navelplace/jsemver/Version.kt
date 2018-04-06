package com.navelplace.jsemver

import com.navelplace.jsemver.RequirementType.*
import com.navelplace.jsemver.antlr.VersionLexer
import com.navelplace.jsemver.antlr.VersionParser as AntlrParser
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

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
    val preReleaseElements: Array<String>
    val metadataElements: Array<String>

    /**
     * @suppress
     */
    companion object {
        /**
         * No [Version] is greater than [MAX_VERSION]
         */
        @JvmField val MAX_VERSION = Version(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        /**
         * No [Version] is less than [MIN_VERSION]
         */
        @JvmField val MIN_VERSION = Version(Int.MIN_VALUE,Int.MIN_VALUE,Int.MIN_VALUE)

        /**
         * Checks [version] for correct formatting
         * @param version The string to be tested
         * @return True if the version string is correctly formatted
         */
        @JvmStatic fun isValid(version: String): Boolean {
            try {
                Version.fromString(version)
                return true
            }
            catch(e: InvalidVersionFormatException) {
                return false
            }
        }

        /**
         * Constructs a [Version] based on the value of [version]
         * @param version The string to be converted to a [Version] instance
         * @return The constructed [Version]
         * @throws [InvalidVersionFormatException]
         */
        @JvmStatic fun fromString(version: String) = Version(version)

        private fun escape(value: String): String {
            val elements = value.split("-")
            if (elements.size == 1) return value
            return elements[0] + "&" + elements.subList(1, elements.size).joinToString("-")
        }
    }

    /**
     * Parses [version] as a Semver-compliant string
     * @param version The string representation of the [Version]
     */
    constructor(version: String) {
        raw = version.trim()

        /*
          Brutal Hack(tm):
          1.0.0-has-dashes+build-number is valid
          It's remarkably hard to make ANTLR aware of the first dash without all the other
          ones making it mad. So we just make that first dash a special character:
          1.0.0&has-dashes+build-number
          The ANTLR grammar is looking for the '&' where you'd think a dash would be.
          A developer of stronger moral fiber would figure out how to get ANTLR to do it.
        */
        val escaped = escape(raw)
        val lexer = VersionLexer(CharStreams.fromString(escaped))
        val parser = AntlrParser(CommonTokenStream(lexer))

        val listener = ThrowingErrorListener(raw)
        lexer.addErrorListener(listener)
        parser.addErrorListener(listener)

        val fullVersion = parser.fullVersion()
        val semver = fullVersion.semver()

        val prereleaseElement = fullVersion?.prerelease()?.prereleaseElement()?: emptyList()
        this.preReleaseElements = prereleaseElement.map { it.text }.toTypedArray()
        val build = fullVersion?.build()?.buildElement()?: emptyList()
        this.metadataElements = build.map { it.text }.toTypedArray()

        this.preRelease = preReleaseElements.joinToString(".")
        this.metadata = metadataElements.joinToString(".")

        this.major = semver.major.text.toInt()
        this.minor = semver.minor.text.toInt()
        this.patch = semver.patch.text.toInt()

    }


    constructor(major: Int, minor: Int, patch: Int, prerelease: Array<String> = emptyArray(), build: Array<String> = emptyArray()) {
        this.major = major
        this.minor = minor
        this.patch = patch
        this.preReleaseElements = prerelease
        this.metadataElements = build
        this.preRelease = prerelease.joinToString(".")
        this.metadata = build.joinToString(".")
        val buildString = if (metadata.isNotEmpty()) "+$metadata" else ""
        val tail = if(preRelease.isNotEmpty()) "-$preRelease$buildString" else ""
        this.raw = "$major.$minor.$patch$tail"
    }

    /**
     * The raw string representation of the [Version].
     */
    override fun toString() = raw

    /**
     * Tests for literal object equivalence. For semantic equivalence see [equivalentTo]
     * @param other The other [Version] to be compared
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
     * Tests if another [Version] is newer
     * @param other The other [Version] to be compared
     * @return True if [other] is semantically newer.
     */
    @JvmName("isNewerThan")
    fun newerThan(other: Version): Boolean {
        return compareTo(other) > 0
    }

    /**
     * Tests if another [Version] is older
     * @param other The other [Version] to be compared
     * @return True if [other] is semantically older.
     */
    @JvmName("isOlderThan")
    fun olderThan(other: Version): Boolean {
        return compareTo(other) < 0
    }

    /**
     * Tests [other] against this instance for version equivalence.
     *
     * For example, `1.1.1-alpha+beta` is semantically equivalent to `1.1.1-alpha+gamma`
     *
     * @param other The other [Version] to be compared
     * @return True if [other] is semantically equivalent.
     */
    @JvmName("isEquivalentTo")
    fun equivalentTo(other: Version): Boolean {
        return compareTo(other) == 0
    }

    /**
     * Parses [versionRequirement] as a [VersionRequirement] and tests if this [Version] satisfies the
     * requirement
     *
     * @param versionRequirement The string representation of the [VersionRequirement]
     * @param type The specific [VersionRequirement] implementation as defined by the [RequirementType]
     * @return True if the requirement is satisfied
     * @see VersionRequirement
     * @see RequirementType
     */
    fun satisfies(versionRequirement: String, type: RequirementType = SIMPLE): Boolean {
        return satisfies(VersionRequirement.fromString(versionRequirement, type))
    }

    /**
     * Tests if this [Version] satisfies [versionRequirement]
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

        //If one array is longer, we initially disregard the extra elements
        for (i in 0..Math.min(preReleaseElements.size - 1, other.preReleaseElements.size - 1)) {
            compare = elementCompare(preReleaseElements[i], other.preReleaseElements[i])
            if (compare != 0) return compare
        }

        // If we got here, the versions are identical except that one preRelease may be longer than the other
        // The shorter one comes first, so just compare the number of elements in preRelease
        return preReleaseElements.size.compareTo(other.preReleaseElements.size)
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
 * @suppress
 */
class InvalidVersionFormatException(val format: String): RuntimeException("Invalid version format: $format")

/**
 * @suppress
 */
class ThrowingErrorListener(val input: String) : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        throw InvalidVersionFormatException(input)
    }
}