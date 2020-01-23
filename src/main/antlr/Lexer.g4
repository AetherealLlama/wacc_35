lexer grammar Lexer;

BANG : '!' ;
MINUS : '-' ;
LEN: 'len' ;
ORD: 'ord' ;
CHR: 'chr' ;
MUL: '*' ;
DIV: '/' ;
MOD: '%' ;
PLUS: '+' ;
GT: '>' ;
GTE: '>=' ;
LT: '<' ;
LTE: '<=' ;
EQ: '==' ;
NEQ: '!=' ;
LAND: '&&' ;
LOR: '||' ;

SQUOTE: '\'' ;
DQUOTE: '"' ;

NULL: 'null' ;

TRUE: 'true' ;
FALSE: 'false' ;

UNOP: BANG
    | MINUS
    | LEN
    | ORD
    | CHR
    ;

BINOP: MUL
     | DIV
     | MOD
     | PLUS
     | MINUS
     | GT
     | GTE
     | LT
     | LTE
     | EQ
     | NEQ
     | LAND
     | LOR
     ;
INTSIGN : PLUS
        | MINUS
        ;

fragment DIGIT: [0-9] ;
fragment CHARACTER: ~('\\' | '\'' | '"')
                  | '\\' ESCAPEDCHARS
                  ;
fragment ESCAPEDCHARS: '0'
                     | 'b'
                     | 't'
                     | 'n'
                     | 'f'
                     | 'r'
                     | '"'
                     | '\''
                     | '\\'
                     ;


IDENT : [_a-zA-Z] [_a-zA-Z0-9]* ;
NEWLINE : '\r'? '\n' -> skip ;
WS : [ \t] -> skip;
COMMENT : '#' ~[\r?\n]* -> skip;
