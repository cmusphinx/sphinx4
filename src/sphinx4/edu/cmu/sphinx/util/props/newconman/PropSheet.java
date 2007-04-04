package edu.cmu.sphinx.util.props.newconman;

import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;

import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

/**
 * A property sheet defines a collection of properties for a single component in the system.
 *
 * @author Holger Brandl
 */
public interface PropSheet {

    /**
     * Registers a new property which type and default value are defined by the given sphinx property.
     *
     * @param propName The name of the property to be registered.
     * @param property The property annoation masked by a proxy.
     */
    void registerProperty(String propName, S4PropWrapper property);


    /**
     * Sets the given property to the given name
     *
     * @param name the simple property name
     */
    void setString(String name);


    /**
     * Sets the given property to the given name
     *
     * @param name  the simple property name
     * @param value the value for the property
     */
    void setInt(String name, int value);


    /**
     * Sets the given property to the given name
     *
     * @param name the simple property name
     */
    void setFloat(String name);


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     */
    String getString(String name) throws PropertyException;


    /**
     * Sets the raw property to the given name
     *
     * @param key the simple property name
     * @param val the value for the property
     */
    public void setRaw(String key, Object val);


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    int getInt(String name) throws PropertyException;


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    float getFloat(String name) throws PropertyException;


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    double getDouble(String name) throws PropertyException;


    /**
     * Gets the value associated with this name
     *
     * @param name the name
     * @return the value
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    boolean getBoolean(String name) throws PropertyException;


    /**
     * Gets a resource associated with the given parameter name
     *
     * @param name the parameter name
     * @return the resource associated with the name or NULL if it doesn't exist.
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the resource cannot be found
     */
    URL getResource(String name);


    /**
     * Gets a component associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    SimpleConfigurable getComponent(String name) throws PropertyException;


    /**
     * Gets a list of components associated with the given parameter name
     *
     * @param name the parameter name
     * @return the component associated with the name
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the component does not exist or is of the wrong type.
     */
    List<SimpleConfigurable> getComponentList(String name) throws PropertyException;


    /**
     * Gets the list of strings associated with this name
     *
     * @param name the name
     * @return an array (possibly empty) of configurable strings
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if the named property is not of this type
     */
    List getStrings(String name);


    /**
     * Retrieves the names of all the properties currently defined for this property sheet
     *
     * @return the array of names
     */
    String[] getNames();


    /**
     * Gets the raw value associated with this name
     *
     * @param name the name
     * @return the value as an object (it could be a String or a String[] depending upon the property type)
     */
    Object getRaw(String name);


    /**
     * Gets the raw value associated with this name, no global symbol replacement is performed.
     *
     * @param name the name
     * @return the value as an object (it could be a String or a String[] depending upon the property type)
     */
    public Object getRawNoReplacement(String name);


    /**
     * Gets the owning property manager
     *
     * @return the property manager
     */
    ConfigurationManager getPropertyManager();


    /**
     * Returns a logger to use for this configurable component. The logger can be configured with the property:
     * 'logLevel' - The default logLevel value is define by the global property 'defaultLogLevel' (which defaults to
     * WARNING).
     *
     * @return the logger for this component
     * @throws edu.cmu.sphinx.util.props.PropertyException
     *          if an error occurs
     */
    Logger getLogger();


    /**
     * Dumps this sheet to the given stream
     *
     * @param out the print stream to dump the sheet on
     */
    public void dump(PrintStream out);


    public Collection<String> getRegisteredProperties();
}
