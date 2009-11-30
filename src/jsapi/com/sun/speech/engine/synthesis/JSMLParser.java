/**
 * Copyright 1998-2001 Sun Microsystems, Inc.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package com.sun.speech.engine.synthesis;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import javax.speech.synthesis.JSMLException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Parses a JSML 0.6 document and returns a DOM.
 */
public class JSMLParser {
    private DocumentBuilder validatingDocumentBuilder;

    private DocumentBuilder nonvalidatingDocumentBuilder;

    /**
     * The DOM.
     */
    Document document;
    
    /**
     * Creates a new JSMLParser for the given JSML
     * text.  Parses the text immediately and return any errors.
     * The resulting DOM Document can be retrieved via
     * <code>getDocument</code>.
     * The optional validate will do validation of the JSML text.
     * This is typically not used since JSML doesn't require validation.
     *
     * @param jsmlText the JSML text
     * @param validate if <code>true</code>, validate the JSML text
     *
     * @see #getDocument
     *
     * @throws JSMLException if the JSML text contains errors
     */
    public JSMLParser(String jsmlText, boolean validate)
        throws JSMLException {

        // Handle case where text does not include a root element
        //
        if (!(jsmlText.substring(0,2).equals("<?"))) {
            jsmlText = "<jsml>\n" + jsmlText + "</jsml>\n";
        }

        try {
            document = parse(new InputSource(new StringReader(jsmlText)),
                             validate);
        } catch (IOException e) {
            throw new JSMLException("JSMLParser: " + e.getMessage());
        }
    }

    /**
     * Creates a new JSMLParser for the given <code>URL</code>.
     * Parses the text immediately and returns any errors.
     * The resulting DOM Document can be retrieved via
     * <code>getDocument</code>.
     * The optional validate will do validation of the JSML text.
     * This is typically not used since JSML doesn't require validation.
     *
     * @param jsmlSource the URL containing JSML text
     * @param validate if <code>true</code>, validate the JSML text
     *
     * @see #getDocument
     *
     * @throws JSMLException if the JSML text contains errors
     * @throws IOException if problems encountered with URL
     */
    public JSMLParser(URL jsmlSource, boolean validate) 
        throws JSMLException, IOException {
        final InputStream in = jsmlSource.openStream();
        final InputSource source = new InputSource(in);
        document = parse(source, validate);
    }

    /**
     * Gets the document for this parser.
     *
     * @return a DOM
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Lazy instantiation of the document builder.
     * @param validate <code>true</code> if the document builder should be
     *          validating.
     * @return document builder to use
     * @throws JSMLException
     *         error creating the document builder.
     */
    private DocumentBuilder getDocumentBuilder(boolean validate)
        throws JSMLException {
        if (validate && (validatingDocumentBuilder != null)) {
            return validatingDocumentBuilder;
        } else if (!validate && (nonvalidatingDocumentBuilder != null)) {
            return nonvalidatingDocumentBuilder;
        }
        final DocumentBuilderFactory dbf =
            DocumentBuilderFactory.newInstance();

        dbf.setValidating(validate);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(false);
        dbf.setCoalescing(true);
        dbf.setExpandEntityReferences(true);

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            if (validate) {
                validatingDocumentBuilder = db;
            } else {
                nonvalidatingDocumentBuilder = db;
            }
            return db;
        } catch (ParserConfigurationException e) {
            throw new JSMLException(
                "JSMLParser: " + e.getMessage());
        }
    }

    /**
     * Parses the source and optionally validates it.
     *
     * @param source the JSML text
     * @param validate if <code>true</code>, validate the JSML text
     *
     * @throws JSMLException if the JSML text contains errors
     * @throws IOException if problems encountered with <code>source</code>
     */
    protected Document parse(InputSource source, boolean validate)
        throws JSMLException, IOException {
        final DocumentBuilder db = getDocumentBuilder(validate);
        final Document doc;
        try {
            doc = db.parse(source);
        } catch (SAXException e) {
            throw new JSMLException("JSMLParser: " + e.getMessage());
        }
        return doc;
    }
}
