parser grammar WaccParser;

options {
    tokenVocab=WaccLexer;
}

program: BEGIN include* cls* func* stat END EOF ;

library: BEGIN include* cls* func* END EOF ;

include: INCLUDE FILENAME SEMICOLON ;

cls: CLASS IDENT IS (param SEMICOLON)* func*  END ;

func: type IDENT OPEN_PAREN paramList? CLOSE_PAREN IS stat END ;

paramList: param (COMMA param)* ;

param: type IDENT ;

stat: SKIPKW                          # Skip
    | type IDENT EQUALS assignRhs     # AssignNew
    | assignLhs EQUALS assignRhs      # Assign
    | READ assignLhs                  # Read
    | FREE expr                       # Free
    | RETURN expr                     # Return
    | EXIT expr                       # Exit
    | PRINT expr                      # Print
    | PRINTLN expr                    # Println
    | IF expr THEN stat ELSE stat FI  # IfThenElse
    | WHILE expr DO stat DONE         # WhileDo
    | BEGIN stat END                  # Begin
    | stat SEMICOLON stat             # Compose
    ;

assignLhs: (expr DOT)? IDENT      # AssignLhsVariable
         | arrayElem              # AssignLhsArrayElem
         | pairElem               # AssignLhsPairElem
         ;

assignRhs: expr                                                        # AssignRhsExpr
         | arrayLiter                                                  # AssignRhsArrayLiter
         | NEWPAIR OPEN_PAREN expr COMMA expr CLOSE_PAREN              # AssignRhsNewpair
         | pairElem                                                    # AssignRhsPairElem
         | CALL (expr DOT)? IDENT OPEN_PAREN argList? CLOSE_PAREN      # AssignRhsCall
         ;

argList: expr (COMMA expr)* ;

pairElem: acc=(FST | SND) expr ;

type: bt=(INT | BOOL | CHAR | STRING)                              # BaseType
    | type OPEN_SQUARE_BR CLOSE_SQUARE_BR                          # ArrayType
    | PAIR OPEN_PAREN pairElemType COMMA pairElemType CLOSE_PAREN  # PairType
    | IDENT                                                        # ClassType
    ;

pairElemType: bt=(INT | BOOL | CHAR | STRING)     # BasePairElemType
            | type OPEN_SQUARE_BR CLOSE_SQUARE_BR # ArrayPairElemType
            | PAIR                                # PairPairElemType
            | IDENT                               # ClassPairElemType
            ;

expr: integer                                             # Int
    | lit=(BOOLLITER | CHARLITER | STRLITER | PAIRLITER)  # Literal
    | IDENT                                               # IdExpr
    | arrayElem                                           # ArrayElemExpr
    | op=(BANG | MINUS | LEN | ORD | CHR | BNOT) expr     # UnaryOpExpr
    | expr op=(MUL | DIV | MOD) expr                      # BinaryOpExpr
    | expr op=(PLUS | MINUS) expr                         # BinaryOpExpr
    | expr op=(BLEFT | BRIGHT) expr                       # BinaryOpExpr
    | expr op=(GT | GTE | LT | LTE) expr                  # BinaryOpExpr
    | expr op=(EQ | NEQ) expr                             # BinaryOpExpr
    | expr op=BAND expr                                   # BinaryOpExpr
    | expr op=BXOR expr                                   # BinaryOpExpr
    | expr op=BOR expr                                    # BinaryOpExpr
    | expr op=(LAND | LOR) expr                           # BinaryOpExpr
    | expr DOT IDENT                                      # ClassFieldExpr
    | NEWKW IDENT                                         # InstantiateExpr
    | OPEN_PAREN expr CLOSE_PAREN                         # ParensExpr
    ;

integer: sign=(PLUS | MINUS)? INTLITER ;

arrayElem: IDENT (OPEN_SQUARE_BR expr CLOSE_SQUARE_BR)+ ;

arrayLiter: OPEN_SQUARE_BR (expr (COMMA expr)*)? CLOSE_SQUARE_BR ;
