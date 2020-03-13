lexer grammar WaccLexer;

// Types and literals
INTLITER: DIGIT+ ;
BOOLLITER: TRUE | FALSE ;
CHARLITER: '\'' CHARACTER '\'' ;
STRLITER: '"' CHARACTER* '"' ;
PAIRLITER: NULL ;

// Keywords
BEGIN: 'begin' ;
END: 'end' ;
IS: 'is' ;
SKIPKW: 'skip' ;  // KW=Keyword, SKIP is reserved in ANTLR4
READ: 'read' ;
FREE: 'free' ;
RETURN: 'return' ;
EXIT: 'exit' ;
PRINT: 'print' ;
PRINTLN: 'println' ;
IF: 'if' ;
THEN: 'then' ;
ELSE: 'else' ;
FI: 'fi' ;
WHILE: 'while' ;
DO: 'do' ;
DONE: 'done' ;
NEWPAIR: 'newpair' ;
CALL: 'call' ;
FST: 'fst' ;
SND: 'snd' ;
PAIR: 'pair' ;
TRUE: 'true' ;
FALSE: 'false' ;
INT: 'int' ;
BOOL: 'bool' ;
CHAR: 'char' ;
STRING: 'string' ;
NULL: 'null' ;
CLASS: 'class' ;
NEWKW: 'new' ;
INCLUDE: 'include' ;

// Parens and Brackets
OPEN_PAREN: '(' ;
CLOSE_PAREN: ')' ;
OPEN_SQUARE_BR: '[' ;
CLOSE_SQUARE_BR: ']' ;

// Symbols
COMMA: ',' ;
EQUALS: '=' ;
SEMICOLON: ';' ;
DOT: '.' ;

// Operators
BANG: '!' ;
PLUS: '+' ;
MINUS: '-' ;
LEN: 'len' ;
ORD: 'ord' ;
CHR: 'chr' ;
MUL: '*' ;
DIV: '/' ;
MOD: '%' ;
GT: '>' ;
GTE: '>=' ;
LT: '<' ;
LTE: '<=' ;
EQ: '==' ;
NEQ: '!=' ;
LAND: '&&' ;
LOR: '||' ;
BAND: '&' ;
BOR: '|' ;
BNOT: '~' ;
BXOR: '^' ;
BLEFT: '<<' ;
BRIGHT: '>>' ;

// Identifiers
fragment DIGIT: [0-9] ;
IDENT: [_a-zA-Z] [_a-zA-Z0-9]* ;
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPEDCHARACTER ;
fragment ESCAPEDCHARACTER: '0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\' ;
FILENAME: [a-zA-Z0-9_]+.'waccl' ; // TODO: find a better file extension, 4 chars is too much

// Ignore
COMMENT: '#' ~[\r\n]* -> skip ;
WS: [ \r\n\t] -> skip ;
