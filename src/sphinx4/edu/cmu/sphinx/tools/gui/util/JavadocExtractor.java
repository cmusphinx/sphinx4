/*
 * JavadocExtractor.java
 *
 * Created on February 6, 2007, 7:40 PM
 *
 * Portions Copyright 2007 Mitsubishi Electric Research Laboratories.
 * Portions Copyright 2007 Harvard Extension Schoool, Harvard University
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 */

package edu.cmu.sphinx.tools.gui.util;

/**
 * This is a helper class to retrieve the javadoc information of 
 * Sphinx classes
 *
 * @author Ariani
 */
public class JavadocExtractor {
    
    private static String _last_class;
    private static String _last_comment;
    
    /** execute the javadoc command to command line and get the result String */
    private static String extractJavadocComment(
            String classname, String class_path, String source_path )
    {
        
        if ( _last_class == null || ! _last_class.equals(classname)){
            try {                
                if( ! source_path.trim().endsWith("/")){ // add '/' at the end of path
                    source_path = source_path.concat("/");
                }
                String source_file = classname.replace('.', '/') + ".java"; // convert class name to file path
                String commandLine = "javadoc -docletpath \"" + class_path +
                    "\" -doclet edu.cmu.sphinx.tools.gui.util.DocletHelper \"" +
                    source_path.trim() + source_file.trim() + '\"';

                SysCommandExecutor cmdExecutor = SysCommandExecutor.getInstance(); 		   		
                cmdExecutor.runCommand(commandLine);

                // String cmdError = cmdExecutor.getCommandError();
                // for debugging: System.out.println(cmdOutput);
                // for debugging: System.err.println(cmdError);
                
                _last_comment = cmdExecutor.getCommandOutput(); 
                _last_class = classname;
            }catch(Exception e){
                   System.err.println("Exception "+e.getMessage());
            }
        }        
        return _last_comment;
    }
    
    /** 
     * do string manipulation to get the property comment
     * @param classname Fully qualified class name that holds the property
     * @param class_path Absolute path to the root of .class files;
     *              must end in '\' or '/'
     * @param source_path Absolute path to the source code root directory; 
     *              must end in '\' or '/'
     * @param prop_name Name of property to be searched
     * @return Javadoc comment for the property that belongs to class name
     */
    public static String getJavadocComment(String classname, String class_path,
            String source_path, String prop_name)
    {
        String allComment;
        
        if (source_path == null || source_path.trim().isEmpty()) { // no path to source code
            return null;
        }
        
        if ( _last_class == null || ! _last_class.trim().equals(classname.trim())){
            allComment = extractJavadocComment(classname,class_path,source_path);
        }
        else allComment = _last_comment;
        //  System.out.println("comment : " + allComment);        
        
        // do processing for the comment
        // one property comment block is defined by a '==='
        String[] comments = allComment.split("={3}"); 
        
         // index 0 is javadoc's result comment
        for (int i=1; i<comments.length;i++){
            //  System.out.println("comment" + i + " : " + comments[i]);
            String[] oneComment = comments[i].trim().split("={2}");
            if(oneComment[0].trim().equals(prop_name) && oneComment.length > 1){
                // property found, remove newlines in the comment string         
                oneComment[1] = oneComment[1].replaceAll("(\n|\r)+"," ");
                return oneComment[1]; 
            }
        }
        
        return null; // does not find the comment for the class property
    }
}
