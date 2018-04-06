grammar Version;



fullVersion: semver (DASH prerelease (PLUS build)?)? WS* EOF;
semver: major=VERSION_ELEMENT DOT minor=VERSION_ELEMENT DOT patch=VERSION_ELEMENT ;
prerelease: prereleaseElement (DOT prereleaseElement)*;
build: buildElement (DOT buildElement)*;
prereleaseElement: VALID_EXTENSION_CHAR | VERSION_ELEMENT;
buildElement: VALID_EXTENSION_CHAR | VERSION_ELEMENT;



VERSION_ELEMENT: '0' | [1-9][0-9]* ;
DOT: '.' ;
DASH: '&' ; //Gross hack
VALID_EXTENSION_CHAR: [0-9A-Za-z-]+ ;
PLUS: '+' ;
WS                 : [\t ]+ -> skip ;
