package edu.cmu.sphinx.util.props.newconman;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A list property.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.newconman.ConMan
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4ComponentList {

    Class<? extends SimpleConfigurable> type();


    /**
     * A default list of <code>Configurable</code>s used to configure this component list given the case that no
     * component list was defined (via xml or during runtime).
     */
    Class<? extends SimpleConfigurable>[] defaultList() default {};
}
