/**
 * Portions Copyright 2001-2003 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute,
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides the definitions for US English whitespace, punctuations,
 * prepunctuation, and postpunctuation symbols. It also contains a set of
 * Regular Expressions for the US English language. With regular expressions,
 * it specifies what are whitespace, letters in the alphabet, uppercase and
 * lowercase letters, alphanumeric characters, identifiers, integers, doubles,
 * digits, and 'comma and int'.
 *
 * It translates the following code from flite: src/regex/cst_regex.c
 * lang/usenglish/us_text.c
 */
class UsEnglish {
    /** default whitespace regular expression pattern */
    public static final String RX_DEFAULT_US_EN_WHITESPACE = "[ \n\t\r]+";
    /** default letter regular expression pattern */
    public static final String RX_DEFAULT_US_EN_ALPHABET = "[A-Za-z]+";
    /** default uppercase regular expression pattern */
    public static final String RX_DEFAULT_US_EN_UPPERCASE = "[A-Z]+";
    /** default lowercase regular expression pattern */
    public static final String RX_DEFAULT_US_EN_LOWERCASE = "[a-z]+";
    /** default alpha-numeric regular expression pattern */
    public static final String RX_DEFAULT_US_EN_ALPHANUMERIC = "[0-9A-Za-z]+";
    /** default identifier regular expression pattern */
    public static final String RX_DEFAULT_US_EN_IDENTIFIER =
            "[A-Za-z_][0-9A-Za-z_]+";
    /** default integer regular expression pattern */
    public static final String RX_DEFAULT_US_EN_INT = "-?[0-9]+";
    /** default double regular expression pattern */
    public static final String RX_DEFAULT_US_EN_DOUBLE =
            "-?(([0-9]+\\.[0-9]*)|([0-9]+)|(\\.[0-9]+))([eE][---+]?[0-9]+)?";
    /** default integer with commas regular expression pattern */
    public static final String RX_DEFAULT_US_EN_COMMAINT =
            "[0-9][0-9]?[0-9]?[,']([0-9][0-9][0-9][,'])*[0-9][0-9][0-9](\\.[0-9]+)?";
    /** default digits regular expression pattern */
    public static final String RX_DEFAULT_US_EN_DIGITS = "[0-9][0-9]*";
    /** default dotted abbreviation regular expression pattern */
    public static final String RX_DEFAULT_US_EN_DOTTED_ABBREV =
            "([A-Za-z]\\.)*[A-Za-z]";
    /** default ordinal number regular expression pattern */
    public static final String RX_DEFAULT_US_EN_ORDINAL_NUMBER =
            "[0-9][0-9,]*(th|TH|st|ST|nd|ND|rd|RD)";
    /** default has-vowel regular expression */
    public static final String RX_DEFAULT_HAS_VOWEL = ".*[aeiouAEIOU].*";
    /** default US money regular expression */
    public static final String RX_DEFAULT_US_MONEY = "\\$[0-9,]+(\\.[0-9]+)?";
    /** default -illion regular expression */
    public static final String RX_DEFAULT_ILLION = ".*illion";
    /** default digits2dash (e.g. 999-999-999) regular expression */
    public static final String RX_DEFAULT_DIGITS2DASH =
            "[0-9]+(-[0-9]+)(-[0-9]+)+";
    /** default digits/digits (e.g. 999/999) regular expression */
    public static final String RX_DEFAULT_DIGITSSLASHDIGITS = "[0-9]+/[0-9]+";
    /** default number time regular expression */
    public static final String RX_DEFAULT_NUMBER_TIME =
            "((0[0-2])|(1[0-9])):([0-5][0-9])";
    /** default Roman numerals regular expression */
    public static final String RX_DEFAULT_ROMAN_NUMBER =
            "(II?I?|IV|VI?I?I?|IX|X[VIX]*)";
    /** default drst "Dr. St" regular expression */
    public static final String RX_DEFAULT_DRST = "([dD][Rr]|[Ss][Tt])";
    /** default numess */
    public static final String RX_DEFAULT_NUMESS = "[0-9]+s";
    /** default 7-digit phone number */
    public static final String RX_DEFAULT_SEVEN_DIGIT_PHONE_NUMBER =
            "[0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]";
    /** default 4-digit number */
    public static final String RX_DEFAULT_FOUR_DIGIT = "[0-9][0-9][0-9][0-9]";
    /** default 3-digit number */
    public static final String RX_DEFAULT_THREE_DIGIT = "[0-9][0-9][0-9]";

    /** whitespace regular expression pattern */
    public static String RX_WHITESPACE = RX_DEFAULT_US_EN_WHITESPACE;
    /** letter regular expression pattern */
    public static String RX_ALPHABET = RX_DEFAULT_US_EN_ALPHABET;
    /** uppercase regular expression pattern */
    public static String RX_UPPERCASE = RX_DEFAULT_US_EN_UPPERCASE;
    /** lowercase regular expression pattern */
    public static String RX_LOWERCASE = RX_DEFAULT_US_EN_LOWERCASE;
    /** alphanumeric regular expression pattern */
    public static String RX_ALPHANUMERIC = RX_DEFAULT_US_EN_ALPHANUMERIC;
    /** identifier regular expression pattern */
    public static String RX_IDENTIFIER = RX_DEFAULT_US_EN_IDENTIFIER;
    /** integer regular expression pattern */
    public static String RX_INT = RX_DEFAULT_US_EN_INT;
    /** double regular expression pattern */
    public static String RX_DOUBLE = RX_DEFAULT_US_EN_DOUBLE;
    /** comma separated integer regular expression pattern */
    public static String RX_COMMAINT = RX_DEFAULT_US_EN_COMMAINT;
    /** digits regular expression pattern */
    public static String RX_DIGITS = RX_DEFAULT_US_EN_DIGITS;
    /** dotted abbreviation regular expression pattern */
    public static String RX_DOTTED_ABBREV = RX_DEFAULT_US_EN_DOTTED_ABBREV;
    /** ordinal number regular expression pattern */
    public static String RX_ORDINAL_NUMBER = RX_DEFAULT_US_EN_ORDINAL_NUMBER;
    /** has-vowel regular expression */
    public static final String RX_HAS_VOWEL = RX_DEFAULT_HAS_VOWEL;
    /** US money regular expression */
    public static final String RX_US_MONEY = RX_DEFAULT_US_MONEY;
    /** -illion regular expression */
    public static final String RX_ILLION = RX_DEFAULT_ILLION;
    /** digits2dash (e.g. 999-999-999) regular expression */
    public static final String RX_DIGITS2DASH = RX_DEFAULT_DIGITS2DASH;
    /** digits/digits (e.g. 999/999) regular expression */
    public static final String RX_DIGITSSLASHDIGITS =
            RX_DEFAULT_DIGITSSLASHDIGITS;
    /** number time regular expression */
    public static final String RX_NUMBER_TIME = RX_DEFAULT_NUMBER_TIME;
    /** Roman numerals regular expression */
    public static final String RX_ROMAN_NUMBER = RX_DEFAULT_ROMAN_NUMBER;
    /** drst "Dr. St" regular expression */
    public static final String RX_DRST = RX_DEFAULT_DRST;
    /** default numess */
    public static final String RX_NUMESS = RX_DEFAULT_NUMESS;
    /** 7-digit phone number */
    public static final String RX_SEVEN_DIGIT_PHONE_NUMBER =
            RX_DEFAULT_SEVEN_DIGIT_PHONE_NUMBER;
    /** 4-digit number */
    public static final String RX_FOUR_DIGIT = RX_DEFAULT_FOUR_DIGIT;
    /** 3-digit number */
    public static final String RX_THREE_DIGIT = RX_DEFAULT_THREE_DIGIT;

    // the following symbols are from lang/usenglish/us_text.c

    /** punctuation regular expression pattern */
    public static final String PUNCTUATION_SYMBOLS = "\"'`.,:;!?(){}[]";
    /** pre-punctuation regular expression pattern */
    public static final String PREPUNCTUATION_SYMBOLS = "\"'`({[";
    /** single char symbols regular expression pattern */
    public static final String SINGLE_CHAR_SYMBOLS = "";
    /** whitespace symbols regular expression pattern */
    public static final String WHITESPACE_SYMBOLS = " \t\n\r";

    /**
     * Not constructable
     */
    private UsEnglish() {}
}


/**
 * Converts the Tokens (in US English words) in an Utterance into a list of
 * words. It puts the produced list back into the Utterance. Usually, the
 * tokens that gets expanded are numbers like "23" (to "twenty" "three").
 * <p>
 * * It translates the following code from flite: <br>
 * <code>
 * lang/usenglish/us_text.c
 * </code>
 */
public class UsEnglishWordExpander implements WordExpander {
    // Patterns for regular expression matching
    private static final Pattern alphabetPattern;
    private static final Pattern commaIntPattern;
    private static final Pattern digits2DashPattern;
    private static final Pattern digitsPattern;
    private static final Pattern digitsSlashDigitsPattern;
    private static final Pattern dottedAbbrevPattern;
    private static final Pattern doublePattern;
    private static final Pattern drStPattern;
    private static final Pattern fourDigitsPattern;
    private static final Pattern illionPattern;
    private static final Pattern numberTimePattern;
    private static final Pattern numessPattern;
    private static final Pattern ordinalPattern;
    private static final Pattern romanNumbersPattern;
    private static final Pattern sevenPhoneNumberPattern;
    private static final Pattern threeDigitsPattern;
    private static final Pattern usMoneyPattern;

    static {
        alphabetPattern = Pattern.compile(UsEnglish.RX_ALPHABET);
        commaIntPattern = Pattern.compile(UsEnglish.RX_COMMAINT);
        digits2DashPattern = Pattern.compile(UsEnglish.RX_DIGITS2DASH);
        digitsPattern = Pattern.compile(UsEnglish.RX_DIGITS);
        digitsSlashDigitsPattern =
                Pattern.compile(UsEnglish.RX_DIGITSSLASHDIGITS);
        dottedAbbrevPattern = Pattern.compile(UsEnglish.RX_DOTTED_ABBREV);
        doublePattern = Pattern.compile(UsEnglish.RX_DOUBLE);
        drStPattern = Pattern.compile(UsEnglish.RX_DRST);
        fourDigitsPattern = Pattern.compile(UsEnglish.RX_FOUR_DIGIT);
        Pattern.compile(UsEnglish.RX_HAS_VOWEL);
        illionPattern = Pattern.compile(UsEnglish.RX_ILLION);
        numberTimePattern = Pattern.compile(UsEnglish.RX_NUMBER_TIME);
        numessPattern = Pattern.compile(UsEnglish.RX_NUMESS);
        ordinalPattern = Pattern.compile(UsEnglish.RX_ORDINAL_NUMBER);
        romanNumbersPattern = Pattern.compile(UsEnglish.RX_ROMAN_NUMBER);
        sevenPhoneNumberPattern =
                Pattern.compile(UsEnglish.RX_SEVEN_DIGIT_PHONE_NUMBER);
        threeDigitsPattern = Pattern.compile(UsEnglish.RX_THREE_DIGIT);
        usMoneyPattern = Pattern.compile(UsEnglish.RX_US_MONEY);
    }

    // King-like words
    private static final String[] kingNames = {"louis", "henry", "charles",
            "philip", "george", "edward", "pius", "william", "richard",
            "ptolemy", "john", "paul", "peter", "nicholas", "frederick",
            "james", "alfonso", "ivan", "napoleon", "leo", "gregory",
            "catherine", "alexandria", "pierre", "elizabeth", "mary", "elmo",
            "erasmus"};

    private static final String[] kingTitles = {"king", "queen", "pope",
            "duke", "tsar", "emperor", "shah", "caesar", "duchess", "tsarina",
            "empress", "baron", "baroness", "sultan", "count", "countess"};

    // Section-like words
    private static final String[] sectionTypes = {"section", "chapter",
            "part", "phrase", "verse", "scene", "act", "book", "volume",
            "chap", "war", "apollo", "trek", "fortran"};

    /**
     * Here we use a map for constant time matching, instead of using if
     * (A.equals(B) || A.equals(C) || ...) to match Strings
     */
    private static Map<String, String> kingSectionLikeMap = new HashMap<String, String>();

    private static final String KING_NAMES = "kingNames";
    private static final String KING_TITLES = "kingTitles";
    private static final String SECTION_TYPES = "sectionTypes";

    static {
        for (int i = 0; i < kingNames.length; i++) {
            kingSectionLikeMap.put(kingNames[i], KING_NAMES);
        }
        for (int i = 0; i < kingTitles.length; i++) {
            kingSectionLikeMap.put(kingTitles[i], KING_TITLES);
        }
        for (int i = 0; i < sectionTypes.length; i++) {
            kingSectionLikeMap.put(sectionTypes[i], SECTION_TYPES);
        }
    }


    // Finite state machines to check if a Token is pronounceable
    private PronounceableFSM prefixFSM = null;
    private PronounceableFSM suffixFSM = null;

    // List of US states abbreviations and their full names
    private static final String[][] usStates = {
            {"AL", "ambiguous", "alabama"}, {"Al", "ambiguous", "alabama"},
            {"Ala", "", "alabama"}, {"AK", "", "alaska"},
            {"Ak", "", "alaska"}, {"AZ", "", "arizona"},
            {"Az", "", "arizona"}, {"CA", "", "california"},
            {"Ca", "", "california"}, {"Cal", "ambiguous", "california"},
            {"Calif", "", "california"}, {"CO", "ambiguous", "colorado"},
            {"Co", "ambiguous", "colorado"}, {"Colo", "", "colorado"},
            {"DC", "", "d", "c"}, {"DE", "", "delaware"},
            {"De", "ambiguous", "delaware"}, {"Del", "ambiguous", "delaware"},
            {"FL", "", "florida"}, {"Fl", "ambiguous", "florida"},
            {"Fla", "", "florida"}, {"GA", "", "georgia"},
            {"Ga", "", "georgia"}, {"HI", "ambiguous", "hawaii"},
            {"Hi", "ambiguous", "hawaii"}, {"IA", "", "iowa"},
            {"Ia", "ambiguous", "iowa"}, {"IN", "ambiguous", "indiana"},
            {"In", "ambiguous", "indiana"}, {"Ind", "ambiguous", "indiana"},
            {"ID", "ambiguous", "idaho"}, {"IL", "ambiguous", "illinois"},
            {"Il", "ambiguous", "illinois"}, {"ILL", "ambiguous", "illinois"},
            {"KS", "", "kansas"}, {"Ks", "", "kansas"},
            {"Kans", "", "kansas"}, {"KY", "ambiguous", "kentucky"},
            {"Ky", "ambiguous", "kentucky"}, {"LA", "ambiguous", "louisiana"},
            {"La", "ambiguous", "louisiana"},
            {"Lou", "ambiguous", "louisiana"},
            {"Lous", "ambiguous", "louisiana"},
            {"MA", "ambiguous", "massachusetts"},
            {"Mass", "ambiguous", "massachusetts"},
            {"Ma", "ambiguous", "massachusetts"},
            {"MD", "ambiguous", "maryland"}, {"Md", "ambiguous", "maryland"},
            {"ME", "ambiguous", "maine"}, {"Me", "ambiguous", "maine"},
            {"MI", "", "michigan"}, {"Mi", "ambiguous", "michigan"},
            {"Mich", "ambiguous", "michigan"},
            {"MN", "ambiguous", "minnestota"},
            {"Minn", "ambiguous", "minnestota"},
            {"MS", "ambiguous", "mississippi"},
            {"Miss", "ambiguous", "mississippi"},
            {"MT", "ambiguous", "montanna"}, {"Mt", "ambiguous", "montanna"},
            {"MO", "ambiguous", "missouri"}, {"Mo", "ambiguous", "missouri"},
            {"NC", "ambiguous", "north", "carolina"},
            {"ND", "ambiguous", "north", "dakota"},
            {"NE", "ambiguous", "nebraska"}, {"Ne", "ambiguous", "nebraska"},
            {"Neb", "ambiguous", "nebraska"},
            {"NH", "ambiguous", "new", "hampshire"}, {"NV", "", "nevada"},
            {"Nev", "", "nevada"}, {"NY", "", "new", "york"},
            {"OH", "ambiguous", "ohio"}, {"OK", "ambiguous", "oklahoma"},
            {"Okla", "", "oklahoma"}, {"OR", "ambiguous", "oregon"},
            {"Or", "ambiguous", "oregon"}, {"Ore", "ambiguous", "oregon"},
            {"PA", "ambiguous", "pennsylvania"},
            {"Pa", "ambiguous", "pennsylvania"},
            {"Penn", "ambiguous", "pennsylvania"},
            {"RI", "ambiguous", "rhode", "island"},
            {"SC", "ambiguous", "south", "carlolina"},
            {"SD", "ambiguous", "south", "dakota"},
            {"TN", "ambiguous", "tennesee"}, {"Tn", "ambiguous", "tennesee"},
            {"Tenn", "ambiguous", "tennesee"}, {"TX", "ambiguous", "texas"},
            {"Tx", "ambiguous", "texas"}, {"Tex", "ambiguous", "texas"},
            {"UT", "ambiguous", "utah"}, {"VA", "ambiguous", "virginia"},
            {"WA", "ambiguous", "washington"},
            {"Wa", "ambiguous", "washington"},
            {"Wash", "ambiguous", "washington"},
            {"WI", "ambiguous", "wisconsin"},
            {"Wi", "ambiguous", "wisconsin"},
            {"WV", "ambiguous", "west", "virginia"},
            {"WY", "ambiguous", "wyoming"}, {"Wy", "ambiguous", "wyoming"},
            {"Wyo", "", "wyoming"}, {"PR", "ambiguous", "puerto", "rico"}};

    // Again map for constant time searching.
    private static Map<String, String[]> usStatesMap = new HashMap<String, String[]>();
    static {
        for (int i = 0; i < usStates.length; i++) {
            usStatesMap.put(usStates[i][0], usStates[i]);
        }
    };

    // class variables

    // the word relation that we are building
    private WordRelation wordRelation;

    // the current token Item
    private Item tokenItem;

    // a CART for classifying numbers
    private DecisionTree cart;

    /**
     * Constructs a default USTokenWordProcessor. It uses the USEnglish regular
     * expression set (USEngRegExp) by default.
     *
     * @param usNumbersCART the cart to use to classify numbers
     */
    public UsEnglishWordExpander() {
        try {
            cart = new DecisionTree(getClass().getResource("nums_cart.txt"));
            prefixFSM =
                    new PrefixFSM(getClass().getResource("prefix_fsm.txt"));
            suffixFSM =
                    new SuffixFSM(getClass().getResource("suffix_fsm.txt"));
        } catch (IOException e) {
            throw new IllegalStateException("resources not found", e);
        }
    }

    /**
     * Returns the currently processing token Item.
     *
     * @return the current token Item; null if no item
     */
    public Item getTokenItem() {
        return tokenItem;
    }

    /**
     * process the utterance
     *
     * @param utterance the utterance contain the tokens
     *
     * @throws ProcessException if an IOException is thrown during the
     *         processing of the utterance
     */
    public List<String> expand(String text) {
        
        String simplifiedText = simplifyChars(text);
        
        CharTokenizer tokenizer = new CharTokenizer();
        tokenizer.setWhitespaceSymbols(UsEnglish.WHITESPACE_SYMBOLS);
        tokenizer.setSingleCharSymbols(UsEnglish.SINGLE_CHAR_SYMBOLS);
        tokenizer.setPrepunctuationSymbols(UsEnglish.PREPUNCTUATION_SYMBOLS);
        tokenizer.setPostpunctuationSymbols(UsEnglish.PUNCTUATION_SYMBOLS);
        tokenizer.setInputText(simplifiedText);
        Utterance utterance = new Utterance(tokenizer);

        Relation tokenRelation;
        if ((tokenRelation = utterance.getRelation(Relation.TOKEN)) == null) {
            throw new IllegalStateException("token relation does not exist");
        }

        wordRelation = WordRelation.createWordRelation(utterance, this);

        for (tokenItem = tokenRelation.getHead(); tokenItem != null; tokenItem =
                tokenItem.getNext()) {

            FeatureSet featureSet = tokenItem.getFeatures();
            String tokenVal = featureSet.getString("name");

            // convert the token into a list of words
            tokenToWords(tokenVal);
        }

        List<String> words = new ArrayList<String>();
        for (Item item = utterance.getRelation(Relation.WORD).getHead(); item != null; item =
                item.getNext()) {
            if (!item.toString().isEmpty() && !item.toString().contains("#")) {
                words.add(item.toString());
            }
        }
        return words;
    }

    private String simplifyChars(String text) {
        text = text.replace('’', '\'');
        text = text.replace('‘', '\'');
        text = text.replace('”', '"');
        text = text.replace('“', '"');
        text = text.replace('»', '"');
        text = text.replace('«', '"');
        text = text.replace('–', '-');
        text = text.replace('—', ' ');
        text = text.replace('…', ' ');
        text = text.replace((char)0xc, ' ');
        return text;
    }

    /**
     * Returns true if the given token matches part of a phone number
     *
     * @param tokenItem the token
     * @param tokenVal the string value of the token
     *
     * @return true or false
     */
    private boolean matchesPartPhoneNumber(String tokenVal) {

        String n_name = (String) tokenItem.findFeature("n.name");
        String n_n_name = (String) tokenItem.findFeature("n.n.name");
        String p_name = (String) tokenItem.findFeature("p.name");
        String p_p_name = (String) tokenItem.findFeature("p.p.name");

        boolean matches3DigitsP_name = matches(threeDigitsPattern, p_name);

        return ((matches(threeDigitsPattern, tokenVal) && ((!matches(
                digitsPattern, p_name) && matches(threeDigitsPattern, n_name) && matches(
                    fourDigitsPattern, n_n_name))
                || (matches(sevenPhoneNumberPattern, n_name)) || (!matches(
                digitsPattern, p_p_name) && matches3DigitsP_name && matches(
                    fourDigitsPattern, n_name)))) || (matches(
                fourDigitsPattern, tokenVal) && (!matches(digitsPattern,
                n_name) && matches3DigitsP_name && matches(threeDigitsPattern,
                    p_p_name))));
    }


    /**
     * Converts the given Token into (word) Items in the WordRelation.
     *
     * @param tokenVal the String value of the token, which may or may not be
     *        same as the one in called "name" in flite
     *
     */
    private void tokenToWords(String tokenVal) {
        FeatureSet tokenFeatures = tokenItem.getFeatures();
        String itemName = tokenFeatures.getString("name");
        int tokenLength = tokenVal.length();

        if (tokenFeatures.isPresent("phones")) {
            wordRelation.addWord(tokenVal);

        } else if ((tokenVal.equals("a") || tokenVal.equals("A"))
                && ((tokenItem.getNext() == null)
                        || !(tokenVal.equals(itemName)) || !(((String) tokenItem
                            .findFeature("punc")).equals("")))) {
            /* if A is a sub part of a token, then its ey not ah */
            wordRelation.addWord("_a");

        } else if (matches(alphabetPattern, tokenVal)) {

            if (matches(romanNumbersPattern, tokenVal)) {

                /* XVIII */
                romanToWords(tokenVal);

            } else if (matches(illionPattern, tokenVal)
                    && matches(usMoneyPattern,
                            (String) tokenItem.findFeature("p.name"))) {
                /* $ X -illion */
                wordRelation.addWord(tokenVal);
                wordRelation.addWord("dollars");

            } else if (matches(drStPattern, tokenVal)) {
                /* St Andrew's St, Dr King Dr */
                drStToWords(tokenVal);
            } else if (tokenVal.equals("Mr")) {
                tokenItem.getFeatures().setString("punc", "");
                wordRelation.addWord("mister");
            } else if (tokenVal.equals("Mrs")) {
                tokenItem.getFeatures().setString("punc", "");
                wordRelation.addWord("missus");
            } else if (tokenLength == 1
                    && Character.isUpperCase(tokenVal.charAt(0))
                    && ((String) tokenItem.findFeature("n.whitespace"))
                            .equals(" ")
                    && Character.isUpperCase(((String) tokenItem
                            .findFeature("n.name")).charAt(0))) {

                tokenFeatures.setString("punc", "");
                String aaa = tokenVal.toLowerCase();
                if (aaa.equals("a")) {
                    wordRelation.addWord("_a");
                } else {
                    wordRelation.addWord(aaa);
                }
            } else if (isStateName(tokenVal)) {
                /*
                 * The name of a US state isStateName() has already added the
                 * full name of the state, so we're all set.
                 */
            } else if (tokenLength > 1 && !isPronounceable(tokenVal)) {
                /* Need common exception list */
                /* unpronouncable list of alphas */
                NumberExpander.expandLetters(tokenVal, wordRelation);

            } else {
                /* just a word */
                wordRelation.addWord(tokenVal.toLowerCase());
            }

        } else if (matches(dottedAbbrevPattern, tokenVal)) {

            /* U.S.A. */
            // remove all dots
            NumberExpander.expandLetters(tokenVal.replace(".", ""),
                    wordRelation);

        } else if (matches(commaIntPattern, tokenVal)) {

            /* 99,999,999 */
            NumberExpander.expandReal(tokenVal.replace(",", "").replace("'", ""), wordRelation);

        } else if (matches(sevenPhoneNumberPattern, tokenVal)) {

            /* 234-3434 telephone numbers */
            int dashIndex = tokenVal.indexOf('-');
            String aaa = tokenVal.substring(0, dashIndex);
            String bbb = tokenVal.substring(dashIndex + 1);

            NumberExpander.expandDigits(aaa, wordRelation);
            wordRelation.addBreak();
            NumberExpander.expandDigits(bbb, wordRelation);

        } else if (matchesPartPhoneNumber(tokenVal)) {

            /* part of a telephone number */
            String punctuation = (String) tokenItem.findFeature("punc");
            if (punctuation.equals("")) {
                tokenItem.getFeatures().setString("punc", ",");
            }
            NumberExpander.expandDigits(tokenVal, wordRelation);
            wordRelation.addBreak();

        } else if (matches(numberTimePattern, tokenVal)) {
            /* 12:35 */
            int colonIndex = tokenVal.indexOf(':');
            String aaa = tokenVal.substring(0, colonIndex);
            String bbb = tokenVal.substring(colonIndex + 1);

            NumberExpander.expandNumber(aaa, wordRelation);
            if (!(bbb.equals("00"))) {
                NumberExpander.expandID(bbb, wordRelation);
            }
        } else if (matches(digits2DashPattern, tokenVal)) {
            /* 999-999-999 */
            digitsDashToWords(tokenVal);
        } else if (matches(digitsPattern, tokenVal)) {
            digitsToWords(tokenVal);
        } else if (tokenLength == 1
                && Character.isUpperCase(tokenVal.charAt(0))
                && ((String) tokenItem.findFeature("n.whitespace"))
                        .equals(" ")
                && Character.isUpperCase(((String) tokenItem
                        .findFeature("n.name")).charAt(0))) {

            tokenFeatures.setString("punc", "");
            String aaa = tokenVal.toLowerCase();
            if (aaa.equals("a")) {
                wordRelation.addWord("_a");
            } else {
                wordRelation.addWord(aaa);
            }
        } else if (matches(doublePattern, tokenVal)) {
            NumberExpander.expandReal(tokenVal, wordRelation);
        } else if (matches(ordinalPattern, tokenVal)) {
            /* explicit ordinals */
            String aaa = tokenVal.substring(0, tokenLength - 2);
            NumberExpander.expandOrdinal(aaa, wordRelation);
        } else if (matches(usMoneyPattern, tokenVal)) {
            /* US money */
            usMoneyToWords(tokenVal);
        } else if (tokenLength > 0 && tokenVal.charAt(tokenLength - 1) == '%') {
            /* Y% */
            tokenToWords(tokenVal.substring(0, tokenLength - 1));
            wordRelation.addWord("percent");
        } else if (matches(numessPattern, tokenVal)) {
            NumberExpander.expandNumess(tokenVal.substring(0, tokenLength - 1), wordRelation);
        } else if (matches(digitsSlashDigitsPattern, tokenVal)
                && tokenVal.equals(itemName)) {
            digitsSlashDigitsToWords(tokenVal);
        } else if (tokenVal.indexOf('-') != -1) {
            dashToWords(tokenVal);
        } else if (tokenLength > 1 && !matches(alphabetPattern, tokenVal)) {
            notJustAlphasToWords(tokenVal);
        } else if (tokenVal.equals("&")) {
            // &
            wordRelation.addWord("and");
        } else if (tokenVal.equals("-")) {
            // Skip it
        } else {
            // Just a word.
            wordRelation.addWord(tokenVal.toLowerCase());
        }
    }

    /**
     * Convert the given digit token with dashes (e.g. 999-999-999) into (word)
     * Items in the WordRelation.
     *
     * @param tokenVal the digit string
     */
    private void digitsDashToWords(String tokenVal) {
        int tokenLength = tokenVal.length();
        int a = 0;
        for (int p = 0; p <= tokenLength; p++) {
            if (p == tokenLength || tokenVal.charAt(p) == '-') {
                String aaa = tokenVal.substring(a, p);
                NumberExpander.expandDigits(aaa, wordRelation);
                wordRelation.addBreak();
                a = p + 1;
            }
        }
    }

    /**
     * Convert the given digit token into (word) Items in the WordRelation.
     *
     * @param tokenVal the digit string
     */
    private void digitsToWords(String tokenVal) {
        FeatureSet featureSet = tokenItem.getFeatures();
        String nsw = "";
        if (featureSet.isPresent("nsw")) {
            nsw = featureSet.getString("nsw");
        }

        if (nsw.equals("nide")) {
            NumberExpander.expandID(tokenVal, wordRelation);
        } else {
            String rName = featureSet.getString("name");
            String digitsType = null;

            if (tokenVal.equals(rName)) {
                digitsType = (String) cart.interpret(tokenItem);
            } else {
                featureSet.setString("name", tokenVal);
                digitsType = (String) cart.interpret(tokenItem);
                featureSet.setString("name", rName);
            }

            if (digitsType.equals("ordinal")) {
                NumberExpander.expandOrdinal(tokenVal, wordRelation);
            } else if (digitsType.equals("digits")) {
                NumberExpander.expandDigits(tokenVal, wordRelation);
            } else if (digitsType.equals("year")) {
                NumberExpander.expandID(tokenVal, wordRelation);
            } else {
                NumberExpander.expandNumber(tokenVal, wordRelation);
            }
        }
    }

    /**
     * Converts the given Roman numeral string into (word) Items in the
     * WordRelation.
     *
     * @param romanString the roman numeral string
     */
    private void romanToWords(String romanString) {
        String punctuation = (String) tokenItem.findFeature("p.punc");

        if (punctuation.equals("")) {
            /* no preceeding punctuation */
            String n = String.valueOf(NumberExpander.expandRoman(romanString));

            if (kingLike(tokenItem)) {
                wordRelation.addWord("the");
                NumberExpander.expandOrdinal(n, wordRelation);
            } else if (sectionLike(tokenItem)) {
                NumberExpander.expandNumber(n, wordRelation);
            } else {
                NumberExpander.expandLetters(romanString, wordRelation);
            }
        } else {
            NumberExpander.expandLetters(romanString, wordRelation);
        }
    }

    /**
     * Returns true if the given key is in the {@link #kingSectionLikeMap} map,
     * and the value is the same as the given value.
     *
     * @param key key to look for in the map
     * @param value the value to match
     *
     * @return true if it matches, or false if it does not or if the key is not
     *         mapped to any value in the map.
     */
    private static boolean inKingSectionLikeMap(String key, String value) {
        if (kingSectionLikeMap.containsKey(key)) {
            return kingSectionLikeMap.get(key).equals(value);
        }
        return false;
    }

    /**
     * Returns true if the given token item contains a token that is in a
     * king-like context, e.g., "King" or "Louis".
     *
     * @param tokenItem the token item to check
     *
     * @return true or false
     */
    public static boolean kingLike(Item tokenItem) {
        String kingName =
                ((String) tokenItem.findFeature("p.name")).toLowerCase();
        if (inKingSectionLikeMap(kingName, KING_NAMES)) {
            return true;
        } else {
            String kingTitle =
                    ((String) tokenItem.findFeature("p.p.name")).toLowerCase();
            return inKingSectionLikeMap(kingTitle, KING_TITLES);
        }
    }

    /**
     * Returns true if the given token item contains a token that is in a
     * section-like context, e.g., "chapter" or "act".
     *
     * @param tokenItem the token item to check
     *
     * @return true or false
     */
    public static boolean sectionLike(Item tokenItem) {
        String sectionType =
                ((String) tokenItem.findFeature("p.name")).toLowerCase();
        return inKingSectionLikeMap(sectionType, SECTION_TYPES);
    }

    /**
     * Converts the given string containing "St" and "Dr" to (word) Items in
     * the WordRelation.
     *
     * @param drStString the string with "St" and "Dr"
     */
    private void drStToWords(String drStString) {
        String street = null;
        String saint = null;
        char c0 = drStString.charAt(0);

        if (c0 == 's' || c0 == 'S') {
            street = "street";
            saint = "saint";
        } else {
            street = "drive";
            saint = "doctor";
        }

        FeatureSet featureSet = tokenItem.getFeatures();
        String punctuation = featureSet.getString("punc");

        String featPunctuation = (String) tokenItem.findFeature("punc");

        if (tokenItem.getNext() == null || punctuation.indexOf(',') != -1) {
            wordRelation.addWord(street);
        } else if (featPunctuation.equals(",")) {
            wordRelation.addWord(saint);
        } else {
            String pName = (String) tokenItem.findFeature("p.name");
            String nName = (String) tokenItem.findFeature("n.name");

            char p0 = pName.charAt(0);
            char n0 = nName.charAt(0);

            if (Character.isUpperCase(p0) && Character.isLowerCase(n0)) {
                wordRelation.addWord(street);
            } else if (Character.isDigit(p0) && Character.isLowerCase(n0)) {
                wordRelation.addWord(street);
            } else if (Character.isLowerCase(p0) && Character.isUpperCase(n0)) {
                wordRelation.addWord(saint);
            } else {
                String whitespace =
                        (String) tokenItem.findFeature("n.whitespace");
                if (whitespace.equals(" ")) {
                    wordRelation.addWord(saint);
                } else {
                    wordRelation.addWord(street);
                }
            }
        }

        if (punctuation != null && punctuation.equals(".")) {
            featureSet.setString("punc", "");
        }
    }

    /**
     * Converts US money string into (word) Items in the WordRelation.
     *
     * @param tokenVal the US money string
     */
    private void usMoneyToWords(String tokenVal) {
        int dotIndex = tokenVal.indexOf('.');
        if (matches(illionPattern, (String) tokenItem.findFeature("n.name"))) {
            NumberExpander.expandReal(tokenVal.substring(1), wordRelation);
        } else if (dotIndex == -1) {
            String aaa = tokenVal.substring(1);
            tokenToWords(aaa);
            if (aaa.equals("1")) {
                wordRelation.addWord("dollar");
            } else {
                wordRelation.addWord("dollars");
            }
        } else if (dotIndex == (tokenVal.length() - 1)
                || (tokenVal.length() - dotIndex) > 3) {
            // Simply read as mumble point mumble.
            NumberExpander.expandReal(tokenVal.substring(1), wordRelation);
            wordRelation.addWord("dollars");
        } else {
            String aaa = tokenVal.substring(1, dotIndex).replace(",", "");
            String bbb = tokenVal.substring(dotIndex + 1);

            NumberExpander.expandNumber(aaa, wordRelation);

            if (aaa.equals("1")) {
                wordRelation.addWord("dollar");
            } else {
                wordRelation.addWord("dollars");
            }

            if (bbb.equals("00")) {
                // Add nothing to the word list.
            } else {
                NumberExpander.expandNumber(bbb, wordRelation);
                if (bbb.equals("01")) {
                    wordRelation.addWord("cent");
                } else {
                    wordRelation.addWord("cents");
                }
            }
        }
    }

    /**
     * Convert the given digits/digits string into word (Items) in the
     * WordRelation.
     *
     * @param tokenVal the digits/digits string
     */
    private void digitsSlashDigitsToWords(String tokenVal) {

        /* might be fraction, or not */
        int index = tokenVal.indexOf('/');
        String aaa = tokenVal.substring(0, index);
        String bbb = tokenVal.substring(index + 1);
        int a;

        // if the previous token is a number, add an "and"
        if (matches(digitsPattern, (String) tokenItem.findFeature("p.name"))
                && tokenItem.getPrevious() != null) {
            wordRelation.addWord("and");
        }

        if (aaa.equals("1") && bbb.equals("2")) {
            wordRelation.addWord("a");
            wordRelation.addWord("half");
        } else if ((a = Integer.parseInt(aaa)) < (Integer.parseInt(bbb))) {
            NumberExpander.expandNumber(aaa, wordRelation);
            NumberExpander.expandOrdinal(bbb, wordRelation);
            if (a > 1) {
                wordRelation.addWord("'s");
            }
        } else {
            NumberExpander.expandNumber(aaa, wordRelation);
            wordRelation.addWord("slash");
            NumberExpander.expandNumber(bbb, wordRelation);
        }
    }

    /**
     * Convert the given dashed string (e.g. "aaa-bbb") into (word) Items in
     * the WordRelation.
     *
     * @param tokenVal the dashed string
     */
    private void dashToWords(String tokenVal) {
        int index = tokenVal.indexOf('-');
        String aaa = tokenVal.substring(0, index);
        String bbb = tokenVal.substring(index + 1, tokenVal.length());

        if (matches(digitsPattern, aaa) && matches(digitsPattern, bbb)) {
            FeatureSet featureSet = tokenItem.getFeatures();
            featureSet.setString("name", aaa);
            tokenToWords(aaa);
            wordRelation.addWord("to");
            featureSet.setString("name", bbb);
            tokenToWords(bbb);
            featureSet.setString("name", "");
        } else {
            tokenToWords(aaa);
            tokenToWords(bbb);
        }
    }

    /**
     * Convert the given string (which does not only consist of alphabet) into
     * (word) Items in the WordRelation.
     *
     * @param tokenVal the string
     */
    private void notJustAlphasToWords(String tokenVal) {
        /* its not just alphas */
        int index = 0;
        int tokenLength = tokenVal.length();

        for (; index < tokenLength - 1; index++) {
            if (isTextSplitable(tokenVal, index)) {
                break;
            }
        }
        if (index == tokenLength - 1) {
            wordRelation.addWord(tokenVal.toLowerCase());
            return;
        }

        String aaa = tokenVal.substring(0, index + 1);
        String bbb = tokenVal.substring(index + 1, tokenLength);

        FeatureSet featureSet = tokenItem.getFeatures();
        featureSet.setString("nsw", "nide");
        tokenToWords(aaa);
        tokenToWords(bbb);
    }

    /**
     * Returns true if the given word is pronounceable. This method is
     * originally called us_aswd() in Flite 1.1.
     *
     * @param word the word to test
     *
     * @return true if the word is pronounceable, false otherwise
     */
    public boolean isPronounceable(String word) {
        String lcWord = word.toLowerCase();
        return prefixFSM.accept(lcWord) && suffixFSM.accept(lcWord);
    }

    /**
     * Returns true if the given token is the name of a US state. If it is, it
     * will add the name of the state to (word) Items in the WordRelation.
     *
     * @param tokenVal the token string
     */
    private boolean isStateName(String tokenVal) {
        String[] state = (String[]) usStatesMap.get(tokenVal);
        if (state != null) {
            boolean expandState = false;

            // check to see if the state initials are ambiguous
            // in the English language
            if (state[1].equals("ambiguous")) {
                String previous = (String) tokenItem.findFeature("p.name");
                String next = (String) tokenItem.findFeature("n.name");

                int nextLength = next.length();
                FeatureSet featureSet = tokenItem.getFeatures();

                // check if the previous word starts with a capital letter,
                // is at least 3 letters long, is an alphabet sequence,
                // and has a comma.
                boolean previousIsCity =
                        (Character.isUpperCase(previous.charAt(0))
                                && previous.length() > 2
                                && matches(alphabetPattern, previous) && tokenItem
                                .findFeature("p.punc").equals(","));

                // check if next token starts with a lower case, or
                // this is the end of sentence, or if next token
                // is a period (".") or a zip code (5 or 10 digits).
                boolean nextIsGood =
                        (Character.isLowerCase(next.charAt(0))
                                || tokenItem.getNext() == null
                                || featureSet.getString("punc").equals(".") || ((nextLength == 5 || nextLength == 10) && matches(
                                digitsPattern, next)));

                if (previousIsCity && nextIsGood) {
                    expandState = true;
                } else {
                    expandState = false;
                }
            } else {
                expandState = true;
            }
            if (expandState) {
                for (int j = 2; j < state.length; j++) {
                    if (state[j] != null) {
                        wordRelation.addWord(state[j]);
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if the given input matches the given Pattern.
     *
     * @param pattern the pattern to match
     * @param input the string to test
     *
     * @return <code>true</code> if the input string matches the given Pattern;
     *         <code>false</code> otherwise
     */
    private static boolean matches(Pattern pattern, String input) {
        Matcher m = pattern.matcher(input);
        return m.matches();
    }

    /**
     * Determines if the character at the given position of the given input
     * text is splittable. A character is splittable if:
     * <p>
     * 1) the character and the following character are not letters in the
     * English alphabet (A-Z and a-z)
     * <p>
     * 2) the character and the following character are not digits (0-9)
     * <p>
     *
     * @param text the text containing the character of interest
     * @param index the index of the character of interest
     *
     * @return true if the position of the given text is splittable false
     *         otherwise
     */
    private static boolean isTextSplitable(String text, int index) {


        char c0 = text.charAt(index);
        char c1 = text.charAt(index + 1);

        if (Character.isLetter(c0) && Character.isLetter(c1)) {
            return false;
        } else if (Character.isDigit(c0) && Character.isDigit(c1)) {
            return false;
        } else if (c0 == '\'' || Character.isLetter(c1)) {
            return false;
        } else if (c1 == '\'' || Character.isLetter(c0)) {
            return false;
        } else {
            return true;
        }
    }
}
