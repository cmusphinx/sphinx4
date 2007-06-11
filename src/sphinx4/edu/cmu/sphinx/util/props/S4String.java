package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A string property.
 *
 * @author Holger Brandl
 * @see ConfigurationManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4String {

    public static final String NOT_DEFINED = "nullnullnull";


    String defaultValue() default NOT_DEFINED; // this default value will be mapped to zero by the configuration manager


    String[] range() default {};


    boolean mandatory() default true;
}
