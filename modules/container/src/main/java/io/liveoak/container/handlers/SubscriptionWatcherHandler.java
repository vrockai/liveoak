package io.liveoak.container.handlers;

import io.liveoak.container.ResourceErrorResponse;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ResourceResponse;
import io.liveoak.container.codec.EncodingResult;
import io.liveoak.container.codec.IncompatibleMediaTypeException;
import io.liveoak.container.codec.MediaTypeMatcher;
import io.liveoak.container.codec.ResourceCodecManager;
import io.liveoak.container.subscriptions.SubscriptionManager;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.Resource;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.xnio.channels.StreamSinkChannel;

/**
 * @author Bob McWhirter
 */
public class SubscriptionWatcherHandler implements HttpHandler {

    public SubscriptionWatcherHandler(HttpHandler next, SubscriptionManager subscriptionManager) {
        this.next = next;
        this.subscriptionManager = subscriptionManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ResourceResponse response = exchange.getAttachment(ResourceResponse.ATTACHMENT_KEY );

        if ( response != null ) {
            switch (response.responseType()) {
                case CREATED:
                    this.subscriptionManager.resourceCreated( response.resource() );
                    break;
                case READ:
                    // ignore
                    break;
                case UPDATED:
                    this.subscriptionManager.resourceUpdated( response.resource() );
                    break;
                case DELETED:
                    this.subscriptionManager.resourceDeleted( response.resource() );
                    break;
                case ERROR:
                    // ignore
                    break;
            }
        }

        this.next.handleRequest(exchange);


    }

    private HttpHandler next;
    private SubscriptionManager subscriptionManager;
}
