/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.liveoak.container.handlers.ServerHandler;
import io.undertow.Undertow;
import org.vertx.java.core.Vertx;

/**
 * Base server capable of connecting a container to a network ports.
 *
 * @author Bob McWhirter
 */
public class HttpServer implements NetworkServer {

    public HttpServer(ResourceServer server, String host, int port) throws UnknownHostException {
        this(server, InetAddress.getByName(host), port);
    }

    public HttpServer(ResourceServer server, InetAddress host, int port) {
        this.server = server;
        this.host = host;
        this.port = port;
    }


    /**
     * Synchronously start the network listener.
     *
     * @throws InterruptedException If interrupted before completely starting.
     */
    public void start() throws Exception {
        this.httpServer = Undertow.builder()
                .addListener(this.port, this.host.getHostName())
                .setHandler(new ServerHandler(container()))
                .build();

        this.httpServer.start();
    }

    /**
     * Synchronously stop the network listener.
     *
     * @throws InterruptedException If interrupted before completely stopping.
     */
    public void stop() throws Exception {
        System.err.println( "stopping undertwo" );
        this.httpServer.stop();
        System.err.println( "stopped undertwo" );
    }

    public DefaultContainer container() {
        return this.server.container();
    }

    private ResourceServer server;
    private int port;
    private InetAddress host;
    private Undertow httpServer;

}
