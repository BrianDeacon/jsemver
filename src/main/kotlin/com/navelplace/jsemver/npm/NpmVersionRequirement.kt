package com.navelplace.jsemver.npm

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType
import com.navelplace.jsemver.ThrowingRequirementErrorListener
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
        private val arrow = '\u2194'
        private val dash = '\uFF0D'.toString()
        private val versionElement = """(?:0|[1-9]\d*)"""

        private val preReleaseEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(${versionElement}\.${versionElement}\.${versionElement})-""".toRegex()
        private val dashEscape = """(\s+[-])|(\s+[-]\s+)|([-]\s+)""".toRegex()
        private val vEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)[vV]""".toRegex()
        private val xEscape = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.)[xX]""".toRegex()
        private val xEscape2 = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.(?:0|[1-9]\d*)\.)[xX]""".toRegex()
        private val xEscape3 = """(\s|^|\=|\<\=|\>\=|\<|\>|~|\^)(0|[1-9]\d*)(\.\*\.)[xX]""".toRegex()

        /*
        The characters "x", "X", "v", "V", and "-" are ambiguous for the parser.
        v1.1.1-snapshot-x-v.v.1.2.3
        Remove the extraneous v in "v1.1.1-vvv", but leave other v's alone.
        In "1.1.1-SNAPSHOT-1" make the prerelease separator a distinct character.
        In "2.0 - 3.0" make the dash yet another distinct character
        Change the x's in "1.x.X" to "*". But don't change x's in "1.1.1-x.y.z"
        After escaping, v's, x's and dashes will only appear in either the prerelease or build.
        The following are then unambiguous to ANTLR:
        1.1.1[DASH]PrereleaseWithXandV+buildWithXAndV || 1.0 [ARROW] 2.0 || 1.*.*
         */
        private fun disambiguate(requirement: String): String {
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

        internal fun isWildcard(value: String?): Boolean {
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

        fun isValid(versionRequirement: String) =
                try {
                    NpmVersionRequirement(versionRequirement)
                    true
                } catch (e: InvalidRequirementFormatException) {
                    false
                }
    }

    private val requirement: Clause

    /**
     * Parses an npm-compatible requirement string from the [rawRequirement] argument
     */
    constructor(rawRequirement: String): super(rawRequirement.trim(), RequirementType.NPM) {
        val parser = parserFor(disambiguate(this.rawRequirement))
        val intersections = parser.union().intersection()
        this.requirement = OrClause(intersections.map { intersection ->
            val operatorClauses: Collection<Clause> = intersection.operatorClause().map { OperatorClauseFactory.clauseFor(it) }
            val dashClauses = intersection.dashClause().map { DashClause(it) }
            val clauses = ArrayList<Clause>()
            clauses.addAll(operatorClauses)
            clauses.addAll(dashClauses)
            AndClause(clauses)
        })
    }

    override fun isSatisfiedBy(version: Version) = requirement.isSatisfiedBy(version)

}

