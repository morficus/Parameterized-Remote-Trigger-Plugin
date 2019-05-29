package org.jenkinsci.plugins.ParameterizedRemoteTrigger.utils;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

// Trust every server
public class NaiveTrustManager implements X509TrustManager {
    @Override
    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {}

    @Override
    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {}

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return null;
    }
}
