package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jenkinsci.plugins.ParameterizedRemoteTrigger.BasicBuildContext;
import org.jenkinsci.plugins.tokenmacro.MacroEvaluationException;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;

public class TokenMacroUtils {

    public static String applyTokenMacroReplacements(String input, BasicBuildContext context) throws IOException {
        try {
            if (isUseTokenMacro(context)) {
                return TokenMacro.expandAll(context.run, context.workspace, context.listener, input);
            }
        } catch (MacroEvaluationException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        return input;
    }

    public static Map<String, String> applyTokenMacroReplacements(Map<String, String> input, BasicBuildContext context)
            throws IOException {

        Map<String, String> output = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : input.entrySet()) {
            output.put(entry.getKey(), applyTokenMacroReplacements(entry.getValue(), context));
        }
        return output;
    }

    public static boolean isUseTokenMacro(BasicBuildContext context) {
        return context != null && context.run != null && context.workspace != null && context.listener != null;
    }

}
