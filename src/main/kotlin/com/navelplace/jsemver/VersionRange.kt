package com.navelplace.jsemver

/**
 * An inclusively or exclusively bounded range of Versions
 *
 * @property min The mimimum Version
 * @property minInclusive Whether the range includes the value of [min]
 * @property max The maximum Version
 * @property maxInclusive Whether the range includes the value of [max]
 *
 * @author Brian Deacon (bdeacon@navelplace.com)
 */
data class VersionRange(val min: Version, val minInclusive: Boolean = true, val max: Version, val maxInclusive: Boolean = true) {

    /**
     * Tests a [Version] for inclusion in this range
     *
     * @param version The version to test for inclusion
     * @return True of the [version] is part of this range
     */
    fun contains(version: Version): Boolean {
        if (minInclusive && version.equivalentTo(min)) return true
        if (maxInclusive && version.equivalentTo(max)) return true
        return version.greaterThan(min) && version.lessThan(max)
    }
}