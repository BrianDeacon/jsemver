grammar Ruby;

clauses: clause (COMMA clause)* EOF ;
clause: WS* (singleQuote unquotedClause singleQuote) WS* | WS* unquotedClause WS* ;
singleQuote: SINGLE_QUOTE ;
unquotedClause: operator? version ;
operator: OPERATOR ;
version: major (DOT minor (DOT patch)?)?  ;
major: VERSION_ELEMENT ;
minor: VERSION_ELEMENT ;
patch: VERSION_ELEMENT ;

VERSION_ELEMENT: ZERO | NON_ZERO (NUMBER)*  ;
NON_ZERO: '1'..'9' ;
ZERO: '0' ;
NUMBER: ZERO | NON_ZERO ;

OPERATOR: GTEQ | LTEQ | LT | GT | EQ ;
fragment LTEQ: LT EQ ;
fragment GTEQ: GT EQ ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment EQ: '=' ;

DOT: '.' ;
COMMA: ',' ;
SINGLE_QUOTE: '\'';

WS                 : [\t ]+;
