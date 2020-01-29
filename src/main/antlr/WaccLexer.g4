lexer grammar WaccLexer;

// Types and literals
BASETYPE: INT | BOOL | CHAR | STRING ;
fragment INTSIGN: PLUS | MINUS ;
INTLITER: INTSIGN? DIGIT+ ;
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

// Parens and Brackets
OPEN_PAREN: '(' ;
CLOSE_PAREN: ')' ;
OPEN_SQUARE_BR: '[' ;
CLOSE_SQUARE_BR: ']' ;

// Symbols
COMMA: ',' ;
EQUALS: '=' ;
SEMICOLON: ';' ;

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

// Identifiers
fragment DIGIT: [0-9] ;
IDENT: [_a-zA-Z] [_a-zA-Z0-9]* ;
fragment CHARACTER: ~('\\' | '\'' | '"') | '\\' ESCAPEDCHARACTER ;
fragment ESCAPEDCHARACTER: '0' | 'b' | 't' | 'n' | 'f' | 'r' | '"' | '\'' | '\\' ;

// Ignore
COMMENT: '#' ~[\r\n]* -> skip ;
WS: [ \r\n\t] -> skip ;
