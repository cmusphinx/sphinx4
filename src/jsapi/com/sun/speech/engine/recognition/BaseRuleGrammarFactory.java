package com.sun.speech.engine.recognition;

import javax.speech.recognition.Recognizer;

public class BaseRuleGrammarFactory implements RuleGrammarFactory {

	Recognizer recognizer;
	
	public BaseRuleGrammarFactory (Recognizer recognizer) {
		this.recognizer = recognizer;
	}
	
	@Override
	public BaseRuleGrammar newGrammar(String name) {
		if (recognizer != null)
			return (BaseRuleGrammar) recognizer.newRuleGrammar(name);
		else
			return new BaseRuleGrammar (null, name);
	}
}
