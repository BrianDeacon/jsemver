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


enum class RubyOperator {
    EQ, GT, LT, GTEQ, LTEQ;

    companion object {
        private val matches = mapOf(
                "=" to "EQ",
                ">" to "GT",
                "<" to "LT",
                "<=" to "LTEQ",
                ">=" to "GTEQ")

        fun forString(value: String?): RubyOperator? {
            if (value == null) return null
            return RubyOperator.valueOf(matches.getValue(value.toUpperCase()))
        }
    }
}
class RubyGemsVersionRequirement: VersionRequirement {

    companion object {
        fun isValid(versionRequirement: String): Boolean {
            return try {
                val parser = parserFor(versionRequirement)
                parser.clauses() != null
            } catch(e: InvalidRequirementFormatException) {
                false
            }
        }

        private fun parserFor(value: String): RubyParser {
            val lexer = RubyLexer(CharStreams.fromString(value))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            val parser = RubyParser(CommonTokenStream(lexer))
            parser.addErrorListener(listener)
            return parser
        }
    }

    val validRanges: Array<VersionRange>

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
            }
        }.toTypedArray()
    }


    override fun isSatisfiedBy(version: Version) = validRanges.any { it.contains(version) }

}
