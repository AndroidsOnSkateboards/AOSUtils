package org.aosutils;

import java.util.ArrayList;

public class DebugUtils {
	public static ArrayList<String> getStackTrace(Exception e) {
		StackTraceElement[] stackTraceElements = e.getStackTrace();
		
		ArrayList<String> stackTrace = new ArrayList<String>();
		
		stackTrace.add(e.getClass().getName() + (e.getMessage() == null ? "" : ": " + e.getMessage()));
		stackTrace.add("");
		
		for (StackTraceElement stackTraceElement : stackTraceElements) {
			stackTrace.add(String.format("at %s.%s(%s:%s)", stackTraceElement.getClassName(), stackTraceElement.getMethodName(), stackTraceElement.getFileName(), stackTraceElement.getLineNumber()));
		}
		
		return stackTrace;
	}
}
