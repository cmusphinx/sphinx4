package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A component property.
 *
 * @author Holger Brandl
 * @see ConMan
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Component {

    Class<? extends Configurable> type();


    Class<? extends Configurable> defaultClass() default Configurable.class;


    boolean mandatory() default false;
}
