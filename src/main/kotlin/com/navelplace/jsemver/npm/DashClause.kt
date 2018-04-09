package com.navelplace.jsemver.npm

import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.antlr.NPMParser

/**
 * @suppress
 */
class DashClause : Clause {

    val validRange: VersionRange

    constructor(context: NPMParser.DashClauseContext) {
        val min = context.version(0)
        val max = context.version(1)

        var legalMinChars = min.preRelease()?.dottedLegal()?.legalCharacters()
        var legalMaxChars = max.preRelease()?.dottedLegal()?.legalCharacters()
        val preReleaseMin = if (legalMinChars != null) legalMinChars.map { it.text }.toTypedArray() else emptyArray()
        val preReleaseMax = if (legalMaxChars != null) legalMaxChars.map { it.text }.toTypedArray() else emptyArray()

        legalMinChars = min.build()?.dottedLegal()?.legalCharacters()
        legalMaxChars = max.build()?.dottedLegal()?.legalCharacters()
        val buildMin = if (legalMinChars != null) legalMinChars.map { it.text }.toTypedArray() else emptyArray()
        val buildMax = if (legalMaxChars != null) legalMaxChars.map { it.text }.toTypedArray() else emptyArray()

        val minVersion = NpmVersionRequirement.minFor(min.major().text, min.minor()?.text, min.patch()?.text, preReleaseMin, buildMin)
        val maxVersion = NpmVersionRequirement.maxFor(max.major().text, min.minor()?.text, min.patch()?.text, preReleaseMax, buildMax)
        this.validRange = VersionRange(min = minVersion, max = maxVersion)
    }

    override fun isSatisfiedBy(version: Version): Boolean {
        return version.preReleaseElements.isEmpty() &&
                version.metadataElements.isEmpty() &&
                validRange.contains(version)
    }
}

