package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * An integer property.
 *
 * @author Holger Brandl
 * @see ConfigurationManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Integer {

    public static final int NOT_DEFINED = -918273645;


    int defaultValue() default NOT_DEFINED;


    int[] range() default {-Integer.MAX_VALUE, Integer.MAX_VALUE};


    boolean mandatory() default true;
}
