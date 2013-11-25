/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.responders;

import io.liveoak.container.ResourceErrorResponse;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ResourceResponse;
import io.liveoak.container.ResourceResponseSink;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.undertow.server.HttpServerExchange;

/**
 * @author Bob McWhirter
 */
public class BaseResponder implements Responder {

    public BaseResponder(ResourceRequest inReplyTo, ResourceResponseSink sink) {
        this.inReplyTo = inReplyTo;
        this.sink = sink;
    }

    BaseResponder createBaseResponder() {
        return new BaseResponder(this.inReplyTo, this.sink);
    }

    ResourceRequest inReplyTo() {
        return this.inReplyTo;
    }

    @Override
    public void resourceRead(Resource resource) {
        ResourceResponse response = new ResourceResponse(inReplyTo(), ResourceResponse.ResponseType.READ, resource);
        sink.accept( response );
    }

    @Override
    public void resourceCreated(Resource resource) {
        ResourceResponse response = new ResourceResponse(inReplyTo(), ResourceResponse.ResponseType.CREATED, resource);
        sink.accept( response );
    }

    @Override
    public void resourceDeleted(Resource resource) {
        ResourceResponse response = new ResourceResponse(inReplyTo(), ResourceResponse.ResponseType.DELETED, resource);
        sink.accept( response );
    }

    @Override
    public void resourceUpdated(Resource resource) {
        ResourceResponse response = new ResourceResponse(inReplyTo(), ResourceResponse.ResponseType.UPDATED, resource);
        sink.accept( response );
    }

    @Override
    public void createNotSupported(Resource resource) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.CREATE_NOT_SUPPORTED);
        sink.accept( response );
    }

    @Override
    public void readNotSupported(Resource resource) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.READ_NOT_SUPPORTED);
        sink.accept( response );
    }

    @Override
    public void updateNotSupported(Resource resource) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.UPDATE_NOT_SUPPORTED);
        sink.accept( response );
    }

    @Override
    public void deleteNotSupported(Resource resource) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.DELETE_NOT_SUPPORTED);
        sink.accept( response );
    }

    @Override
    public void noSuchResource(String id) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.NO_SUCH_RESOURCE);
        sink.accept( response );
    }

    @Override
    public void resourceAlreadyExists(String id) {
        ResourceErrorResponse response = new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.RESOURCE_ALREADY_EXISTS);
        sink.accept( response );
    }

    @Override
    public void internalError(String message) {
        ResourceErrorResponse response =  new ResourceErrorResponse(inReplyTo(), ResourceErrorResponse.ErrorType.INTERNAL_ERROR);
        sink.accept( response );
    }

    private final ResourceRequest inReplyTo;
    protected final ResourceResponseSink sink;
}
