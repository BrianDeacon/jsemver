package com.navelplace.jsemver.maven

import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.antlr.MavenLexer
import com.navelplace.jsemver.antlr.MavenParser
import com.navelplace.jsemver.npm.ThrowingRequirementErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

/**
 * @suppress
 */
internal class ParsedVersion {
    val major: String?
    val minor: String?
    val patch: String?

    constructor(major: String?, minor: String?, patch: String?) {
        this.major = checkZero(major)
        this.minor = checkZero(minor)
        this.patch = checkZero(patch)
    }

    private fun checkZero(value: String?): String? {
        if (value != null && value.length > 1 && value.startsWith("0")) {
            throw InvalidMavenVersionRequirementFormatException(value)
        } else {
            return value
        }
    }

    override fun toString(): String {
        val minorString = if (minor != null) ".$minor" else ""
        val patchString = if (patch != null) ".$patch" else ""
        return "$major$minorString$patchString"
    }
}

/**
 * @suppress
 */
internal class ParsedRange {
    val v1: ParsedVersion?
    val v2: ParsedVersion?
    val comma: String?
    val open: String?
    val close: String?

    override fun toString(): String {
        return "$open$v1$comma$v2$close"
    }

    fun hasNoBrackets() = open == null && close == null
    fun bracketsValid() = (open == null && close == null) || (open != null && close != null)
    fun isEmpty() = v1 == null && v2 == null

    constructor(v1: ParsedVersion?, v2: ParsedVersion?, comma: String?, open: String?, close: String?, entire: String) {
        this.comma = comma
        this.open = open
        this.close = close
        val stripped = entire
                .trim()
                .removePrefix("(")
                .removePrefix("[")
                .removeSuffix(")")
                .removeSuffix("]")
                .trim()
        if (v2 == null && stripped.startsWith(",")) {
            this.v1 = null
            this.v2 = v1
        } else {
            this.v1 = v1
            this.v2 = v2
        }
    }

    fun toVersionRange(): VersionRange {
        val minInclusive = open == null || open == "["
        val maxInclusive = close == null || close == "]"

        val min = this.min()
        val max = when {
            open == null && close == null -> Version.MAX_VERSION // "1.5"
            this.comma == null -> min //[1.5]
            else -> this.max()
        }

        return VersionRange(min = min, max = max, minInclusive = minInclusive, maxInclusive = maxInclusive)
    }

    fun min(): Version {
        //"[,]" or "[,2]"
        return if (v1 == null) {
            Version.MIN_VERSION
        } else {
            //[1.1,2.2] or [1.1,] or 1.5 or [1.5]
            val major = v1?.major!!.toInt()
            val minor = v1.minor?.toInt() ?: 0
            val patch = v1.patch?.toInt() ?: 0
            Version(major, minor, patch)
        }
    }

    fun max(): Version {
        if (hasNoBrackets()) {
            return Version.MAX_VERSION
        }
        return if (v2 == null) {
            Version.MAX_VERSION
        } else {
            val major = v2.major?.toInt()?: 0
            val minor = v2.minor?.toInt()?: 0
            val patch = v2.patch?.toInt()?: 0
            Version(major, minor, patch)
        }
    }
}

/**
 * @suppress
 */
internal class ParsedRanges {
    companion object {
        private fun parserFor(value: String): MavenParser {
            val lexer = MavenLexer(CharStreams.fromString(value))
            val parser = MavenParser(CommonTokenStream(lexer))
            val listener = ThrowingRequirementErrorListener(value)
            lexer.addErrorListener(listener)
            parser.addErrorListener(listener)
            return parser
        }
    }

    val ranges: Array<ParsedRange>

    constructor(ranges: Array<ParsedRange>) {
        this.ranges = ranges
    }

    constructor(requirement: String): this(parserFor(requirement))

    constructor(parser: MavenParser) {
        this.ranges = parser.ranges().range().map {
            val versions = it.version().map {
                ParsedVersion(it?.major()?.text, it?.minor()?.text, it?.patch()?.text)
            }.toTypedArray()
            val v1 = if (versions.size > 0) versions[0] else null
            val v2 = if (versions.size > 1) versions[1] else null
            ParsedRange(v1, v2, it?.COMMA()?.text, it?.open()?.text, it?.close()?.text, it.text)
        }.toTypedArray()
    }

    override fun toString(): String {
        return "${ranges.joinToString(",")}"
    }

    fun toVersionRanges() = ranges.map { it.toVersionRange() }.toTypedArray()
}
