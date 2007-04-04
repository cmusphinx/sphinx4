package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.props.Registry;

/**
 * DOCUMENT ME!
 *
 * @author Holger Brandl
 */

/** Represents an entry in the symbol table */
public class SimpleSymbol {

    private String name;
    private SimpleConfigurable object;
    private PropSheet propertySheet;
    private Registry registry;


    /**
     * Creates a symbol table entry
     *
     * @param name the name of the symbol
     */
    public SimpleSymbol(String name, PropSheet propertySheet, SimpleConfigurable obj) {
        this.name = name;
        this.propertySheet = propertySheet;
        this.object = obj;
    }


    /** @return Returns the name. */
    public String getName() {
        return name;
    }


    /** @return Returns the object. */
    public SimpleConfigurable getObject() {
        return object;
    }


    /** @return Returns the propertySheet. */
    public PropSheet getPropertySheet() {
        return propertySheet;
    }
}
