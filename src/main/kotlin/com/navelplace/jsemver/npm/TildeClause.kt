package com.navelplace.jsemver.npm

import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.antlr.NPMParser

/**
 * @suppress
 */
class TildeClause(context: NPMParser.OperatorClauseContext) : OperatorClause(context) {
    override fun rangeFor(versionContext: NPMParser.VersionContext): VersionRange {
        return VersionRange(min = minFor(versionContext), max = maxFor(versionContext), minInclusive = true, maxInclusive = false)
    }

    override fun maxFor(versionContext: NPMParser.VersionContext): Version {
        var major = versionContext.major().text.toInt()
        val minorWildcard = NpmVersionRequirement.isWildcard(versionContext.minor()?.text)
        val patchWildcard = NpmVersionRequirement.isWildcard(versionContext.patch()?.text)
        var minor = if (minorWildcard) 0 else versionContext.minor()!!.text.toInt()
        var patch = if (patchWildcard) 0 else versionContext.patch()!!.text.toInt()

        when {
        // ~0 := >=0.0.0 <(0+1).0.0 := >=0.0.0 <1.0.0 (Same as 0.x)
            major == 0 && minorWildcard && patchWildcard -> { major += 1; minor = 0; patch = 0; }
        //~0.2 := >=0.2.0 <0.(2+1).0 := >=0.2.0 <0.3.0 (Same as 0.2.x)
            major == 0 && !minorWildcard && patchWildcard -> { major = major; minor += 1; patch = 0; }
        //~1.2.3 := >=1.2.3 <1.(2+1).0 := >=1.2.3 <1.3.0
            major > 0 && minor > 0 && patch > 0 -> { major = major; minor += 1; patch = 0 }
        //~1.2 := >=1.2.0 <1.(2+1).0 := >=1.2.0 <1.3.0 (Same as 1.2.x)
            major > 0 && minor > 0 && patchWildcard -> { major = major; minor += 1; patch = 0 }
        //~1 := >=1.0.0 <(1+1).0.0 := >=1.0.0 <2.0.0 (Same as 1.x)
            major > 0 && minorWildcard && patchWildcard -> { major += 1; minor = 0; patch = 0 }
        //~0.2.3 := >=0.2.3 <0.(2+1).0 := >=0.2.3 <0.3.0
            major == 0 && !minorWildcard && !patchWildcard -> { major = 0; minor += 1; patch = 0; }
        }
        return Version(major, minor, patch)
    }

    override fun isSatisfiedBy(version: Version) =
            if (version.preRelease.isNotBlank()) {
                version.major == range.min.major &&
                        version.minor == range.min.minor &&
                        version.patch == range.min.patch &&
                        range.contains(version)

            } else {
                range.contains(version)
            }
}
