package org.braincopy.ws;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;

@ApplicationPath("app")
public class TLEApplication extends ResourceConfig {
    public TLEApplication() {
        packages("org.braincopy.ws");
    }
}