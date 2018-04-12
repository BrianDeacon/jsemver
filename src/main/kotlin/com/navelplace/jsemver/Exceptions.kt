package com.navelplace.jsemver

/**
 * @suppress
 */
open class SemverException(message: String): RuntimeException(message)


/**
 * @suppress
 */
class InvalidVersionFormatException(val format: String): SemverException("Invalid version format: $format")

/**
 * @suppress
 */
open class InvalidRequirementFormatException(message: String): SemverException(message)

