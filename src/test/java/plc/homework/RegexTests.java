package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. A framework of the test structure 
 * is provided, you will fill in the remaining pieces.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("UF CISE Domain", "otherdomain@ufl.cise.edu", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Multiple @", "missingdot@@gmail.com", false),
                Arguments.of("Domain is too large", "missingdot@gmail.comm", false),
                Arguments.of("Domain is too small", "missingdot@gmail.cm", false),
                Arguments.of("Symbols in domain", "missingdot@gma$il.com", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOddStringsRegex(String test, String input, boolean success) {
        test(input, Regex.ODD_STRINGS, success);
    }

    public static Stream<Arguments> testOddStringsRegex() {
        return Stream.of(
                // what have eleven letters and starts with gas?
                Arguments.of("Less than 10", "5five", false),
                Arguments.of("More than 20", "Iwroteawholesentance", false),
                Arguments.of("Odd String 11", "elevenright", true),
                Arguments.of("Odd String 19", "nineteen and eleven", true),
                Arguments.of("Odd over 21", "thisis twentyone char", false),
                Arguments.of("20 chars", "thisis ten", false),
                Arguments.of("10 chars", "this is twenty chars", false),
                Arguments.of("Odd String Expressions", "fifteen!!!*&^", true),
                Arguments.of("Odd String Space", "fifteen     ogs", true),
                Arguments.of("Odd String digit", "fifteen192702", true)

        );
    }

    @ParameterizedTest
    @MethodSource
    public void testCharacterListRegex(String test, String input, boolean success) {
        test(input, Regex.CHARACTER_LIST, success);
    }

    public static Stream<Arguments> testCharacterListRegex() {
        return Stream.of(
                Arguments.of("No Element", "[]", true),
                Arguments.of("Single Element", "['a']", true),
                Arguments.of("Multiple Elements", "['a','b','c']", true),
                Arguments.of("ME with spaces", "['a','b', 'c']", true),
                Arguments.of("ME with spaces or not", "['a', 'b','c', '8']", true),
                Arguments.of("ME with many spaces", "['a', 'b', 'c', '8', '[']", false),
                Arguments.of("ME too many spaces", "['a',  'b', 'c', '8', '[']", false),
                Arguments.of("Missing Front Bracket", "'a','b','c']", false),
                Arguments.of("Missing Back Bracket", "['a','b','c'", false),
                Arguments.of("Missing Commas", "['a' 'b' 'c']", false),

                Arguments.of("ME with space at end", "['a', 'b', 'c', '8', '['] ", false),
                Arguments.of("ME with comma at end", "['a', 'b', 'c', '8', '[',]", false),
                Arguments.of("ME with bracket ending inside set", "['a', 'b', 'c']'8', '[']", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testDecimalRegex(String test, String input, boolean success) {
        test(input, Regex.DECIMAL, success); //TODO
    }

    public static Stream<Arguments> testDecimalRegex() {
        return Stream.of(
                Arguments.of("Integer", "25", false),
                Arguments.of("Decimal", "25.02", true),
                Arguments.of("Leading 0 Decimal", "025.02", false),
                Arguments.of("Leading 0 <1 Decimal", "0.02", true),
                Arguments.of("Leading -0 > -1 Decimal", "-0.02", true),
                Arguments.of("Negative Decimal", "-25.02", true),
                Arguments.of("Double Negative Decimal", "--25.02", false),
                Arguments.of("Trailing 0 Decimal", "25.0200", true),
                Arguments.of("No Head Decimal", ".02", false),
                Arguments.of("Multiple Decimal", "10.0202.9", false),
                Arguments.of("Big Number", "1008920030.89", true),
                Arguments.of("No Trail Decimal", "25.", false),
                Arguments.of("Negative 0", "-0.0", true)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success); //TODO
    }

    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
                Arguments.of("Valid String", "\"Hello\"", true),
                Arguments.of("Valid Long String", "\"Hello this is a very long string of only characters\"", true),
                Arguments.of("VS with more quotes", "\"Hel\"lo\"", true),
                Arguments.of("VS No Front Quote", "Hello\"", false),
                Arguments.of("VS No Back Quote", "\"Hello", false),
                Arguments.of("VS with a \\", "\"Hello \\World\"", false),
                Arguments.of("VS with a \\digit", "\"Hello\\7seven\"", false),
                Arguments.of("VS with a Valid\\", "\"Hello \\\\World\"", true),
                Arguments.of("Valid \\\"", "\"Hello \\\"World\\\"\"", true),
                Arguments.of("Valid \'", "\"Hello\'C\'\"", true),
                Arguments.of("Bad \\", "\"Hello \\\rWorld\"", false),
                Arguments.of("Bad \b", "\"Hello \\\bWorld\"", false)

        );

    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
