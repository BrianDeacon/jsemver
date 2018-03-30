package com.navelplace.jsemver

class MavenVersionRequirement(rawRequirement: String) : VersionRequirement(rawRequirement, RequirementType.MAVEN) {
    companion object {

        // Any comma preceded by a ] or ) is the delimiter for:
        // [1.1,2.2],[3.3,4.4),(5.5,6.6.6],(7.7,8.8)
        val VERSION_GROUPS_REGEX = """
        (?<=[\]\)])\s*\,
        """.trim().toRegex()

        private val open = """
            [\[\(]
            """.trim()
        private val close = """
            [\]\)]
            """.trim()
        // 0 or 1 or 10 but not 01
        private val versionNumber = """
            (?:0|[1-9]\d*)
            """.trim()
        // x.y.z
        private val version = """
            $versionNumber\.$versionNumber(?:\.$versionNumber)?
            """.trim()

        /*
           1.1,2.2
           3.3.3,4.4
           5.5,6.6.6
           ,8.8
           9.9,
         */
        private val versions = """
            ($version)?\s*\,\s*($version)?
            """.trim()

        // [1.1,2.2],[3.3,4.4),(5.5,6.6],(7.7,8.8)
        val VERSION_REQUIREMENT_REGEX = """
        ($open)\s*($versions)\s*($close)
        """.trim().toRegex()

        val SINGLE_VERSION_REQUIREMENT_REGEX = """
            $open\s*($version)\s*$close
            """.trim().toRegex()

        // [1.0,2.0] -> [1.0.0,2.0.0]
        private fun normalize(value: String): String {
            return when {
                value.isBlank() -> ""
                value.split(".").size != 3 -> "$value.0"
                else -> value
            }

        }
    }

    override fun calculate(rawRequirement: String): Array<VersionRange> {
        val elements: List<String> = rawRequirement.trim().split(VERSION_GROUPS_REGEX)
        return elements.map {
            when {
            //"1.5"
                !it.startsWith("[") && !it.startsWith("(") -> {
                    VersionRange(min=Version(normalize(it)), max = Version.MAX_VERSION)
                }

            //"[1.5]"
                SINGLE_VERSION_REQUIREMENT_REGEX.find(it) != null -> {
                    val version = normalize(SINGLE_VERSION_REQUIREMENT_REGEX.find(it)?.groups?.get(1)?.value?: "")
                    VersionRange(min = Version(version), max = Version(version))
                }

            //"[1.5,1.6]"
                else -> {
                    val groups = VERSION_REQUIREMENT_REGEX.find(it.trim())?.groups ?: throw InvalidMavenVersionRequirementFormatException(it)
                    val minInclusive = "[" == groups[1]?.value
                    val maxInclusive = "]" == groups[5]?.value
                    var minString = normalize(groups[3]?.value?: "")
                    var maxString = normalize(groups[4]?.value?: "")
                    val min = if (minString.isNotBlank()) Version(minString) else (Version.MIN_VERSION)
                    val max = if (maxString.isNotBlank()) Version(maxString) else (Version.MAX_VERSION)
                    VersionRange(min = min, minInclusive = minInclusive, max = max, maxInclusive = maxInclusive)
                }
            }
        }.toTypedArray()
    }
}

class InvalidMavenVersionRequirementFormatException(format: String): InvalidRequirementFormatException("Invalid format for MavenVersionRequirement: $format")