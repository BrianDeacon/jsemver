package com.navelplace.jsemver

data class VersionRange(val min: Version, val minInclusive: Boolean = true, val max: Version, val maxInclusive: Boolean = true) {
    fun contains(version: Version): Boolean {
        if (minInclusive && version.equivalentTo(min)) return true
        if (maxInclusive && version.equivalentTo(max)) return true
        return version.greaterThan(min) && version.lessThan(max)
    }
}