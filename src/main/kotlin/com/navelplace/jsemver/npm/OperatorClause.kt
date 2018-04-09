package com.navelplace.jsemver.npm

import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.antlr.NPMParser

/**
 * @suppress
 */
internal enum class Operator {
    EQ, GT, LT, GTEQ, LTEQ, TILDE, CARET;

    companion object {
        private val matches = mapOf(
                "=" to "EQ",
                ">" to "GT",
                "<" to "LT",
                "<=" to "LTEQ",
                ">=" to "GTEQ",
                "~" to "TILDE",
                "^" to "CARET")
        fun forString(value: String?): Operator? {
            if (value == null) return null
            return Operator.valueOf(matches.getValue(value.toUpperCase()))
        }
    }
}

/**
 * @suppress
 */
object OperatorClauseFactory {
    fun clauseFor(context: NPMParser.OperatorClauseContext, rawRequirement: String) : Clause {
        val operator = Operator.forString(context.operator()?.text?.toUpperCase())
        return when(operator) {
            null, Operator.EQ -> EqualClause(context)
            Operator.TILDE -> TildeClause(context)
            Operator.CARET -> CaretClause(context)
            Operator.LT -> LessThanClause(context)
            Operator.GTEQ -> GreaterThanEqualClause(context)
            Operator.GT -> GreaterThanClause(context)
            Operator.LTEQ -> LessThanEqualClause(context)
        }
    }
}


/**
 * @suppress
 */
abstract class OperatorClause : Clause {

    val range: VersionRange

    constructor(context: NPMParser.OperatorClauseContext) {
        this.range = rangeFor(context.version())
    }

    protected abstract fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange

    protected open fun minFor(versionContext: NPMParser.VersionContext): Version {
        val preRelease = versionContext.preRelease()?.dottedLegal()?.legalCharacters()?.map { it.text }?.toTypedArray()
        val build = versionContext.build()?.dottedLegal()?.legalCharacters()?.map { it.text }?.toTypedArray()
        return NpmVersionRequirement.minFor(versionContext.major().text, versionContext.minor()?.text, versionContext.patch()?.text, preRelease, build)
    }

    protected open fun maxFor(versionContext: NPMParser.VersionContext): Version {
        val preRelease = versionContext.preRelease()?.dottedLegal()?.legalCharacters()?.map { it.text }?.toTypedArray()
        val build = versionContext.build()?.dottedLegal()?.legalCharacters()?.map { it.text }?.toTypedArray()
        return NpmVersionRequirement.maxFor(versionContext.major().text, versionContext.minor()?.text, versionContext.patch()?.text, preRelease, build)
    }

    override fun isSatisfiedBy(version: Version): Boolean {
        return range.contains(version)
    }
}

/**
 * @suppress
 */
class EqualClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = minFor(versionContext), max = maxFor(versionContext), minInclusive = true, maxInclusive = false)
    }
}

/**
 * @suppress
 */
class GreaterThanClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = minFor(versionContext), max = Version.MAX_VERSION, minInclusive = false, maxInclusive = true)
    }
}

/**
 * @suppress
 */
class LessThanClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = Version.MIN_VERSION, max = minFor(versionContext), minInclusive = true, maxInclusive = false)
    }
}

/**
 * @suppress
 */
class GreaterThanEqualClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = minFor(versionContext), max = Version.MAX_VERSION, minInclusive = true, maxInclusive = true)
    }
}

/**
 * @suppress
 */
class LessThanEqualClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = Version.MIN_VERSION, max = minFor(versionContext), minInclusive = true, maxInclusive = true)
    }
}
