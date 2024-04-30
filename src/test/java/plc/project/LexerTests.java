package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import plc.project.Lexer;
import plc.project.ParseException;
import plc.project.Token;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("@alpha", "@getname", true),
                Arguments.of("Multiple @", "@thelegend@", false),
                Arguments.of("hyphen", "theleg-jk", true),
                Arguments.of("underscore", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),

                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "12345", true),
                Arguments.of("Negative", "-1", true),
                Arguments.of("Negative", "-5", true),
                Arguments.of("Negative Decimal", "-1.0", false),
                Arguments.of("Negative", "-1893", true),
                Arguments.of("Zero", "0", true),
                Arguments.of("Negative Zero", "-0", false),
                Arguments.of("Negative", "-01", false),
                Arguments.of("Negative", "-", false),
                Arguments.of("Not a number", "hello", false),
                Arguments.of("Negative", "--1", false),
                Arguments.of("Leading Zero", "01", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Negative Decimal", "-11.0.834", false),
                Arguments.of("Negative Decimallong", "-1.0829032890", true),
                Arguments.of("Positive Decimallong", "1.0829032890", true),
                Arguments.of("Negative Decimal000", "-0.00000", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Multiple Decimal", "1.4.7", false),
                Arguments.of("Multiple Decimal", "1..7", false),
                Arguments.of("big negative", "-8934.893429", true),
                Arguments.of("Multiple Decimal2", "1..0", false),
                Arguments.of("Leading Decimal", ".5", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Alphabetic", "\'n\'", true),
                Arguments.of("Alphabetic", "\'9\'", true),
                Arguments.of("\\", "\'\\\\\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("extra'", "\'K\'\'", false),
                Arguments.of("extra\\", "\'K\\\\\'", false),
                Arguments.of("unterminated", "\'a", false),
                Arguments.of("no starting'", "b\'", false),
                Arguments.of("Multiple", "\'abc\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Fix 3", "\"sq\\'dq\\\"bs\\\\\"", true),
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Newline unterminated String", "\"unterminated\n\"", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "!=", true),
                Arguments.of("Comparison", "==", true),
                Arguments.of("equals", "=", true),
                Arguments.of("not", "!", true),
                Arguments.of("((", "((", false),
                Arguments.of("Space", " ", false),
                Arguments.of("-", "-", true),
                Arguments.of("Tab", "\t", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testEscape(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        testwhite(input, success);
    }

    private static Stream<Arguments> testEscape() {
        return Stream.of(
                Arguments.of("Space", " ", true),
                Arguments.of("Line terminator", "\n", true),
                Arguments.of("Tab", "\t", true),

                Arguments.of("\b", "\b", true),
                Arguments.of("b", "b", false),
                Arguments.of("t", "t", false),
                Arguments.of("n", "n", false),
                Arguments.of("\\", "\\", true),
                Arguments.of("f", "f", false)
        );
    }

    private static void testwhite(String input, boolean success) {
        try {
            Assertions.assertEquals(success, new Lexer(input).escape());

        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0)

                )),
                Arguments.of("Operator", "; ) == ;", Arrays.asList(
                        new Token(Token.Type.OPERATOR, ";", 0),
                        new Token(Token.Type.OPERATOR, ")", 2),
                        new Token(Token.Type.OPERATOR, "==", 4),
                        new Token(Token.Type.OPERATOR, ";", 7)

                )),
                Arguments.of("Operator -", "-", Arrays.asList(
                        new Token(Token.Type.OPERATOR, "-", 0)
                )),
                Arguments.of("Example 2", "LET it be done", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "it", 4),
                        new Token(Token.Type.IDENTIFIER, "be", 7),
                        new Token(Token.Type.IDENTIFIER, "done", 10)

                )),
                Arguments.of("Example 2", "LET x 5", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.INTEGER, "5", 6)

                )),
                Arguments.of("Example 3", "LET x 5 LET b -5", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.INTEGER, "5", 6),
                        new Token(Token.Type.IDENTIFIER, "LET", 8),
                        new Token(Token.Type.IDENTIFIER, "b", 12),
                        new Token(Token.Type.INTEGER, "-5", 14)

                )),
                Arguments.of("Example 4", "LET x = bebe;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.IDENTIFIER, "bebe", 8),
                        new Token(Token.Type.OPERATOR, ";", 12)

                )),
                Arguments.of("Example 5", "LET x = \'b\';", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.CHARACTER, "\'b\'", 8),
                        new Token(Token.Type.OPERATOR, ";", 11)

                )),
                Arguments.of("Example 6", "LET x   = \'b\';", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 8),
                        new Token(Token.Type.CHARACTER, "\'b\'", 10),
                        new Token(Token.Type.OPERATOR, ";", 13)

                )),
                Arguments.of("Fix 1", "2.0 - 3", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "2.0", 0),
                        new Token(Token.Type.OPERATOR, "-", 4),
                        new Token(Token.Type.INTEGER, "3", 6)
                )),
                Arguments.of("multipule decimals", "1.2.6", Arrays.asList(
                        new Token(Token.Type.DECIMAL, "1.2", 0),
                        new Token(Token.Type.OPERATOR, ".", 3),
                        new Token(Token.Type.INTEGER, "6", 4)
                )),
                Arguments.of("number method", "1.toString()", Arrays.asList(
                        new Token(Token.Type.INTEGER, "1", 0),
                        new Token(Token.Type.OPERATOR, ".", 1),
                        new Token(Token.Type.IDENTIFIER, "toString", 2),
                        new Token(Token.Type.OPERATOR, "(", 10),
                        new Token(Token.Type.OPERATOR, ")", 11)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Foo",
                        "VAR i = -1 : Integer;\n" +
                        "VAL inc = 2 : Integer;\n" +
                        "FUN foo() DO\n" +
                        "    WHILE i != 1 DO\n" +
                        "        IF i > 0 DO\n" +
                        "            print(\"bar\");\n"+
                        "        END\n" +
                        "        i = i + inc;\n" +
                        "    END\n" +
                        "END", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "VAR", 0),
                        new Token(Token.Type.IDENTIFIER, "i", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "-1", 8),
                        new Token(Token.Type.OPERATOR, ":", 11),
                        new Token(Token.Type.IDENTIFIER, "Integer", 13),
                        new Token(Token.Type.OPERATOR, ";", 20),
//VAL inc = 2 : Integer;
                        new Token(Token.Type.IDENTIFIER, "VAL", 22),
                        new Token(Token.Type.IDENTIFIER, "inc", 26),
                        new Token(Token.Type.OPERATOR, "=", 30),
                        new Token(Token.Type.INTEGER, "2", 32),
                        new Token(Token.Type.OPERATOR, ":", 34),
                        new Token(Token.Type.IDENTIFIER, "Integer", 36),
                        new Token(Token.Type.OPERATOR, ";", 43),
//DEF foo() DO
                        new Token(Token.Type.IDENTIFIER, "FUN", 45),
                        new Token(Token.Type.IDENTIFIER, "foo", 49),
                        new Token(Token.Type.OPERATOR, "(", 52),
                        new Token(Token.Type.OPERATOR, ")", 53),
                        new Token(Token.Type.IDENTIFIER, "DO", 55),
// WHILE i != 1 DO
                        new Token(Token.Type.IDENTIFIER, "WHILE", 62),
                        new Token(Token.Type.IDENTIFIER, "i", 68),
                        new Token(Token.Type.OPERATOR, "!=", 70),
                        new Token(Token.Type.INTEGER, "1", 73),
                        new Token(Token.Type.IDENTIFIER, "DO", 75),
// IF i > 0 DO
                        new Token(Token.Type.IDENTIFIER, "IF", 86),
                        new Token(Token.Type.IDENTIFIER, "i", 89),
                        new Token(Token.Type.OPERATOR, ">", 91),
                        new Token(Token.Type.INTEGER, "0", 93),
                        new Token(Token.Type.IDENTIFIER, "DO", 95),
// print(\"bar\");
                        new Token(Token.Type.IDENTIFIER, "print", 110),
                        new Token(Token.Type.OPERATOR, "(", 115),
                        new Token(Token.Type.STRING, "\"bar\"", 116),
                        new Token(Token.Type.OPERATOR, ")", 121),
                        new Token(Token.Type.OPERATOR, ";", 122),
// END
                        new Token(Token.Type.IDENTIFIER, "END", 132),
// i = i + inc;
                        new Token(Token.Type.IDENTIFIER, "i",144),
                        new Token(Token.Type.OPERATOR, "=", 146),
                        new Token(Token.Type.IDENTIFIER, "i", 148),
                        new Token(Token.Type.OPERATOR, "+", 150),
                        new Token(Token.Type.IDENTIFIER, "inc", 152),
                        new Token(Token.Type.OPERATOR, ";", 155),
// END
                        new Token(Token.Type.IDENTIFIER, "END", 161),
//END
                        new Token(Token.Type.IDENTIFIER, "END", 165)
                ))
                /*
                Arguments.of("Fizzbuzz", "LET i = 1;\n" +
                        "WHILE i != 100 DO\n" +
                        "    IF rem(i, 3) == 0 AND rem(i, 5) == 0 DO\n" +
                        "        print(\"FizzBuzz\");\n" +
                        "    ELSE IF rem(i, 3) == 0 DO\n" +
                        "        print(\"Fizz\");\n" +
                        "    ELSE IF rem(i, 5) == 0 DO\n" +
                        "        print(\"Buzz\");\n" +
                        "    ELSE\n" +
                        "        print(i);\n" +
                        "    END END END\n" +
                        "    i = i + 1;\n" +
                        "END", Arrays.asList(
                                new Token(Token.Type.IDENTIFIER, "lET", 0),
                                new Token(Token.Type.IDENTIFIER, "i", 4),
                                new Token(Token.Type.OPERATOR, "=", 6),
                                new Token(Token.Type.INTEGER, "1", 8),
                                new Token(Token.Type.OPERATOR, ";", 9),
                                new Token(Token.Type.IDENTIFIER, "WHILE", 11),
                                new Token(Token.Type.IDENTIFIER, "Integer", 13),
                                new Token(Token.Type.OPERATOR, ";", 20)
                ))

                 */
        );
    }

    @Test
    void testException() {
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());

            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
