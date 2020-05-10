package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class Base64UtilsTest {

    @Test
    public void testGenAuthNoToken() throws Exception {
        String result = Base64Utils.generateAuthorizationHeaderValue(Base64Utils.AUTHTYPE_BASIC, "user", "$password", null, false);
        assertEquals("Basic dXNlcjokcGFzc3dvcmQ=", result);
    }

}