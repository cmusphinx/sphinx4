package edu.cmu.sphinx.util.props;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;

/**
 * Handles configurations like the old one but is also able to process a new "include"-field
 *
 * @author Holger Brandl
 */
public class IncludingConfigHandler extends ConfigHandler {

    public IncludingConfigHandler(Map<String, RawPropertyData> rpdMap, GlobalProperties globalProperties) {
        super(rpdMap, globalProperties);
    }


    /* (non-Javadoc)
    * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
    */
    public void startElement(String uri, String localName, String qName,
                             Attributes attributes) throws SAXException {
        if (qName.equals("config")) {
            // nothing to do
        } else if (qName.equals("include")) {
            String includeFileName = attributes.getValue("file");

            try {
                URL fileURL = new File(includeFileName).toURI().toURL();
                SaxLoader saxLoader = new SaxLoader(fileURL, globalProperties, rpdMap);
                saxLoader.load();
            } catch (IOException e) {
                throw new RuntimeException("Error while processing <include file=\"" + includeFileName + "\">: " + e.toString(), e);
            }
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
//                    String symbolName = "${" + name + "}"; // why should we warp the global props here
                globalProperties.setValue(name, value);
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
            itemList = new ArrayList<String>();
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
}
