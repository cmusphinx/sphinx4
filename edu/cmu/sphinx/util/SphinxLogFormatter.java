/**
 * [[[copyright]]]
 */
package edu.cmu.sphinx.util;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


/**
 * Provides a log formatter for use with sphinx. This formatter
 * generates nicer looking console messages than the default
 * formatter.  To use the formatter, set the property
 *
 * 	java.util.logging.ConsoleHandler.formatter 
 * to 
 *	edu.cmu.sphinx.util.SphinxLogFormatter
 *
 * This is typically done in a custom loger.properties file
 */
public class SphinxLogFormatter extends Formatter {

    private static DateFormat DATE_FORMATTER = new
	SimpleDateFormat("hh:mm.SSS");

    /**
     * Formats the given log record and return the formatted string.
     *
     * @record the record to format
     * 
     * @return the formatted string
     */
    public String format(LogRecord record) {
	String date = DATE_FORMATTER.format(new Date(record.getMillis()) );
	StringBuffer sbuf = new StringBuffer();
	sbuf.append(date + " " +
		Utilities.pad(record.getLevel().getName(), 8) +
		record.getMessage() + "\n");
	if (record.getLevel().equals(Level.WARNING) ||
	    record.getLevel().equals(Level.SEVERE)) {
	    sbuf.append("                   in " 
		    + record.getSourceClassName() + ":"
		    + record.getSourceMethodName() + "\n");
	}
	return sbuf.toString();
    }
}
