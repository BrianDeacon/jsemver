package com.navelplace.jsemver.ruby

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType
import com.navelplace.jsemver.ThrowingRequirementErrorListener
import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.VersionRequirement
import com.navelplace.jsemver.antlr.RubyLexer
import com.navelplace.jsemver.antlr.RubyParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream


private enum class RubyOperator {
    EQ, GT, LT, GTEQ, LTEQ, TWIDDLE_WAKA;

    companion object {
        private val matches = mapOf(
                "=" to "EQ",
                ">" to "GT",
                "<" to "LT",
                "<=" to "LTEQ",
                ">=" to "GTEQ",
                "~>" to "TWIDDLE_WAKA")

        fun forString(value: String?): RubyOperator? {
            if (value == null) return null
            return RubyOperator.valueOf(matches.getValue(value.toUpperCase()))
        }
    }
}

/**
 * [VersionRequirement] implementation based on Ruby Gems version range specification
 * at http://guides.rubygems.org/patterns/#semantic-versioning
 */
class RubyGemsVersionRequirement: VersionRequirement {

    /**
     * @suppress
     */
    companion object {
        /**
         * Verifies that [versionRequirement] is a valid requirement string according
         * to the Ruby standard
         * @param versionRequirement The ruby requirement as a string
         * @return True if the provided string is a valid Ruby Gems requirement
         */
        @JvmStatic fun isValid(versionRequirement: String): Boolean {
            return try {
                val parser = parserFor(versionRequirement)
                parser.clauses() != null
            } catch(e: InvalidRequirementFormatException) {
                false
            }
        }

        private fun parserFor(value: String): RubyParser {
            val lexer = RubyLexer(CharStreams.fromString(value.trim()))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            val parser = RubyParser(CommonTokenStream(lexer))
            parser.addErrorListener(listener)
            return parser
        }

        // "~>" is called a Twiddle Waka. I'm not being cute. The Ruby folks are
        // being cute. Like they do.
        internal fun twiddleWaka(version: Version): VersionRange {
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
            var max: Version? = null
            when {
            //0.0.0, 0.0, 0, 1.0.0, 1.0, 1
                version.minor + version.patch == 0 -> max = Version.MAX_VERSION
            //0.1.0, 1.1.0
                minor !=0 && patch == 0 -> { major += 1; minor = 0; patch = 0; }
            //0.1.2, 1.1.2
                minor != 0 && patch != 0 -> { minor += 1; patch = 0 }

                else -> max = Version.MAX_VERSION
            }
            return VersionRange(min = min, max = max?: Version(major, minor, patch), minInclusive = true, maxInclusive = false)
        }

        /**
         * Parses [versionRequirement] into an instance of [RubyGemsVersionRequirement]
         */
        @JvmStatic fun fromString(versionRequirement: String) = RubyGemsVersionRequirement(versionRequirement)
    }

    private val validRanges: Array<VersionRange>

    /**
     * Parses a Ruby Gems requirement string from the [rawRequirement] argument
     */
    constructor(versionRequirement: String): super(versionRequirement, RequirementType.RUBY) {
        val parser = parserFor(versionRequirement)
        this.validRanges = parser.clauses().clause().map {
            val clause = it.unquotedClause()
            val version = clause.version()
            val major = version?.major()?.text?.toInt()?: 0
            val minor = version?.minor()?.text?.toInt()?: 0
            val patch = version?.patch()?.text?.toInt()?: 0

            val operatorString = clause?.operator()?.text
            val operator = if (operatorString != null) RubyOperator.forString(operatorString) else RubyOperator.EQ

            val min = Version(major, minor, patch)
            when(operator) {
                null, RubyOperator.EQ -> VersionRange(min = min, max = min)
                RubyOperator.GT -> VersionRange(min=min, max = Version.MAX_VERSION, minInclusive = false, maxInclusive = true)
                RubyOperator.GTEQ -> VersionRange(min=min, max = Version.MAX_VERSION, minInclusive = true, maxInclusive = true)
                RubyOperator.LT -> VersionRange(min=Version.MIN_VERSION, max = min, minInclusive = true, maxInclusive = false)
                RubyOperator.LTEQ -> VersionRange(min=Version.MIN_VERSION, max = min, minInclusive = true, maxInclusive = true)
                RubyOperator.TWIDDLE_WAKA -> twiddleWaka(Version(major, minor, patch))
            }
        }.toTypedArray()
    }


    override fun isSatisfiedBy(version: Version) = validRanges.any { it.contains(version) }

}
