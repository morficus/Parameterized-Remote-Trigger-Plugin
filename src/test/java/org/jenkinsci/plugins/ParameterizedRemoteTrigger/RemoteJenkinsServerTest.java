package org.jenkinsci.plugins.ParameterizedRemoteTrigger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;


public class RemoteJenkinsServerTest {

    @Test
    public void payAttentionToCloneContract() throws Exception {
        RemoteJenkinsServer server = new RemoteJenkinsServer();
        server.setAddress("http://www.example.org:8443");
        server.setDisplayName("My example server.");
        server.setHasBuildTokenRootSupport(false);

        Object clone = server.clone();

        assertTrue(clone.equals(server));
        assertFalse(System.identityHashCode(server) == System.identityHashCode(clone));
    }
}
