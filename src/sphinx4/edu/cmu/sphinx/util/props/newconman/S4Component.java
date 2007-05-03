package edu.cmu.sphinx.util.props.newconman;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A component property.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.newconman.ConMan
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Component {

    Class<? extends SimpleConfigurable> type();


    Class<? extends SimpleConfigurable> defaultClass() default SimpleConfigurable.class;


    boolean mandatory() default false;
}
