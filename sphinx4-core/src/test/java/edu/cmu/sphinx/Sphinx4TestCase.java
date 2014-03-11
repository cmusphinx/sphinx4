package edu.cmu.sphinx;

import java.io.File;
import java.net.URL;

public class Sphinx4TestCase {

    protected URL getResourceUrl(String resourceName) {
        String parent = getClass().getPackage().getName().replace(".", "/");
        String path = new File("/" + parent, resourceName).getPath();
        return getClass().getResource(path);
    }
    
    protected File getResourceFile(String resourceName) {
        return new File(getResourceUrl(resourceName).getFile());
    }
}
