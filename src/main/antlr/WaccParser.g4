parser grammar WaccParser;

options {
    tokenVocab=WaccLexer;
}

program: BEGIN func* stat END EOF ;

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

assignLhs: IDENT      # AssignLhsVariable
         | arrayElem  # AssignLhsArrayElem
         | pairElem   # AssignLhsPairElem
         ;

assignRhs: expr                                            # AssignRhsExpr
         | arrayLiter                                      # AssignRhsArrayLiter
         | NEWPAIR OPEN_PAREN expr COMMA expr CLOSE_PAREN  # AssignRhsNewpair
         | pairElem                                        # AssignRhsPairElem
         | CALL IDENT OPEN_PAREN argList? CLOSE_PAREN      # AssignRhsCall
         ;

argList: expr (COMMA expr)* ;

pairElem: acc=(FST | SND) expr ;

type: BASETYPE                                                     # BaseType
    | type OPEN_SQUARE_BR CLOSE_SQUARE_BR                          # ArrayType
    | PAIR OPEN_PAREN pairElemType COMMA pairElemType CLOSE_PAREN  # PairType
    ;

pairElemType: BASETYPE                            # BasePairElemType
            | type OPEN_SQUARE_BR CLOSE_SQUARE_BR # ArrayPairElemType
            | PAIR                                # PairPairElemType
            ;

expr: lit=INTLITER                                                                                 # Literal
    | lit=BOOLLITER                                                                                # Literal
    | lit=CHARLITER                                                                                # Literal
    | lit=STRLITER                                                                                 # Literal
    | lit=PAIRLITER                                                                                # Literal
    | IDENT                                                                                        # IdExpr
    | arrayElem                                                                                    # ArrayElemExpr
    | op=(BANG | MINUS | LEN | ORD | CHR) expr                                                     # UnaryOpExpr
    | expr op=(MUL | DIV | MOD | PLUS | MINUS | GT | GTE | LT | LTE | EQ | NEQ | LAND | LOR) expr  # BinaryOpExpr
    | OPEN_PAREN expr CLOSE_PAREN                                                                  # ParensExpr
    ;

arrayElem: IDENT (OPEN_SQUARE_BR expr CLOSE_SQUARE_BR)+ ;

arrayLiter: OPEN_SQUARE_BR (expr (COMMA expr)*)? CLOSE_SQUARE_BR ;
