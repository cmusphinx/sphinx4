package edu.cmu.sphinx.util.props;

/**
 * A global property of the sphinx configuration system
 *
 * @author Holger Brandl
 */
public class GlobalProperty {

    Object value;


    public GlobalProperty(Object value) {
        this.value = value;
    }


    public Object getValue() {
        return value;
    }


    public void setValue(Object value) {
        this.value = value;
    }


    public String toString() {
        return value != null ? value.toString() : null;
    }
}
