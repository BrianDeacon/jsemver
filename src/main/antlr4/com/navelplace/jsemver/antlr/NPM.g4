grammar NPM;

union
  : intersection (OR intersection)*
  ;

intersection
  : (operatorClause | dashClause)+
  ;

dashClause
  : WS* version DASH version WS*
  ;

operatorClause:
  operator? WS* version
  ;

version
  : v? major (DOT minor (DOT patch (DASH preRelease (PLUS build)?)?)?)?
  ;

v: V ;

major: VERSION_ELEMENT | WILDCARD ;
minor: VERSION_ELEMENT | WILDCARD;
patch: VERSION_ELEMENT | WILDCARD ;
preRelease: dottedLegal;
build: dottedLegal;

dottedLegal: legalCharacters (DOT legalCharacters)* ;
legalCharacters: (WITHOUT_V | v | DASH | x | VERSION_ELEMENT)+ ;
x: X ;

operator
  : LT | GT | GTEQ | LTEQ | EQ | TILDE | CARET
  ;

VERSION_ELEMENT: '0' | [1-9][0-9]* ;

DOT: '.' ;
PLUS: '+' ;
DASH: '-' ;
CARET: '^' ;
ASTERISK: '*' ;

V: [vV] ;
//WITH_DASH: [0-9a-zA-Z-]+ ;
//WITHOUT_DASH: [0-9a-zA-Z]+;
WITHOUT_V: [a-uA-U0-9wy-zWY-Z]+;
LTEQ: LT EQ ;
GTEQ: GT EQ ;
LT: '<' ;
GT: '>' ;
EQ: '=' ;
PIPE: '|' ;
OR: PIPE PIPE ;
WILDCARD: X | ASTERISK ;
X: [xX] ;

TILDE: '~' ;
WS                 : [\t ]+ -> skip ;
