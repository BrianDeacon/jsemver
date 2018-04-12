grammar Maven;

ranges: (range (COMMA range)*) EOF ;
range: open? version? COMMA? version? close? ;

open: LEFT_BRACKET | LEFT_PARENS ;
close: RIGHT_BRACKET | RIGHT_PARENS ;
version: major (DOT minor (DOT patch)?)?  ;

major: VERSION_ELEMENT ;
minor: VERSION_ELEMENT ;
patch: VERSION_ELEMENT ;

VERSION_ELEMENT: ZERO | NON_ZERO (NUMBER)*  ;
NON_ZERO: '1'..'9' ;
ZERO: '0' ;
NUMBER: ZERO | NON_ZERO ;



LEFT_BRACKET: '[' ;
RIGHT_BRACKET: ']' ;
LEFT_PARENS: '(' ;
RIGHT_PARENS: ')' ;

DOT: '.' ;
COMMA: ',' ;

WS                 : [\t ]+ -> skip ;

