package com.navelplace.jsemver.npm

import com.navelplace.jsemver.Version

/**
 * @suppress
 */
interface Clause {
    fun isSatisfiedBy(version: Version): Boolean
}

/**
 * @suppress
 */
class AndClause(val predicates: Collection<Clause>): Clause {
    override fun isSatisfiedBy(version: Version) = predicates.all { it.isSatisfiedBy(version) }
}

/**
 * @suppress
 */
class OrClause(val predicates: Collection<Clause>): Clause {
    override fun isSatisfiedBy(version: Version) = predicates.any { it.isSatisfiedBy(version) }
}
