package com.navelplace.jsemver.regex

fun String.stripWhitespace(): String {
    return this
            .replace(" ", "")
            .replace("\n", "")
            .replace("\t", "")
}


/**
 * @suppress
 */
object RegexConstants {

    val whitespace = """\s*"""
    val comma = """[\,]"""
    val dollar = "$"
    val caret = "^"
    val dash = """(?:[-])"""

    val versionNumber = """
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

    val fullVersion = """
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
}
