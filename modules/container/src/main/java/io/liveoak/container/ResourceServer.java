/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container;

import io.liveoak.container.handlers.ServerHandler;
import io.undertow.Undertow;
import org.vertx.java.core.Vertx;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Base server capable of connecting a container to a network ports.
 *
 * @author Bob McWhirter
 */
public class ResourceServer {

    public static ResourceServer createDefaultResourceServer(DefaultContainer container, String host) throws UnknownHostException {
        ResourceServer server = new ResourceServer(container);
        server.addNetworkServer(new HttpServer(server, host, 8080));
        server.addNetworkServer(new StompServer(server, host, 8675));
        return server;
    }

    public ResourceServer(Vertx vertx) {
        this(new DefaultContainer(vertx));
    }

    public ResourceServer(DefaultContainer container) {
        this.container = container;
    }

    public void addNetworkServer(NetworkServer server) {
        this.servers.add(server);
    }

    /**
     * Synchronously start the network listener.
     *
     * @throws InterruptedException If interrupted before completely starting.
     */
    public void start() throws Exception {
        this.servers.forEach((s) -> {
            try {
                s.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Synchronously stop the network listener.
     *
     * @throws InterruptedException If interrupted before completely stopping.
     */
    public void stop() throws Exception {
        this.servers.forEach((s) -> {
            try {
                s.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public DefaultContainer container() {
        return this.container;
    }

    private DefaultContainer container;
    private List<NetworkServer> servers = new ArrayList<>();

}
