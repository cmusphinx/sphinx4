/*
 * Copyright 1999-2002 Carnegie Mellon University.  
 * Portions Copyright 2002 Sun Microsystems, Inc.  
 * Portions Copyright 2002 Mitsubishi Electric Research Laboratories.
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

package edu.cmu.sphinx.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * Provides a log formatter for use with sphinx. This formatter generates nicer looking console messages than the
 * default formatter. To use the formatter, set the property
 * <p/>
 * java.util.logging.ConsoleHandler.formatter to edu.cmu.sphinx.util.SphinxLogFormatter
 * <p/>
 * This is typically done in a custom loger.properties file
 */
public class SphinxLogFormatter extends Formatter {

    private static DateFormat DATE_FORMATTER = new SimpleDateFormat("hh:mm.SSS");
    private boolean terse;


    /**
     * Sets the level of output
     *
     * @param terse if true, the output level should be terse
     */
    public void setTerse(boolean terse) {
        this.terse = terse;
    }


    /**
     * Retrieves the level of output
     *
     * @return the level of output
     */
    public boolean getTerse() {
        return terse;
    }


    /**
     * Formats the given log record and return the formatted string.
     *
     * @param record the record to format
     * @return the formatted string
     */
    public String format(LogRecord record) {
        if (terse) {
            return record.getMessage() + "\n";
        } else {
            String date = DATE_FORMATTER.format(new Date(record.getMillis()));
            StringBuffer sbuf = new StringBuffer();
            sbuf.append(date);
            sbuf.append(" ");

            String loggerName = record.getLoggerName();
            String source;
            if (loggerName != null) {
                String[] strings = loggerName.split("[.]");
                source = strings[strings.length - 1];
            } else {
                source = loggerName;
            }

            sbuf.append(Utilities.pad(record.getLevel().getName() + " " + source, 24));
            sbuf.append("  ");
            sbuf.append(record.getMessage());
            sbuf.append("\n");
//            if (record.getLevel().equals(Level.WARNING) || record.getLevel().equals(Level.SEVERE)) {
//                sbuf.append("                   in ").append(record.getSourceClassName()).append(":").append(record.getSourceMethodName()).append("-").append(record.getLoggerName()).append("\n");
//            }
            return sbuf.toString();
        }
    }

}
