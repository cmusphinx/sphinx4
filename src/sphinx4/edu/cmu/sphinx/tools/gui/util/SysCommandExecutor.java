/*
 * SysCommandExecutor.java
 *
 * Created on February 5, 2007, 5:20 PM
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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
 
/**
 * This class is used to execute command line at from Java runtime
 *
 * Usage of following class ...
 * 
 * <P><PRE><CODE>
 * 		SysCommandExecutor cmdExecutor = SysCommandExecutor.getInstance(); 		
 * 		int exitStatus = cmdExecutor.runCommand(commandLine);
 * 
 * 		String cmdError = cmdExecutor.getCommandError();
 * 		String cmdOutput = cmdExecutor.getCommandOutput(); 
 * </CODE></PRE></P> 
 */
public class SysCommandExecutor
{	

	private String fWorkingDirectory;
	private List<EnvironmentVar> fEnvironmentVarList;
	
	private StringBuffer fCmdOutput;
	private StringBuffer fCmdError;
	private AsyncStreamReader fCmdOutputThread;
	private AsyncStreamReader fCmdErrorThread;
	
        /* this class uses Singleton pattern for the constructor */
        private SysCommandExecutor() {}
    
        private static class SysExecutorHolder {
            private static final SysCommandExecutor instance = new SysCommandExecutor();
        }
      
        /**
         * Get reference to the <code>SysCommandExecutor</code> 
         * 
         * @return <code>SysCommandExecutor</code>
         */
        public static SysCommandExecutor getInstance(){
            return SysExecutorHolder.instance;
        }
        
	public void setWorkingDirectory(String workingDirectory) {
		fWorkingDirectory = workingDirectory;
	}
	
	public void setEnvironmentVar(String name, String value)
	{
		if( fEnvironmentVarList == null )
			fEnvironmentVarList = new ArrayList<EnvironmentVar>();
		
		fEnvironmentVarList.add(new EnvironmentVar(name, value));
	}
	
        /** get the output of command */
	public String getCommandOutput() {		
		return fCmdOutput.toString();
	}
	
        /** get the error message after command execution */
	public String getCommandError() {
		return fCmdError.toString();
	}
	
        /** 
         * execute the command
         *
         * @param commandLine command
         * @return Exit status of command
         */
	public int runCommand(String commandLine) throws Exception
	{
		/* run command */
		Process process = runCommandHelper(commandLine);
		
		/* start output and error read threads */
		startOutputAndErrorReadThreads(process.getInputStream(), process.getErrorStream());
	    
		/* wait for command execution to terminate */
		int exitStatus = -1;
		try {
			exitStatus = process.waitFor();
					
		} catch (Throwable ex) {
			throw new Exception("SysCommandExecutor :" +ex.getMessage());
			
		} finally {
			/* notify output and error read threads to stop reading */
			notifyOutputAndErrorReadThreadsToStopReading();
		}
		
		return exitStatus;
	}	
	
	private Process runCommandHelper(String commandLine) throws IOException
	{
		Process process = null;		
		if( fWorkingDirectory == null )
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens());
		else
			process = Runtime.getRuntime().exec(commandLine, getEnvTokens(), new File(fWorkingDirectory));
		
		return process;
	}
	
	private void startOutputAndErrorReadThreads(InputStream processOut, InputStream processErr)
	{
		fCmdOutput = new StringBuffer();
		fCmdOutputThread = new AsyncStreamReader(processOut, fCmdOutput, "OUTPUT");		
		fCmdOutputThread.start();
		
		fCmdError = new StringBuffer();
		fCmdErrorThread = new AsyncStreamReader(processErr, fCmdError, "ERROR");
		fCmdErrorThread.start();
	}
	
	private void notifyOutputAndErrorReadThreadsToStopReading()
	{
		fCmdOutputThread.stopReading();
		fCmdErrorThread.stopReading();
	}
	
	private String[] getEnvTokens()
	{
		if( fEnvironmentVarList == null )
			return null;
		
		String[] envTokenArray = new String[fEnvironmentVarList.size()];
		Iterator<EnvironmentVar> envVarIter = fEnvironmentVarList.iterator();
		int nEnvVarIndex = 0; 
		while (envVarIter.hasNext())
		{
			EnvironmentVar envVar = (envVarIter.next());
			String envVarToken = envVar.fName + '=' + envVar.fValue;
			envTokenArray[nEnvVarIndex++] = envVarToken;
		}
		
		return envTokenArray;
	}	
}
 
class AsyncStreamReader extends Thread
{
	private final StringBuffer fBuffer;
	private final InputStream fInputStream;
	private boolean fStop;
	
	private final String fNewLine;
	
	public AsyncStreamReader(InputStream inputStream, StringBuffer buffer, String threadId)
	{
		fInputStream = inputStream;
		fBuffer = buffer;
		
		fNewLine = System.getProperty("line.separator");
	}	
	
	public String getBuffer() {		
		return fBuffer.toString();
	}
	
	@Override
    public void run()
	{
		try {
			readCommandOutput();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	private void readCommandOutput() throws IOException
	{		
		BufferedReader bufOut = new BufferedReader(new InputStreamReader(fInputStream));		
		String line = null;
		while ( (!fStop) && ((line = bufOut.readLine()) != null) )
		{
            fBuffer.append(line).append(fNewLine);
		}		
		bufOut.close();
	}
	
	public void stopReading() {
		fStop = true;
	}
}
 
class EnvironmentVar
{
	public final String fName;
	public final String fValue;
	
	public EnvironmentVar(String name, String value)
	{
		fName = name;
		fValue = value;
	}
}
 
