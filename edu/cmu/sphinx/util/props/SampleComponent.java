/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electronic Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.util.props;
import java.io.File;
import java.net.URL;
import java.util.List;
/**
  * A sample component that demonstrates the new configuration manager
  */
public class SampleComponent implements Configurable {
    
    // javadoc comments omitted for now
    public static String PROP_WIDTH = "width";
    public static int PROP_WIDTH_DEFAULT = 10;
    public static String PROP_HEIGHT = "height";
    public static int PROP_HEIGHT_DEFAULT = 20;
    public static String PROP_DEPTH = "depth";
    public static float PROP_DEPTH_DEFAULT = 3.14f;
    public static String PROP_MSG = "message";
    public static String PROP_MSG_DEFAULT = "default";
    public static String PROP_MY_LIST = "myList";
    public static String PROP_SUBSAMPLES = "subsample";
    public static String PROP_SINGLE = "single";
    
    
    private String name;
    private int width;
    private int height;
    private float depth;
    private String msg;
    private List myList;
    private SampleComponent[] subsamples;
    private SimpleComponent simple;
    /*
     * (non-Javadoc)
     * Register all of the properties that we need
     * @see edu.cmu.sphinx.util.props.Configurable#register(edu.cmu.sphinx.util.props.Registry)
     */
    public void register(String name, Registry registry)
            throws PropertyException {
        this.name = name;
        registry.register(PROP_WIDTH, PropertyType.INT);
        registry.register(PROP_HEIGHT, PropertyType.INT);
        registry.register(PROP_DEPTH, PropertyType.FLOAT);
        registry.register(PROP_MSG, PropertyType.STRING);
        registry.register(PROP_MY_LIST, PropertyType.STRING_LIST);
        registry.register(PROP_SINGLE, PropertyType.COMPONENT);
        registry.register(PROP_SUBSAMPLES, PropertyType.COMPONENT_LIST);
    }
    /**
     * Returns the name
     * 
     * @return the name of this configurable
     */
    public String getName() {
        return name;
    }
    /**
     * Called when this component has new data
     * 
     * @param propertySheet
     *            contains the new data
     */
    public void newProperties(PropertySheet propertySheet) throws PropertyException {
        int tWidth = propertySheet.getInt(PROP_WIDTH, PROP_WIDTH_DEFAULT);
        int tHeight = propertySheet.getInt(PROP_HEIGHT, PROP_HEIGHT_DEFAULT);
        float tDepth = propertySheet.getFloat(PROP_DEPTH, PROP_DEPTH_DEFAULT);
        String tMsg = propertySheet.getString(PROP_MSG, PROP_MSG_DEFAULT);
        
        // validate width
        if (tWidth < 0 || tWidth > 100) {
            throw new PropertyException(this, PROP_WIDTH, "out of range");
        }
        // validate height
        if (tHeight < 0 || tHeight > 100) {
            throw new PropertyException(this, PROP_HEIGHT, "out of range");
        }
        // validate level
        if (tDepth < -10 || tDepth > 10) {
            throw new PropertyException(this, PROP_DEPTH,
                    "out of range -10 to 10");
        }
        // a list of strings
        myList = propertySheet.getStrings(PROP_MY_LIST);
        
        // a list of components of type SampleComponent
        
        List subsampleList = propertySheet.getComponentList(PROP_SUBSAMPLES,
                SampleComponent.class);
        subsamples = (SampleComponent[]) subsampleList
                .toArray(new SampleComponent[subsampleList.size()]);
        
        // A different type of component
        
        simple = (SimpleComponent) propertySheet.getComponent(PROP_SINGLE,
                SimpleComponent.class);
        
        // now that we've verified all of the data, we keep it
        
        width = tWidth;
        height = tHeight;
        depth = tDepth;
        msg = tMsg;
    }
    /**
     * Dumps this component and all of its underlings
     * 
     * @param level
     *            the nested level
     */
    public void dump(int level) {
        System.out.println(pad(level) + " ==== " + getName() + " =====");
        System.out.println(pad(level) + "width: " + width + " height: "
                + height + " depth: " + depth);
        System.out.println(pad(level) + "Msg: " + msg);
        for (int i = 0; i < myList.size(); i++) {
            System.out.println(pad(level) + " Mylist: " + myList.get(i));
        }
        System.out.println(pad(level) + " Subsamples: ");
        for (int i = 0; i < subsamples.length; i++) {
            subsamples[i].dump(level + 1);
        }
    }
    private String pad(int length) {
        return "                                      "
                .substring(0, length * 4);
    }
    /**
     * A test program
     * 
     * @param args
     */
    public static void main(String[] args) {
        // here's a minature property manipuator
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "Usage: SampleComponent URL compName");
        }
        try {
            URL url = new URL(args[0]);
            String compName = args[1];
            ConfigurationManager cm = new ConfigurationManager(url);
            SampleComponent sample = (SampleComponent) cm.lookup("sample");
            PropertySheet ps = cm.getPropertySheet(compName);
            // dump the property sheet
            System.out.println("Props for " + compName);
            System.out.println(ps);
            sample.dump(0);
            System.out.println("Here's the single one");
            System.out.println("Simple is " + sample.simple.getName());
            cm.save(new File("test.xml"));
        } catch (Exception e) {
            System.out.println("Trouble: " + e);
            e.printStackTrace();
        }
    }
}
