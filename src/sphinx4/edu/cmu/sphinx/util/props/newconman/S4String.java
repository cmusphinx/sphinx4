package edu.cmu.sphinx.util.props.newconman;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A string property.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.newconman.ConMan
 */
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4String {

    String defaultValue();


    String[] range() default {};
}
