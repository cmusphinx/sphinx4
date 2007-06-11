package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * * A double property.
 *
 * @author Holger Brandl
 * @see ConfigurationManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Double {

    public static final double NOT_DEFINED = -918273645.12345;


    double defaultValue() default NOT_DEFINED;


    double[] range() default {-Double.MAX_VALUE, Double.MAX_VALUE};


    boolean mandatory() default true;
}
