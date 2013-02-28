
/*
* generated by Xtext
*/
lexer grammar InternalDASMLexer;


@header {
package devcpu.ui.contentassist.antlr.lexer;

// Hack: Use our own Lexer superclass by means of import. 
// Currently there is no other way to specify the superclass for the lexer.
import org.eclipse.xtext.ui.editor.contentassist.antlr.internal.Lexer;
}




KEYWORD_87 : '#'('I'|'i')('N'|'n')('C'|'c')('L'|'l')('U'|'u')('D'|'d')('E'|'e');

KEYWORD_88 : '.'('I'|'i')('N'|'n')('C'|'c')('L'|'l')('U'|'u')('D'|'d')('E'|'e');

KEYWORD_83 : '#'('I'|'i')('M'|'m')('P'|'p')('O'|'o')('R'|'r')('T'|'t');

KEYWORD_84 : '#'('O'|'o')('R'|'r')('I'|'i')('G'|'g')('I'|'i')('N'|'n');

KEYWORD_85 : '.'('I'|'i')('M'|'m')('P'|'p')('O'|'o')('R'|'r')('T'|'t');

KEYWORD_86 : '.'('O'|'o')('R'|'r')('I'|'i')('G'|'g')('I'|'i')('N'|'n');

KEYWORD_81 : '#'('A'|'a')('L'|'l')('I'|'i')('G'|'g')('N'|'n');

KEYWORD_82 : '.'('A'|'a')('L'|'l')('I'|'i')('G'|'g')('N'|'n');

KEYWORD_78 : '.'('D'|'d')('A'|'a')('T'|'t');

KEYWORD_79 : ('P'|'p')('I'|'i')('C'|'c')('K'|'k');

KEYWORD_80 : ('P'|'p')('U'|'u')('S'|'s')('H'|'h');

KEYWORD_39 : '>''>''>';

KEYWORD_40 : ('A'|'a')('D'|'d')('D'|'d');

KEYWORD_41 : ('A'|'a')('D'|'d')('X'|'x');

KEYWORD_42 : ('A'|'a')('N'|'n')('D'|'d');

KEYWORD_43 : ('A'|'a')('S'|'s')('R'|'r');

KEYWORD_44 : ('B'|'b')('O'|'o')('R'|'r');

KEYWORD_45 : ('D'|'d')('A'|'a')('T'|'t');

KEYWORD_46 : ('D'|'d')('I'|'i')('V'|'v');

KEYWORD_47 : ('D'|'d')('V'|'v')('I'|'i');

KEYWORD_48 : ('H'|'h')('W'|'w')('I'|'i');

KEYWORD_49 : ('H'|'h')('W'|'w')('N'|'n');

KEYWORD_50 : ('H'|'h')('W'|'w')('Q'|'q');

KEYWORD_51 : ('I'|'i')('A'|'a')('G'|'g');

KEYWORD_52 : ('I'|'i')('A'|'a')('Q'|'q');

KEYWORD_53 : ('I'|'i')('A'|'a')('S'|'s');

KEYWORD_54 : ('I'|'i')('F'|'f')('A'|'a');

KEYWORD_55 : ('I'|'i')('F'|'f')('B'|'b');

KEYWORD_56 : ('I'|'i')('F'|'f')('C'|'c');

KEYWORD_57 : ('I'|'i')('F'|'f')('E'|'e');

KEYWORD_58 : ('I'|'i')('F'|'f')('G'|'g');

KEYWORD_59 : ('I'|'i')('F'|'f')('L'|'l');

KEYWORD_60 : ('I'|'i')('F'|'f')('N'|'n');

KEYWORD_61 : ('I'|'i')('F'|'f')('U'|'u');

KEYWORD_62 : ('I'|'i')('N'|'n')('T'|'t');

KEYWORD_63 : ('J'|'j')('S'|'s')('R'|'r');

KEYWORD_64 : ('M'|'m')('D'|'d')('I'|'i');

KEYWORD_65 : ('M'|'m')('L'|'l')('I'|'i');

KEYWORD_66 : ('M'|'m')('O'|'o')('D'|'d');

KEYWORD_67 : ('M'|'m')('U'|'u')('L'|'l');

KEYWORD_68 : ('P'|'p')('O'|'o')('P'|'p');

KEYWORD_69 : ('R'|'r')('F'|'f')('I'|'i');

KEYWORD_70 : ('S'|'s')('B'|'b')('X'|'x');

KEYWORD_71 : ('S'|'s')('E'|'e')('T'|'t');

KEYWORD_72 : ('S'|'s')('H'|'h')('L'|'l');

KEYWORD_73 : ('S'|'s')('H'|'h')('R'|'r');

KEYWORD_74 : ('S'|'s')('T'|'t')('D'|'d');

KEYWORD_75 : ('S'|'s')('T'|'t')('I'|'i');

KEYWORD_76 : ('S'|'s')('U'|'u')('B'|'b');

KEYWORD_77 : ('X'|'x')('O'|'o')('R'|'r');

KEYWORD_28 : '!''=';

KEYWORD_29 : '&''&';

KEYWORD_30 : '<''<';

KEYWORD_31 : '<''=';

KEYWORD_32 : '=''=';

KEYWORD_33 : '>''=';

KEYWORD_34 : '>''>';

KEYWORD_35 : ('E'|'e')('X'|'x');

KEYWORD_36 : ('P'|'p')('C'|'c');

KEYWORD_37 : ('S'|'s')('P'|'p');

KEYWORD_38 : '|''|';

KEYWORD_1 : '!';

KEYWORD_2 : '%';

KEYWORD_3 : '&';

KEYWORD_4 : '(';

KEYWORD_5 : ')';

KEYWORD_6 : '*';

KEYWORD_7 : '+';

KEYWORD_8 : ',';

KEYWORD_9 : '-';

KEYWORD_10 : '/';

KEYWORD_11 : ':';

KEYWORD_12 : '<';

KEYWORD_13 : '>';

KEYWORD_14 : '?';

KEYWORD_15 : ('A'|'a');

KEYWORD_16 : ('B'|'b');

KEYWORD_17 : ('C'|'c');

KEYWORD_18 : ('I'|'i');

KEYWORD_19 : ('J'|'j');

KEYWORD_20 : ('X'|'x');

KEYWORD_21 : ('Y'|'y');

KEYWORD_22 : ('Z'|'z');

KEYWORD_23 : '[';

KEYWORD_24 : ']';

KEYWORD_25 : '^';

KEYWORD_26 : '|';

KEYWORD_27 : '~';



RULE_ID : '^'? ('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*;

RULE_HEXNUMBER : '0x' ('0'..'9'|'a'..'f'|'A'..'F')+;

RULE_DECNUMBER : '-'? ('0'..'9')+;

RULE_BINNUMBER : '0b' ('0'|'1')+;

RULE_SL_COMMENT : ';' ~(('\n'|'\r'))*;

RULE_STRING : ('"' ('\\' ('b'|'t'|'n'|'f'|'r'|'u'|'"'|'\''|'\\')|~(('\\'|'"')))* '"'|'\'' ('\\' ('b'|'t'|'n'|'f'|'r'|'u'|'"'|'\''|'\\')|~(('\\'|'\'')))* '\'');

RULE_WS : (' '|'\t')+;

RULE_NL : (' '|'\t')* '\r'? '\n';



