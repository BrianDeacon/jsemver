package com.navelplace.jsemver.maven

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.RequirementType.MAVEN
import com.navelplace.jsemver.Version
import com.navelplace.jsemver.VersionRange
import com.navelplace.jsemver.VersionRequirement

/**
 * [VersionRequirement] implementation based on Maven's version range specification
 * at https://maven.apache.org/enforcer/enforcer-rules/versionRanges.html
 *
 */
class MavenVersionRequirement: VersionRequirement {
    companion object {
        private val whitespace = """\s*"""
        private val comma = """[\,]"""
        private val dollar = "$"
        private val openBracket = """
            [
                \[
                \(
            ]
        """.stripWhitespace()
        private val closeBracket = """
            [
                \]
                \)
            ]
        """.stripWhitespace()

        private val versionNumber = """
            (?:
                (?:
                0
                )

                |

                (?:
                    [1-9]
                    \d*
                )
            )
        """.stripWhitespace()


        private val fullVersion = """
            (?:
                $whitespace
                (?:
                    $versionNumber
                    (?:
                        \.
                        $versionNumber
                        (?:
                            \.
                            $versionNumber
                        )?
                    )?
                )
                $whitespace
            )
            """.stripWhitespace()

        // "[1.5]"
        private val bracketedLoneVersion = """
            (?:
                $whitespace
                    $openBracket
                        $fullVersion
                    $closeBracket
                $whitespace
            )
            """.stripWhitespace()

        private val twoElementRange = """
                (?:
                    $whitespace
                        $openBracket
                            $whitespace
                                $fullVersion?
                                $comma
                                $fullVersion?
                            $whitespace
                        $closeBracket
                    $whitespace
                )
            """.stripWhitespace()
        private val singleRange = """
            (?:
                $twoElementRange
                |
                $bracketedLoneVersion
            )
            """.stripWhitespace()

        private val ranges = """
            ^
                (?:
                    $singleRange
                    (?:
                        $comma
                        $singleRange
                    )*
                )
            $dollar
            """.stripWhitespace()
        private val loneVersion = """

                (?:
                    $fullVersion
                )
            """.stripWhitespace()

        private val regex = """
            ^
                (?:
                    $ranges
                    |
                    $loneVersion
                )
            $dollar
            """.stripWhitespace().toRegex()

        fun validate(requirement: String) {
            if (!isValid(requirement)) {
                throw InvalidMavenVersionRequirementFormatException(requirement)
            }

        }

        fun String.stripWhitespace(): String {
            return this
                    .replace(" ", "")
                    .replace("\n", "")
                    .replace("\t", "")
        }

        fun isValid(requirement: String) = regex.matches(requirement)
    }
    private val validVersions: Array<VersionRange>

    /**
     * Parses [rawRequirement] into an instance of [MavenVersionRequirement]
     */
    constructor(requirement: String) : super(requirement, MAVEN) {
        validate(requirement)
        this.validVersions = ParsedRanges(this.rawRequirement).toVersionRanges()
    }

    override fun isSatisfiedBy(version: Version) = validVersions.any { it.contains(version) }

    //It's fun sized!
    fun size() = validVersions.size

    operator fun get(index: Int) = validVersions.get(index)
}

/**
 * @suppress
 */
class InvalidMavenVersionRequirementFormatException(format: String): InvalidRequirementFormatException("Invalid format for MavenVersionRequirement: $format")
