package edu.cmu.sphinx.util.props;

import java.util.HashMap;
import java.util.Map;

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
        for (Map.Entry<String, GlobalProperty> entry : globalProperties.entrySet()) {
            put(entry.getKey(), new GlobalProperty(entry.getValue()));
        }
    }


    public void setValue(String propertyName, String value) {
        if (keySet().contains(propertyName)) {
            get(propertyName).setValue(value);
        } else {
            put(propertyName, new GlobalProperty(value));
        }
    }

    // todo implement hashCode


    public boolean equals(Object o) {
        if (o != null && o instanceof GlobalProperties) {
            GlobalProperties gp = (GlobalProperties) o;
            if (!keySet().equals(gp.keySet()))
                return false;

            //compare all values
            for (String key : gp.keySet()) {
                if (!get(key).equals(gp.get(key)))
                    return false;
            }

            return true;
        }

        return false;
    }
}
