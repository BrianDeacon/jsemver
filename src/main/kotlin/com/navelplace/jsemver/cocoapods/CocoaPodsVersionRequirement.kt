package com.navelplace.jsemver.cocoapods

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType
import com.navelplace.jsemver.ThrowingRequirementErrorListener
import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.VersionRequirement
import com.navelplace.jsemver.antlr.CocoaPodsLexer
import com.navelplace.jsemver.antlr.CocoaPodsParser
import com.navelplace.jsemver.ruby.RubyGemsVersionRequirement
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * @suppress
 */
private enum class CocoaPodsOperator {
    EQ, GT, LT, GTEQ, LTEQ, TWIDDLE_WAKA;
    companion object {
        private val matches = mapOf(
                "=" to "EQ",
                ">" to "GT",
                "<" to "LT",
                "<=" to "LTEQ",
                ">=" to "GTEQ",
                "~>" to "TWIDDLE_WAKA")

        fun forString(value: String?): CocoaPodsOperator? {
            if (value == null) return null
            return CocoaPodsOperator.valueOf(matches.getValue(value.toUpperCase()))
        }
    }

}

/**
 * [VersionRequirement] implementation based on the Ruby CocoaPods spec:
 * https://guides.cocoapods.org/using/the-podfile.html
 */
class CocoaPodsVersionRequirement: VersionRequirement {
    /**
     * @suppress
     */
    companion object {
        private fun parserFor(value: String): CocoaPodsParser {
            val lexer = CocoaPodsLexer(CharStreams.fromString(value))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            val parser =  CocoaPodsParser(CommonTokenStream(lexer))
            parser.addErrorListener(listener)
            return parser
        }

        private fun twiddleWaka(version: Version) = RubyGemsVersionRequirement.twiddleWaka(version)

        /**
         * Returns true if the given [versionRequirement] is a valid CocoaPods requirement string
         * @param versionRequirement The string to test
         * @return True if the supplied string is a valid CocoaPodsVersionRequirement
         */
        @JvmStatic fun isValid(versionRequirement: String) = try {
            parserFor(versionRequirement).requirement() != null
        } catch(e: InvalidRequirementFormatException) {
            false
        }

        /**
         * Parses a CocoaPods requirement string from the [versionRequirement] argument
         * @param versionRequirement The requirement string to parse
         * @return A CocoaPodsVersionRequirement if the given string was valid.
         */
        @JvmStatic fun fromString(versionRequirement: String) = CocoaPodsVersionRequirement(versionRequirement)
    }


    private val validRanges: Array<VersionRange>

    /**
     * Parses a CocoaPods requirement string from the [requirement] argument
     */
    constructor(requirement: String):super(requirement, RequirementType.COCOAPODS) {
        val parser = parserFor(requirement)
        val operatorString = parser.operator()?.text
        val parserVersion = parser.requirement().version()
        val major = parserVersion.major()!!.text.toInt()
        val minor = parserVersion.minor()?.text?.toInt()?: 0
        val patch = parserVersion.patch()?.text?.toInt()?: 0

        val operator = if (operatorString.isNullOrBlank()) CocoaPodsOperator.EQ else CocoaPodsOperator.forString(operatorString)

        val version = Version(major, minor, patch)

        val range = when(operator) {
            CocoaPodsOperator.EQ -> VersionRange(min=version, max=version)
            CocoaPodsOperator.GT -> VersionRange(min=version, max=Version.MAX_VERSION, minInclusive = false, maxInclusive = true)
            CocoaPodsOperator.GTEQ -> VersionRange(min=version, max=Version.MAX_VERSION, minInclusive = true, maxInclusive = true)
            CocoaPodsOperator.LTEQ -> VersionRange(min= Version.MIN_VERSION, max=version, minInclusive = true, maxInclusive = true)
            CocoaPodsOperator.LT -> VersionRange(min= Version.MIN_VERSION, max=version, minInclusive = true, maxInclusive = false)
            CocoaPodsOperator.TWIDDLE_WAKA -> twiddleWaka(version)
            else -> throw RuntimeException("")
        }

        this.validRanges = arrayOf(range)
    }

    override fun isSatisfiedBy(version: Version) = validRanges.any { it.contains(version) }
}
