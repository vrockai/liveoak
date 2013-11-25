/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container;

import io.liveoak.spi.MediaType;
import io.liveoak.spi.state.ResourceState;
import io.liveoak.stomp.StompMessage;
import io.liveoak.stomp.client.StompClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.fest.assertions.Assertions.assertThat;

public class BasicServerTest {

    private DefaultContainer container;
    private ResourceServer server;

    protected CloseableHttpClient httpClient;

    @Before
    public void setUpServer() throws Exception {
        System.err.println( "*** SET UP SERVER" );
        this.container = new DefaultContainer();
        InMemoryDBResource resource = new InMemoryDBResource("memory");
        this.container.registerResource(resource, new SimpleConfig());

        this.server = ResourceServer.createDefaultResourceServer(this.container, "localhost" );
        this.server.start();
        System.err.println( "*** SET UP SERVER COMPLETE" );
    }

    @Before
    public void setUpClient() throws Exception {
        this.httpClient = HttpClientBuilder.create().build();
    }

    @After
    public void tearDownClient() throws Exception {
        this.httpClient.close();
    }

    @After
    public void tearDownServer() throws Exception {
        System.err.println( "*** TEAR DOWN SERVER" );
        this.server.stop();
        System.err.flush();
        System.err.println( "*** TEAR DOWN SERVER COMPLETE" );
    }

    protected ResourceState decode(HttpResponse response) throws Exception {
        return this.container.getCodecManager().decode(MediaType.JSON, response.getEntity().getContent());
    }

    protected ResourceState decode(ByteBuf buffer) throws Exception {
        return this.container.getCodecManager().decode(MediaType.JSON, new ByteBufInputStream( buffer ) );
    }

    @Test
    public void testServer() throws Exception {

        CompletableFuture<StompMessage> peopleCreationNotification = new CompletableFuture<>();
        CompletableFuture<StompMessage> bobCreationNotification = new CompletableFuture<>();


        System.err.println( "ADDING SUBSCRIPTION" );
        StompClient stompClient = new StompClient();
        stompClient.connect("localhost", 8675, (client) -> {
            // Subscribe only to the contents of /memory/people, and not the
            // memory/people itself plus contents.
            client.subscribe("/memory/people/*", (msg) -> {
                if (msg.headers().get("location").equals("/memory/people")) {
                    peopleCreationNotification.complete(msg);
                } else {
                    bobCreationNotification.complete(msg);
                }
            });
        });
        Thread.sleep( 200 );
        System.err.println( "DONE ADDING SUBSCRIPTION" );

        Header header = new BasicHeader("Accept", "application/json");

        HttpGet getRequest = null;
        HttpPost postRequest = null;
        HttpPut putRequest = null;
        CloseableHttpResponse response = null;

        System.err.println("TEST #1");
        // Root object should exist.
        getRequest = new HttpGet("http://localhost:8080/memory");
        getRequest.addHeader(header);

        response = this.httpClient.execute(getRequest);

        //  {
        //    "id" : "memory",
        //    "self" : {
        //      "href" : "/memory"
        //    },
        //   "content" : [ ]
        // }

        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);

        ResourceState state = decode(response);
        assertThat(state).isNotNull();
        assertThat(state.members().size()).isEqualTo(0);

        response.close();

        System.err.println("TEST #2");
        // people collection should not exist.

        getRequest = new HttpGet("http://localhost:8080/memory/people");

        response = this.httpClient.execute(getRequest);
        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(404);

        response.close();

        System.err.println("TEST #3");
        // create people collection with direct PUT

        putRequest = new HttpPut("http://localhost:8080/memory/people");
        putRequest.setEntity(new StringEntity("{ \"type\": \"collection\" }"));
        putRequest.setHeader("Content-Type", "application/json");
        response = this.httpClient.execute(putRequest);
        System.err.println("response: " + response);
        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(201);

        response.close();

        System.err.println("TEST #4");
        // people collection should exist now.

        getRequest = new HttpGet("http://localhost:8080/memory/people");

        response = this.httpClient.execute(getRequest);
        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);

        response.close();

        assertThat(peopleCreationNotification.getNow(null)).isNull();

        System.err.println("TEST #5");
        // people collection should be enumerable from the root


        getRequest = new HttpGet("http://localhost:8080/memory?expand=members");
        getRequest.addHeader(header);

        response = this.httpClient.execute(getRequest);

        //        {
        //          "id" : "memory",
        //          "self" : {
        //            "href" : "/memory"
        //          },
        //          "content" : [ {
        //            "id" : "people",
        //            "self" : {
        //                "href" : "/memory/people"
        //            },
        //            "content" : [ ]
        //          } ]
        //        }

        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(200);

        state = decode(response);
        assertThat(state).isNotNull();
        assertThat(state.members().size()).isEqualTo(1);

        ResourceState memoryCollection = state.members().get(0);
        assertThat(memoryCollection.getProperty("id")).isEqualTo("people");

        ResourceState selfObj = (ResourceState) memoryCollection.getProperty("self");
        assertThat(selfObj.getProperty("href")).isEqualTo("/memory/people");

        System.err.println("TEST #6");
        // Post a person

        postRequest = new HttpPost("http://localhost:8080/memory/people");
        postRequest.setEntity(new StringEntity("{ \"name\": \"bob\" }"));
        postRequest.setHeader("Content-Type", "application/json");

        response = httpClient.execute(postRequest);
        assertThat(response).isNotNull();
        assertThat(response.getStatusLine().getStatusCode()).isEqualTo(201);

        state = decode(response);
        assertThat(state).isNotNull();
        assertThat(state).isInstanceOf(ResourceState.class);

        assertThat(state.getProperty("id")).isNotNull();
        assertThat(state.getProperty("name")).isEqualTo("bob");

        // check STOMP
        System.err.println("TEST #STOMP");

        StompMessage obj = bobCreationNotification.get(5, TimeUnit.SECONDS);
        assertThat(obj).isNotNull();

        ResourceState bobObjState = (ResourceState) decode(obj.content());
        assertThat(bobObjState.getProperty("name")).isEqualTo("bob");

        assertThat(((ResourceState) state).getProperty("id")).isEqualTo(bobObjState.getProperty("id"));

        response.close();
        System.err.println("TEST #STOMP COMPLETE");
    }


}
