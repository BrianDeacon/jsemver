grammar NPM;

union
  : intersection (OR intersection)*
  ;

intersection
  : (operatorClause | dashClause)+
  ;

dashClause
  : version ARROW version
  ;

operatorClause:
  operator? version
  ;

version
  : major (DOT minor (DOT patch (DASH preRelease (PLUS build)?)?)?)?
  ;

major: VERSION_ELEMENT ;
minor: VERSION_ELEMENT | wildcard;
patch: VERSION_ELEMENT | wildcard ;
preRelease: dottedLegal;
build: dottedLegal;

dottedLegal: legalCharacters (DOT legalCharacters)* ;
legalCharacters: (LEGAL_CHARACTERS | VERSION_ELEMENT)+ ;
operator: OPERATOR ;
wildcard: WILDCARD ;

VERSION_ELEMENT: '0' | ('1'..'9')('0'..'9')* ;
WILDCARD: '*' ;
LEGAL_CHARACTERS: [a-zA-Z0-9-]+ ;
DASH: '\uFF0D' ;
ARROW: '\u2194';

OPERATOR: LT | GT | GTEQ | LTEQ | EQ | TILDE | CARET ;
fragment LTEQ: LT EQ ;
fragment GTEQ: GT EQ ;
fragment LT: '<' ;
fragment GT: '>' ;
fragment EQ: '=' ;
fragment TILDE: '~' ;
fragment CARET: '^' ;

DOT: '.' ;
PLUS: '+' ;

fragment PIPE: '|' ;
OR: PIPE PIPE ;


WS                 : [\t ]+ -> skip ;
