package com.navelplace.jsemver.npm

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType
import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRequirement
import com.navelplace.jsemver.antlr.NPMLexer
import com.navelplace.jsemver.antlr.NPMParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

class NpmVersionRequirement : VersionRequirement {

    /**
     * @suppress
     */
    companion object {
        val arrow = '\u2194'
        val dash = '\uFF0D'.toString()
        val versionElement = """(?:0|[1-9]\d*)"""

        private val preReleaseEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(${versionElement}\.${versionElement}\.${versionElement})-""".toRegex()
        private val dashEscape = """(\s+[-])|(\s+[-]\s+)|([-]\s+)""".toRegex()
        private val vEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)[vV]""".toRegex()
        private val xEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.)[xX]""".toRegex()
        private val xEscape2 = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.(?:0|[1-9]\d*)\.)[xX]""".toRegex()
        private val xEscape3 = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.\*\.)[xX]""".toRegex()

        private fun escape(requirement: String): String {
            var escaped = requirement.replace(dashEscape, " ${arrow} ")
            vEscape.findAll(escaped).forEach {
                escaped = escaped.replaceFirst(vEscape, "${it.groups[1]!!.value}")
            }
            preReleaseEscape.findAll(escaped).forEach {
                escaped = escaped.replaceFirst(preReleaseEscape, "${it.groups[1]!!.value}${it.groups[2]!!.value}${dash}")
            }

            arrayOf(xEscape, xEscape2, xEscape3).forEach { regex ->
                regex.findAll(escaped).forEach {
                    escaped = escaped.replaceFirst(regex, "${it.groups[1]!!.value}${it.groups[2]!!.value}${it.groups[3]!!.value}*")
                }
            }

            return escaped
        }

        fun isWildcard(value: String?): Boolean {
            return  value.isNullOrBlank() ||
                    value == "*"
        }

        internal fun parserFor(value: String): NPMParser {
            val lexer = NPMLexer(CharStreams.fromString(value))
            val parser = NPMParser(CommonTokenStream(lexer))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            parser.addErrorListener(listener)
            return parser
        }

        private fun validate(major: String, minor: String?, patch: String?, preRelease: Array<String>? = null, build: Array<String>? = null) {
            val preReleaseArray = preRelease ?: emptyArray()
            val buildArray = build ?: emptyArray()
            if (isWildcard(minor) && !isWildcard(patch)) {
                throw InvalidRequirementFormatException("$major.$minor.$patch")
            }
            if (
                    (isWildcard(minor) || isWildcard(patch)) &&
                    (preReleaseArray.isNotEmpty() || buildArray.isNotEmpty())
            ) {
                val pre = preReleaseArray.joinToString(".")
                val b = buildArray.joinToString(".")
                //TODO: This might not format the error correctly if pre or b are empty
                throw InvalidRequirementFormatException("$major.$minor.$patch-$pre+$b")
            }
        }
        internal fun maxFor(major: String, minor: String?, patch: String?, preRelease: Array<String>? = null, build: Array<String>? = null): Version {
            var majorInt = major.toInt()
            var minorInt = if (isWildcard(minor)) 0 else minor!!.toInt()
            var patchInt = if (isWildcard(patch)) 0 else patch!!.toInt()

            var preReleaseArray = preRelease ?: emptyArray()
            var buildArray = build ?: emptyArray()
            if (isWildcard(minor) || isWildcard(patch)) {
                preReleaseArray = emptyArray()
                buildArray = emptyArray()
            }




            if (!isWildcard(patch) && isWildcard(minor)) {
                throw InvalidRequirementFormatException("$major.$minor.$patch")
            }
            if (isWildcard(patch) && isWildcard(minor)) {
                majorInt++
            }
            if (isWildcard(patch) && !isWildcard(minor)) {
                minorInt++
            }

            validate(major, minor, patch, preRelease, build)
            return Version(majorInt, minorInt, patchInt, preReleaseArray, buildArray)
        }

        internal fun minFor(major: String, minor: String?, patch: String?, preRelease: Array<String>? = null, build: Array<String>? = null): Version {
            val majorInt = major.toInt()
            val minorInt = if (isWildcard(minor)) 0 else minor!!.toInt()
            val patchInt = if (isWildcard(patch)) 0 else patch!!.toInt()
            var preReleaseArray = preRelease ?: emptyArray()
            var buildArray = build ?: emptyArray()
            if (isWildcard(minor) || isWildcard(patch)) {
                preReleaseArray = emptyArray()
                buildArray = emptyArray()
            }


            if (!isWildcard(patch) && isWildcard(minor)) {
                throw InvalidRequirementFormatException("$major.$minor.$patch")
            }
            validate(major, minor, patch, preRelease, build)
            return Version(majorInt, minorInt, patchInt, preReleaseArray, buildArray)
        }
    }

    private val orClause: Clause

    /**
     * Parses an npm-compatible requirement string from the [rawRequirement] argument
     */
    constructor(rawRequirement: String): super(rawRequirement.trim(), RequirementType.NPM) {
        val parser = parserFor(escape(this.rawRequirement))
        val intersections = parser.union().intersection()
        this.orClause = OrClause(intersections.map { intersection ->
            val operatorClauses: Collection<Clause> = intersection.operatorClause().map { OperatorClauseFactory.clauseFor(it, this.rawRequirement) }
            val dashClauses = intersection.dashClause().map { DashClause(it) }
            val clauses = ArrayList<Clause>()
            clauses.addAll(operatorClauses)
            clauses.addAll(dashClauses)
            AndClause(clauses)
        })
    }

    override fun isSatisfiedBy(version: Version) = orClause.isSatisfiedBy(version)

}

