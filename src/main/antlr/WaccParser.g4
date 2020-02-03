parser grammar WaccParser;

options {
    tokenVocab=WaccLexer;
}

program: BEGIN func* stat END EOF ;

func: type IDENT OPEN_PAREN paramList? CLOSE_PAREN IS stat END ;

paramList: param (COMMA param)* ;

param: type IDENT ;

stat: SKIPKW
    | type IDENT EQUALS assignRhs
    | assignLhs EQUALS assignRhs
    | READ assignLhs
    | FREE expr
    | RETURN expr
    | EXIT expr
    | PRINT expr
    | PRINTLN expr
    | IF expr THEN stat ELSE stat FI
    | WHILE expr DO stat DONE
    | BEGIN stat END
    | stat SEMICOLON stat
    ;

assignLhs: IDENT
         | arrayElem
         | pairElem
         ;

assignRhs: expr
         | arrayLiter
         | NEWPAIR OPEN_PAREN expr COMMA expr CLOSE_PAREN
         | pairElem
         | CALL IDENT OPEN_PAREN argList? CLOSE_PAREN
         ;

argList: expr (COMMA expr)* ;

pairElem: FST expr
        | SND expr
        ;

type: BASETYPE                                                     # BaseType
    | type OPEN_SQUARE_BR CLOSE_SQUARE_BR                          # ArrayType
    | PAIR OPEN_PAREN pairElemType COMMA pairElemType CLOSE_PAREN  # PairType
    ;

pairElemType: BASETYPE                            # BasePairElemType
            | type OPEN_SQUARE_BR CLOSE_SQUARE_BR # ArrayPairElemType
            | PAIR                                # PairPairElemType
            ;

expr: INTLITER
    | BOOLLITER
    | CHARLITER
    | STRLITER
    | PAIRLITER
    | IDENT
    | arrayElem
    | unaryOp expr
    | expr binaryOp expr
    | OPEN_PAREN expr CLOSE_PAREN
    ;

arrayElem: IDENT (OPEN_SQUARE_BR expr CLOSE_SQUARE_BR)+ ;

arrayLiter: OPEN_SQUARE_BR (expr (COMMA expr)*)? CLOSE_SQUARE_BR ;

unaryOp: BANG | MINUS | LEN | ORD | CHR ;

binaryOp: MUL | DIV | MOD | PLUS | MINUS | GT | GTE | LT | LTE | EQ | NEQ | LAND | LOR ;