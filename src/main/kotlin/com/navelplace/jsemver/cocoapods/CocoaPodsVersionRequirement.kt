package com.navelplace.jsemver.cocoapods

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType
import com.navelplace.jsemver.ThrowingRequirementErrorListener
import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.VersionRequirement
import com.navelplace.jsemver.antlr.CocoaPodsLexer
import com.navelplace.jsemver.antlr.CocoaPodsParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

enum class CocoaPodsOperator {
    EQ, GT, LT, GTEQ, LTEQ, TILDE;
    companion object {
        private val matches = mapOf(
                "=" to "EQ",
                ">" to "GT",
                "<" to "LT",
                "<=" to "LTEQ",
                ">=" to "GTEQ",
                "~>" to "TILDE")

        fun forString(value: String?): CocoaPodsOperator? {
            if (value == null) return null
            return CocoaPodsOperator.valueOf(matches.getValue(value.toUpperCase()))
        }
    }

}
class CocoaPodsVersionRequirement: VersionRequirement {
    companion object {
        fun parserFor(value: String): CocoaPodsParser {
            val lexer = CocoaPodsLexer(CharStreams.fromString(value))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            val parser =  CocoaPodsParser(CommonTokenStream(lexer))
            parser.addErrorListener(listener)
            return parser
        }

        fun isValid(versionRequirement: String): Boolean {
            try {
                val parser = parserFor(versionRequirement)
                return parser.requirement() != null
            } catch(e: InvalidRequirementFormatException) {
                return false
            }

        }
        fun tilde(version: Version): VersionRange {
            /*
            '~> 0.1.2' >0.1.2 <0.2.0
            '~> 1.1.2' >1.1.2 <1.2.0
            '~> 0.1' >=0.1.0 <1.0.0
            '~> 1.1' >=1.1.0 <2.0.0
            '~> 0' >=0.0.0
            '~> 1' >=1.0.0
             */
            var major = version.major
            var minor = version.minor
            var patch = version.patch
            val min = Version(major, minor, patch)
            var max = Version.MAX_VERSION
            when {
                //0.0.0, 0.0, 0
                version.major + version.minor + version.patch == 0 -> max = Version.MAX_VERSION
                //1.0.0
                major != 0 && minor == 0 && patch == 0 -> major += 1
                //0.1.0, 1.1.0
                minor !=0 && patch == 0 -> { major += 1; minor = 0; patch = 0; }
                //0.1.2, 1.1.2
                minor != 0 && patch != 0 -> { minor += 1; patch = 0 }

                else -> max = Version.MAX_VERSION
            }
            return VersionRange(min = min, max = Version(major, minor, patch), minInclusive = true, maxInclusive = false)
        }
    }
    val validRanges: Array<VersionRange>

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
            CocoaPodsOperator.TILDE -> tilde(version)
            else -> throw RuntimeException("")
        }

        this.validRanges = arrayOf(range)
    }

    override fun isSatisfiedBy(version: Version) = validRanges.any { it.contains(version) }

}
