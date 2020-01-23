grammar Wacc;
import Lexer;

program : 'begin' func* stat 'end' ;

func : type ID '(' paramList? ')' ;

paramList : param (',' param)* ;

param : type ID ;

stat : 'skip'
     | type ID '=' assignRhs
     | assignLhs '=' assignRhs
     | 'read' assignRhs
     | 'free' expr
     | 'return' expr
     | 'exit' expr
     | 'print' expr
     | 'println' expr
     | 'if' expr 'then' stat 'else' stat 'fi'
     | 'while' expr 'do' stat 'done'
     | 'begin' stat 'end'
     | stat ';' stat
     ;

assignLhs : ID
          | arrayElem
          | pairElem
          ;

assignRhs : expr
          | ARRAYLIT
          | 'newpair' '(' expr ',' expr ')'
          | pairElem
          | 'call' ID '(' argList? ')'
          ;

argList : expr (',' expr)* ;

type : baseType
     | type '[' ']'
     | pairType
     ;

baseType : 'int'
         | 'bool'
         | 'char'
         | 'string'
         ;

arrayType : type '[' ']' ;

pairType : 'pair' '(' pairElemType ',' pairElemType ')' ;

pairElemType : baseType
             | arrayType
             | 'pair'
             ;

expr : INTLIT
     | BOOLLIT
     | CHARLIT
     | ARRAYLIT
     | PAIRLIT
     | ID
     | ARRAYELEM
     | UNOP expr
     | expr BINOP expr
     | '(' expr ')'
     ;

arrayLit: '[' (expr (',' expr)*)? ']' ;
arrayElem: ID ('[' expr ']')+ ;
pairElem: 'fst' expr
        | 'snd' expr
        ;

INTLIT : INTSIGN? DIGIT+ ;
BOOLLIT: TRUE | FALSE ;
CHARLIT: SQUOTE CHARACTER SQUOTE ;
PAIRLIT: NULL ;