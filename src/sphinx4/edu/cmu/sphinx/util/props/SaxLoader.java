/*
 * 
 * Copyright 1999-2004 Carnegie Mellon University.  
 * Portions Copyright 2004 Sun Microsystems, Inc.  
 * Portions Copyright 2004 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package edu.cmu.sphinx.util.props;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
/**
 * Loads configuration from an XML file
 */
class SaxLoader {
    private URL url;
    private Map rpdMap;
    private Map globalProperties;
    /**
     * Creates a loader that will load from the given location
     * 
     * @param url
     *            the location to load
     * @param globalProperties
     *             the map of global properties
     */
    SaxLoader(URL url, Map globalProperties) {
        this.url = url;
        this.globalProperties = globalProperties;
    }
    /**
     * Loads a set of configuration data from the location
     * 
     * @return a map keyed by component name containing RawPropertyData objects
     * @throws IOException
     *             if an I/O or parse error occurs
     */
    Map load() throws IOException {
        rpdMap = new HashMap();
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            XMLReader xr = factory.newSAXParser().getXMLReader();
            ConfigHandler handler = new ConfigHandler();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            InputStream is = url.openStream();
            xr.parse(new InputSource(is));
            is.close();
        } catch (SAXParseException e) {
            String msg = "Error while parsing line " + e.getLineNumber()
                    + " of " + url + ": " + e.getMessage();
            throw new IOException(msg);
        } catch (SAXException e) {
            throw new IOException("Problem with XML: " + e);
        } catch (ParserConfigurationException e) {
            throw new IOException(e.getMessage());
        }
        return rpdMap;
    }
    
    /**
     * A SAX XML Handler implementation that builds up the map of raw property
     * data objects
     *  
     */
    class ConfigHandler extends DefaultHandler {
        RawPropertyData rpd = null;
        Locator locator;
        List itemList = null;
        String itemListName = null;
        StringBuffer curItem;
        
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        public void startElement(String uri, String localName, String qName,
                Attributes attributes) throws SAXException {
            if (qName.equals("config")) {
                // nothing to do
            } else if (qName.equals("component")) {
                String curComponent = attributes.getValue("name");
                String curType = attributes.getValue("type");
                if (rpdMap.get(curComponent) != null) {
                    throw new SAXParseException(
                        "duplicate definition for " + curComponent, locator);
                }
                rpd = new RawPropertyData(curComponent, curType);
            } else if (qName.equals("property")) {
                String name = attributes.getValue("name");
                String value = attributes.getValue("value");
                if (attributes.getLength() != 2 || name == null
                        || value == null) {
                    throw new SAXParseException(
                            "property element must only have "
                                    + "'name' and 'value' attributes", locator);
                }
                if (rpd == null) {
                    // we are not in a component so add this to the global
                    // set of symbols
                    String symbolName = "${" + name + "}";
                    globalProperties.put(symbolName, value);
                } else if (rpd.contains(name)) {
                    throw new SAXParseException("Duplicate property: " + name,
                            locator);
                } else {
                    rpd.add(name, value);
                }
            } else if (qName.equals("propertylist")) {
                itemListName = attributes.getValue("name");
                if (attributes.getLength() != 1 || itemListName == null) {
                    throw new SAXParseException("list element must only have "
                            + "the 'name'  attribute", locator);
                }
                itemList = new ArrayList();
            } else if (qName.equals("item")) {
                if (attributes.getLength() != 0) {
                    throw new SAXParseException("unknown 'item' attribute",
                            locator);
                }
                curItem = new StringBuffer();
            } else {
                throw new SAXParseException("Unknown element '" + qName + "'",
                        locator);
            }
        }
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#characters(char[], int, int)
         */
        public void characters(char ch[], int start, int length)
                throws SAXParseException {
            if (curItem != null) {
                curItem.append(ch, start, length);
            } 
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(String uri, String localName, String qName)
                throws SAXParseException {
            if (qName.equals("component")) {
                rpdMap.put(rpd.getName(), rpd);
                rpd = null;
            } else if (qName.equals("property")) {
                // nothing to do
            } else if (qName.equals("propertylist")) {
                if (rpd.contains(itemListName)) {
                    throw new SAXParseException("Duplicate property: "
                            + itemListName, locator);
                } else {
                    rpd.add(itemListName, itemList);
                    itemList = null;
                }
            } else if (qName.equals("item")) {
                itemList.add(curItem.toString().trim());
                curItem = null;
            }
        }
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
         */
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }
    }
}
