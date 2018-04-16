lexer grammar Common;

VERSION_ELEMENT: ZERO | NON_ZERO (NUMBER)*  ;
NON_ZERO: '1'..'9' ;
ZERO: '0' ;
NUMBER: ZERO | NON_ZERO ;

fragment SIMPLE_OPERATOR: GTEQ | LTEQ | LT | GT | EQ ;
fragment LTEQ: LT EQ ;
fragment GTEQ: GT EQ ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment EQ: '=' ;
fragment TILDE: '~' ;
fragment CARET: '^' ;

LEFT_BRACKET: '[' ;
RIGHT_BRACKET: ']' ;
LEFT_PARENS: '(' ;
RIGHT_PARENS: ')' ;

DOT: '.' ;
COMMA: ',' ;
