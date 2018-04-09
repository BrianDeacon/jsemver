package com.navelplace.jsemver.npm

import com.navelplace.jsemver.InvalidRequirementFormatException
import com.navelplace.jsemver.InvalidVersionFormatException
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

/**
 * @suppress
 */
class ThrowingRequirementErrorListener(val input: String) : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        throw InvalidRequirementFormatException(input)
    }
}

/**
 * @suppress
 */
class ThrowingErrorListener(val input: String) : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        throw InvalidVersionFormatException(input)
    }
}
