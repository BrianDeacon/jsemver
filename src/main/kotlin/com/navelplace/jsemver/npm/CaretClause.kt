package com.navelplace.jsemver.npm

import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.antlr.NPMParser


/**
 * @suppress
 */
class CaretClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = minFor(versionContext), max = maxFor(versionContext), minInclusive = true, maxInclusive = false)
    }

    override fun isSatisfiedBy(otherVersion: Version) =
            if (otherVersion.preRelease.isNotBlank()) {
                /*
                ^1.2.3-beta.2 := >=1.2.3-beta.2 <2.0.0
                Note that prereleases in the 1.2.3 version will be allowed, if they are greater than or equal to beta.2.
                So, 1.2.3-beta.4 would be allowed, but 1.2.4-beta.2 would not, because it is a prerelease of a different
                [major, minor, patch] tuple.

                ^0.0.3-beta := >=0.0.3-beta <0.0.4
                Note that prereleases in the 0.0.3 version only will be allowed, if they are greater than or equal to beta.
                So, 0.0.3-pr.2 would be allowed.
                */
                otherVersion.major == range.min.major &&
                        otherVersion.minor == range.min.minor &&
                        otherVersion.patch == range.min.patch &&
                        range.contains(otherVersion)

            } else {
                range.contains(otherVersion)
            }

    override fun maxFor(versionContext: NPMParser.VersionContext): Version {
        var major = versionContext.major().text.toInt()
        val minorWildcard = NpmVersionRequirement.isWildcard(versionContext.minor()?.text)
        val patchWildcard = NpmVersionRequirement.isWildcard(versionContext.patch()?.text)
        var minor = if (minorWildcard) 0 else versionContext.minor()!!.text.toInt()
        var patch = if (patchWildcard) 0 else versionContext.patch()!!.text.toInt()
        when {
        // ^0.x := >=0.0.0 <1.0.0
            major + minor + patch == 0 && minorWildcard -> { major = 1; minor = 0; patch = 0; }

        // ^0.0.x := >=0.0.0 <0.1.0
        // ^0.0 := >=0.0.0 <0.1.0
            major + minor + patch == 0 &&  patchWildcard && !minorWildcard -> { major = major; minor += 1; patch = 0 }

        // ^1.2.x := >=1.2.0 <2.0.0
        // ^1.2.3 := >=1.2.3 <2.0.0
        // ^2.3.4 := >=2.3.4 <3.0.0
        // ^1.2.3-beta.2 := >=1.2.3-beta.2 <2.0.0
        // ^1.x := >=1.0.0 <2.0.0
            major > 0 -> { major += 1; minor = 0; patch = 0; }

        // ^0.0.3 := >=0.0.3 <0.0.4
            major == 0 && minor == 0 && !patchWildcard && !minorWildcard -> { major = major; minor = minor; patch += 1; }

        // ^0.2.3 := >=0.2.3 <0.3.0
            major == 0 && !patchWildcard && !minorWildcard -> { major = major; minor += 1; patch = 0; }

        // ^0.0.3 := >=0.0.3 <0.0.4
            major == 0 && minor == 0 && patch > 0 -> { major = major; minor = minor; patch += 1; }
        }

        return Version(major, minor, patch)
    }
}


