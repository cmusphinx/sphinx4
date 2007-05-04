package edu.cmu.sphinx.util.props;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A logical property.
 *
 * @author Holger Brandl
 * @see ConfigurationManager
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@S4Property
public @interface S4Boolean {

    boolean defaultValue();

    boolean isNotDefined() default false;
}
