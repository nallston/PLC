package plc.homework;

import java.util.regex.Pattern;

/**
 * Contains {@link Pattern} constants, which are compiled regular expressions.
 * See the assignment page for resources on regexes as needed.
 */
public class Regex {

    public static final Pattern
            EMAIL = Pattern.compile("[A-Za-z0-9._]{2,}@[A-Za-z0-9~]+\\.([A-Za-z0-9-]+\\.)*[a-z]{3}"),
            ODD_STRINGS = Pattern.compile("(..){5,9}.{1}"), //TODO
            CHARACTER_LIST = Pattern.compile("\\[(\\]|('\\w'\\])|('\\w',\\s{0,1})+('\\w']))"), //TODO
            DECIMAL = Pattern.compile("-{0,1}(([1-9]\\d*)|0)\\.\\d+"), //TODO
            STRING = Pattern.compile("\"([^\\\\]|[\\\\][bnrt'\"\\\\])*\""); //TODO
}
