/**
 * Portions Copyright 2001 Sun Microsystems, Inc.
 * Portions Copyright 1999-2001 Language Technologies Institute, 
 * Carnegie Mellon University.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */
package edu.cmu.sphinx.alignment;

/**
 * Contains a parsed token from a Tokenizer.
 */
public class Token {

    private String token = null;
    private String whitespace = null;
    private String prepunctuation = null;
    private String postpunctuation = null;
    private int position = 0; // position in the original input text
    private int lineNumber = 0;

    /**
     * Returns the whitespace characters of this Token.
     * 
     * @return the whitespace characters of this Token; null if this Token does
     *         not use whitespace characters
     */
    public String getWhitespace() {
        return whitespace;
    }

    /**
     * Returns the prepunctuation characters of this Token.
     * 
     * @return the prepunctuation characters of this Token; null if this Token
     *         does not use prepunctuation characters
     */
    public String getPrepunctuation() {
        return prepunctuation;
    }

    /**
     * Returns the postpunctuation characters of this Token.
     * 
     * @return the postpunctuation characters of this Token; null if this Token
     *         does not use postpunctuation characters
     */
    public String getPostpunctuation() {
        return postpunctuation;
    }

    /**
     * Returns the position of this token in the original input text.
     * 
     * @return the position of this token in the original input text
     */
    public int getPosition() {
        return position;
    }

    /**
     * Returns the line of this token in the original text.
     * 
     * @return the line of this token in the original text
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Sets the whitespace characters of this Token.
     * 
     * @param whitespace the whitespace character for this token
     */
    public void setWhitespace(String whitespace) {
        this.whitespace = whitespace;
    }

    /**
     * Sets the prepunctuation characters of this Token.
     * 
     * @param prepunctuation the prepunctuation characters
     */
    public void setPrepunctuation(String prepunctuation) {
        this.prepunctuation = prepunctuation;
    }

    /**
     * Sets the postpunctuation characters of this Token.
     * 
     * @param postpunctuation the postpunctuation characters
     */
    public void setPostpunctuation(String postpunctuation) {
        this.postpunctuation = postpunctuation;
    }

    /**
     * Sets the position of the token in the original input text.
     * 
     * @param position the position of the input text
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Set the line of this token in the original text.
     * 
     * @param lineNumber the line of this token in the original text
     */
    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    /**
     * Returns the string associated with this token.
     * 
     * @return the token if it exists; otherwise null
     */
    public String getWord() {
        return token;
    }

    /**
     * Sets the string of this Token.
     * 
     * @param word the word for this token
     */
    public void setWord(String word) {
        token = word;
    }

    /**
     * Converts this token to a string.
     * 
     * @return the string representation of this object
     */
    public String toString() {
        StringBuffer fullToken = new StringBuffer();

        if (whitespace != null) {
            fullToken.append(whitespace);
        }
        if (prepunctuation != null) {
            fullToken.append(prepunctuation);
        }
        if (token != null) {
            fullToken.append(token);
        }
        if (postpunctuation != null) {
            fullToken.append(postpunctuation);
        }
        return fullToken.toString();
    }
}
