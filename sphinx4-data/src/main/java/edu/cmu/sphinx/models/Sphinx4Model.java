package edu.cmu.sphinx.models;

/**
 * Empty class to load resources from classpath.
 * 
 * Use this class in a couple {@link ClassLoader#getResource(String)} to
 * provide a shortcut to the package prefix.
 * 
 * <pre>
 * {@code
 * String path = "models/acoustic/wsj/noisedict";
 * URL url = Resources.getResource(Sphinx4Model.class, path);
 * }
 * </pre>
 * 
 * @author Alexander Solovets
 */
public class Sphinx4Model {
}
