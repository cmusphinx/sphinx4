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

import edu.cmu.sphinx.tools.tags.ActionTagsParser;

/**
 * A simple application that uses the feature/value method to
 * determine the actions to perform when a user says something.
 * This uses the FeatureValueDemo.gram JSGF grammar.
 */
public class FeatureValueDemo {
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
    
    static ActionTagsParser parser;
    
    static public ActionTagsParser getTagsParser() {
        if (parser == null) {
            parser = new ActionTagsParser();
        }
        return parser;
    }
    
    static public void doTest() throws Exception {
        BaseRecognizer recognizer = new BaseRecognizer();
        recognizer.allocate();
        
        RuleGrammar grammar = recognizer.loadJSGF(
            null, "demo.jsapi.tags.FeatureValueDemo");
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
            processResult(getTagsParser());
        }
    }

    /**
     * Post-process the results from the ActionTags parser.  This should
     * be called after the parseTags method of the ActionTags parser has
     * been called with the RuleParse from a ResultEvent.
     */
    protected static void processResult(ActionTagsParser parser) {
        // Determine what command should be performed.
        //
        String command =
            (String) parser.get("command");

        // Perform the appropriate command.
        //
        if (command == null) {
            System.out.println("MISTAKE IN GRAMMAR");
        } else if (command.equals("buyPizza")) {
            Pizza pizza = new Pizza();
            Object toppings = parser.get("item.toppings");
            if (toppings != null) {
                Double numToppings =
                    (Double) parser.get("item.toppings.length");
                for (int i = 0; i < numToppings.intValue(); i++) {
                    String topping = (String)
                        parser.get("item.toppings[" + i + "]");
                    pizza.addTopping(topping);
                }
            }
            submitOrder(pizza);
        } else if (command.equals("buyBurger")) {
            Burger burger = new Burger();
            Object toppings = parser.get("item.toppings");
            if (toppings != null) {
                Double numToppings =
                    (Double) parser.get("item.toppings.length");
                for (int i = 0; i < numToppings.intValue(); i++) {
                    String topping = (String)
                        parser.get("item.toppings[" + i + "]");
                    burger.addTopping(topping);
                }
            }
            Object condiments = parser.get("item.condiments");
            if (condiments != null) {
                Double numCondiments =
                    (Double) parser.get("item.condiments.length");
                for (int i = 0; i < numCondiments.intValue(); i++) {
                    String condiment = (String)
                        parser.evaluateString("item.condiments[" + i + "]");
                    burger.addCondiment(condiment);
                }
            }
            submitOrder(burger);
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
            doTest();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
