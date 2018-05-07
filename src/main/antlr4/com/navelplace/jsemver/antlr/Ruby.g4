grammar Ruby;

clauses: clause (COMMA clause)* EOF ;
clause: WS* (singleQuote unquotedClause singleQuote) WS* | WS* unquotedClause WS* ;
singleQuote: SINGLE_QUOTE ;
unquotedClause: operator? WS* version ;
operator: OPERATOR ;
version: major (DOT minor (DOT patch)?)?  ;
major: VERSION_ELEMENT ;
minor: VERSION_ELEMENT ;
patch: VERSION_ELEMENT ;

VERSION_ELEMENT: ZERO | NON_ZERO (NUMBER)*  ;
NON_ZERO: '1'..'9' ;
ZERO: '0' ;
NUMBER: ZERO | NON_ZERO ;

OPERATOR: SIMPLE_OPERATOR | TWIDDLE_WAKA ;
fragment TWIDDLE_WAKA: TILDE GT ;
fragment SIMPLE_OPERATOR: GTEQ | LTEQ | LT | GT | EQ ;
fragment LTEQ: LT EQ ;
fragment GTEQ: GT EQ ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment EQ: '=' ;
fragment TILDE: '~' ;

DOT: '.' ;
COMMA: ',' ;
SINGLE_QUOTE: '\'';

WS                 : [\t ]+;
