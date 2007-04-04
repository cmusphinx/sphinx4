package edu.cmu.sphinx.util.props.newconman;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A tag which superclasses all sphinx property annotations. Because there is no real inheritance for annotations all
 * child classes are annotated by this general property annotation.
 *
 * @author Holger Brandl
 * @see edu.cmu.sphinx.util.props.newconman.S4Component
 * @see edu.cmu.sphinx.util.props.newconman.S4Integer
 * @see edu.cmu.sphinx.util.props.newconman.S4ComponentList
 * @see edu.cmu.sphinx.util.props.newconman.S4Double
 * @see edu.cmu.sphinx.util.props.newconman.S4Boolean
 * @see edu.cmu.sphinx.util.props.newconman.S4String
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface S4Property {

}
