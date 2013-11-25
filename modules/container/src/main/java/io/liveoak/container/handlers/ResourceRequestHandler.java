package io.liveoak.container.handlers;

import io.liveoak.container.DefaultContainer;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ResourceRequestDispatcher;
import io.liveoak.container.ResourceResponse;
import io.liveoak.container.responders.CreateResponder;
import io.liveoak.container.responders.DeleteResponder;
import io.liveoak.container.responders.ReadResponder;
import io.liveoak.container.responders.UpdateResponder;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Bob McWhirter
 */
public class ResourceRequestHandler implements HttpHandler {


    public ResourceRequestHandler(HttpHandler next, DefaultContainer container) {
        this.next = next;
        this.container = container;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ResourceRequest request = exchange.getAttachment(ResourceRequest.ATTACHMENT_KEY);

        if (request == null) {
            this.next.handleRequest(exchange);
            return;
        }

        // If a prior handler already attached a response
        // do not process the request.
        // TODO put this logic in a superclass
        ResourceResponse priorResponse = exchange.getAttachment(ResourceResponse.ATTACHMENT_KEY);

        if (priorResponse != null) {
            this.next.handleRequest(exchange);
            return;
        }

        exchange.dispatch();
        new ResourceRequestDispatcher(this.container).dispatch(request, new UndertowResourceResponseSink( this.next, exchange ) );
    }

    private final HttpHandler next;
    private final DefaultContainer container;
}
