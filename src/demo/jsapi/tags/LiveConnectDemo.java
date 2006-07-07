/* Copyright 1999,2004 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All Rights Reserved.  Use is subject to license terms.
 *
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL
 * WARRANTIES.
 */
package demo.jsapi.tags;

import javax.speech.Central;
import javax.speech.recognition.Recognizer;
import javax.speech.recognition.ResultAdapter;
import javax.speech.recognition.ResultEvent;
import javax.speech.recognition.ResultToken;
import javax.speech.recognition.FinalRuleResult;
import javax.speech.recognition.RuleGrammar;
import javax.speech.recognition.RuleParse;

import com.sun.speech.engine.recognition.BaseRecognizer;

import edu.cmu.sphinx.tools.tags.ObjectTagsParser;

/**
 * A simple application that uses the embedded object method to
 * determine the actions to perform when a user says something.
 * This uses the LiveConnectDemo.gram JSGF grammar.
 */
public class LiveConnectDemo {
    static final String[] utterances = {
        "I want a pizza with mushrooms and onions",
        "Mushroom pizza",
        "Sausage and pepperoni pizza",
        "I would like a pizza",
        "I want a cheese and mushroom pizza with onions",
        "I would like a burger",
        "I would like a burger with pickles onions lettuce and cheese",
        "I would like a burger with special sauce lettuce and cheese",
        "I want a pizza with pepperoni and cheese",
        "Cheeseburger with onions",
    };
    
    /**
     * The ActionTags parser.  This one is a subclass of the ActionTagsParser
     * and allows you to embed object instances within the tags in the
     * Grammar.
     */
    ObjectTagsParser parser;

    /**
     * Method to get an instance of an ObjectTagsParser.
     */
    private ObjectTagsParser getTagsParser() {
        if (parser == null) {
            parser = new ObjectTagsParser();
            parser.put("appObj", this);
        }
        return parser;
    }
    
    /**
     * Create a new LiveConnectDemo.
     */
    public LiveConnectDemo() {
    }

    public void doTest() throws Exception {
        BaseRecognizer recognizer = new BaseRecognizer();
        recognizer.allocate();
        
        RuleGrammar grammar = recognizer.loadJSGF(
            null, "demo.jsapi.tags.LiveConnectDemo");
        grammar.setEnabled(true);
        recognizer.commitChanges();

        for (int i = 0; i < utterances.length; i++) {
            RuleParse p = grammar.parse(utterances[i], null);

            if (p == null) {
                System.out.println("ILLEGAL UTTERANCE: " + utterances[i]);
                continue;
            }
            
            System.out.println();
            System.out.println("Utterance: " + utterances[i]);

            getTagsParser().parseTags(p);
        }
    }
    
    /**
     * Submit the order.
     */
    public static void submitOrder(OrderItem item) {
        System.out.println("    Order: " + item);
    }

    /**
     * Standalone operation.
     */
    static public void main(String[] args) {
        try {
            LiveConnectDemo demo = new LiveConnectDemo();
            demo.doTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
