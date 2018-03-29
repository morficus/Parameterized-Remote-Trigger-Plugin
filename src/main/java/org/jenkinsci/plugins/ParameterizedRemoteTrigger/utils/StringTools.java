package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

public class StringTools
{
    
    /**
     * System specific new line character/string
     */
    public static final String NL = getSystemLineSeparator();

    private static String getSystemLineSeparator() {
        String newLine = System.getProperty("line.separator");
        if(newLine == null || newLine.length() <= 0) newLine = "\n";
        return newLine;
    }

}
