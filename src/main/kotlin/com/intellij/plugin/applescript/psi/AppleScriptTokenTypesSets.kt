package com.intellij.plugin.applescript.psi

import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ABOVE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AGAINST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ALIAS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ANY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.APART_FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AROUND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ASIDE_FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.AT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BACK
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BEFORE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BEGINNING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BEHIND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BELOW
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BENEATH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BESIDE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BETWEEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BOOLEAN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUILT_IN_TYPE_S
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.BY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CHARACTER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CLASS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COMMENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CONSIDERING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CONSTANT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CONTINUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.COPY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CUBIC_VOL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT_APP
import com.intellij.plugin.applescript.psi.AppleScriptTypes.CURRENT_APPLICATION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DATA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DATE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DEC_EXPONENT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIGITS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DIV
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOES
import com.intellij.plugin.applescript.psi.AppleScriptTypes.DOES_NOT_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EIGHTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ELSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.END
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ENDS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EQ
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ERROR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EVERY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.EXIT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FALSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FIFTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FILE_SPECIFICATION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FIRST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FOURTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FROM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.FRONT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GIVEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GLOBAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.GT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_INTERLEAVED_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_LABELED_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.HANDLER_POSITIONAL_PARAMETERS_DEFINITION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IGNORING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INSTEAD_OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTEGER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTERNATIONAL_TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INT_DIV
import com.intellij.plugin.applescript.psi.AppleScriptTypes.INTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_CONTAIN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IS_NOT_IN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.IT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ITEM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LAST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LIST
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LNOT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LOCAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LOCATION_SPECIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.LT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MIDDLE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MINUS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MOD
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MISSING_VALUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.MY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NINTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NLS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.NUMBER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ON
import com.intellij.plugin.applescript.psi.AppleScriptTypes.ONTO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OUT_OF
import com.intellij.plugin.applescript.psi.AppleScriptTypes.OVER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PARAGRAPH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PLUS
import com.intellij.plugin.applescript.psi.AppleScriptTypes.POINT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.POW
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PROP
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PROPERTY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.PUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RCURLY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.REAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RECORD
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RECTANGLE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.REFERENCE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.REPEAT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RETURN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RETURNING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.RGB_COLOR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SCRIPT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SECOND
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SET
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SEVENTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SINCE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SIXTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SOME
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SPECIFIER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.SQUARE_AREA
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STAR
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STARTS_BEGINS_WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STRING_LITERAL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STYLED_CLIPBOARD_TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.STYLED_TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TELL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TEMPERATURE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TENTH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TEXT_ITEM
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THAT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THE_KW
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THEN
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THIRD
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THROUGH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.THRU
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TIMEOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TIMES
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TO
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TRANSACTION
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TRUE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TRY
import com.intellij.plugin.applescript.psi.AppleScriptTypes.TYPE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNDER
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNICODE_TEXT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.UNTIL
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHERE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHILE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WHOSE
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITH
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WITHOUT
import com.intellij.plugin.applescript.psi.AppleScriptTypes.WORD
import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet

object AppleScriptTokenTypesSets {

    @JvmField
    val OPERATORS: TokenSet = TokenSet.create(
        PLUS, MINUS, MOD, POW, BAND, LNOT, DIV, INT_DIV, STAR, INT_DIV, EQ, NE, IS_IN,
        IS_CONTAIN, IS_NOT_IN, LE, GE, GT, LT, LCURLY, RCURLY, AS,
    )

    @JvmField
    val NUMBERS: TokenSet = TokenSet.create(DIGITS, DEC_EXPONENT)

    @JvmField
    val LOGICAL_OPERATORS: TokenSet = TokenSet.create(LAND, LOR, LNOT)

    @JvmField
    val COMPARISON_OPERATORS: TokenSet = TokenSet.create(
        EQ, NE, LE, GE, GT, LT, IS_IN, IS_NOT_IN, IS_CONTAIN, DOES_NOT_CONTAIN,
        STARTS_BEGINS_WITH, ENDS_WITH,
    )

    @JvmField
    val LANGUAGE_LITERALS: TokenSet = TokenSet.create(
        TRUE, FALSE, MISSING_VALUE, CURRENT, CURRENT_APP, CURRENT_APPLICATION,
    )

    @JvmField
    val BUILT_IN_TYPES: TokenSet = TokenSet.create(
        ALIAS, ANY, BOOLEAN, BUILT_IN_TYPE_S, CHARACTER, CLASS, CONSTANT, CUBIC_VOL,
        DATA, DATE, FILE, FILE_SPECIFICATION, INTEGER, INTERNATIONAL_TEXT, ITEM, LIST,
        LOCATION_SPECIFIER, NUMBER, PARAGRAPH, POINT, REAL, RECORD, RECTANGLE, REFERENCE,
        RGB_COLOR, SPECIFIER, SQUARE_AREA, STRING, STYLED_CLIPBOARD_TEXT, STYLED_TEXT,
        TEMPERATURE, TEXT, TEXT_ITEM, TYPE, UNICODE_TEXT, WORD,
    )

    @JvmField
    val KEYWORDS: TokenSet = TokenSet.create(
        AROUND, AS, ASIDE_FROM, AT, BACK, BEFORE, BEGINNING, BEHIND, BELOW, BENEATH,
        BESIDE, BETWEEN, BUT, BY, CONSIDERING, CONTINUE, COPY, DIV, DOES, EIGHTH, ELSE,
        END, EQ, ERROR, EVERY, EXIT, FALSE, FIFTH, FIRST, FOR, FOURTH, FROM, FRONT,
        GET, GIVEN, GLOBAL, IF, IGNORING, IN, INSTEAD_OF, INTO, IT, LAST, LOCAL, ME,
        MIDDLE, MOD, MY, NINTH, LNOT, OF, ON, ONTO, LOR, OUT_OF, OVER, PROP, PROPERTY,
        PUT, REFERENCE, REPEAT, RETURN, RETURNING, SCRIPT, SECOND, SET, SEVENTH, SINCE,
        SIXTH, SOME, TELL, TENTH, THAT, THE_KW, THEN, THIRD, THROUGH, THRU, TIMEOUT,
        TIMES, TO, TRANSACTION, TRUE, TRY, UNTIL, WHERE, WHILE, WHOSE, WITH, WITHOUT,
    )

    @JvmField
    val HANDLER_PARAMETER_LABELS: TokenSet = TokenSet.create(
        ABOUT, ABOVE, AGAINST, APART_FROM, AROUND, ASIDE_FROM, AT, BELOW, BENEATH,
        BESIDE, BETWEEN, BY, FOR, FROM, INSTEAD_OF, INTO, ON, ONTO, OUT_OF, OVER,
        SINCE, THRU, THROUGH, UNDER, TO,
    )

    @JvmField
    val HANDLER_DEFINITIONS: TokenSet = TokenSet.create(
        HANDLER_POSITIONAL_PARAMETERS_DEFINITION,
        HANDLER_INTERLEAVED_PARAMETERS_DEFINITION,
        HANDLER_LABELED_PARAMETERS_DEFINITION,
    )

    @JvmField
    val STRINGS: TokenSet = TokenSet.create(STRING_LITERAL)

    @JvmField
    val WHITE_SPACES_SET: TokenSet = TokenSet.create(NLS, TokenType.WHITE_SPACE)

    @JvmField
    val COMMENT_OR_WHITE_SPACE: TokenSet = TokenSet.create(NLS, TokenType.WHITE_SPACE, COMMENT)

}
