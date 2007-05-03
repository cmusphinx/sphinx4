package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An integer property.
 *
 * @author Holger Brandl
 * @see ConMan
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Integer {

    int defaultValue();


    int[] range() default {-Integer.MAX_VALUE, Integer.MAX_VALUE};
}
