package edu.cmu.sphinx.util.props;

import java.lang.reflect.Proxy;

/**
 * Wraps annotations
 *
 * @author Holger Brandl
 */
public class S4PropWrapper {

    private final Proxy annotation;


    public S4PropWrapper(Proxy annotation) {
        this.annotation = annotation;
    }


    public Proxy getAnnotation() {
        return annotation;
    }
}
