package edu.cmu.sphinx.util.props;

import java.util.HashMap;

/**
 * A collection of global properties used within a sphinx4-system configuration
 *
 * @author Holger Brandl
 * @see ConfigurationManager
 */
public class GlobalProperties extends HashMap<String, GlobalProperty> {

    public GlobalProperties() {

    }


    public GlobalProperties(GlobalProperties globalProperties) {
        for (String key : globalProperties.keySet()) {
            put(key, new GlobalProperty(globalProperties.get(key)));
        }
    }


    public void setValue(String propertyName, String value) {
        if (keySet().contains(propertyName)) {
            get(propertyName).setValue(value);
        } else {
            put(propertyName, new GlobalProperty(value));
        }
    }
}
