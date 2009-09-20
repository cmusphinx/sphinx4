package edu.cmu.sphinx.util.props;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** A SAX XML Handler implementation that builds up the map of raw property data objects */
class ConfigHandler extends DefaultHandler {

    protected RawPropertyData rpd = null;
    protected Locator locator;
    protected List<String> itemList = null;
    protected String itemListName = null;
    protected StringBuffer curItem;

    protected Map<String, RawPropertyData> rpdMap;
    protected GlobalProperties globalProperties;


    public ConfigHandler(Map<String, RawPropertyData> rpdMap, GlobalProperties globalProperties) {
        this.rpdMap = rpdMap;
        this.globalProperties = globalProperties;
    }


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
            throw new SAXParseException("Unknown element '" + qName + '\'',
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
