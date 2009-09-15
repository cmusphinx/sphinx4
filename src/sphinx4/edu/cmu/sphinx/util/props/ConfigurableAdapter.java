package edu.cmu.sphinx.util.props;

import java.util.logging.Logger;

/**
 * An default (abstract) implementation of a configurable that implements a meaning {@code toString()} and keeps a
 * references to the {@code Confurable}'s logger.
 *
 * @author Holger Brandl
 */
public abstract class ConfigurableAdapter implements Configurable{

    private String name;
    protected Logger logger;

    public ConfigurableAdapter() {
    }

    public void newProperties(PropertySheet ps) throws PropertyException {
        init( ps.getInstanceName(), ps.getLogger());
    }

    public ConfigurableAdapter(String name, Logger logger) {
        init(name, logger);
    }

    private void init(String name, Logger logger) {
        this.name = name;
        this.logger = logger;

        // fix null names
        name =  name != null ? name : getClass().getSimpleName();
    }

    /** Retunrs the configuration name this {@code Configurable}. */
    public String getName() {
        return name;
    }


    /**
     * Returns the name of this BaseDataProcessor.
     *
     * @return the name of this BaseDataProcessor
     */
    public String toString() {
        return name != null ? name : getClass().getSimpleName();
    }
}
