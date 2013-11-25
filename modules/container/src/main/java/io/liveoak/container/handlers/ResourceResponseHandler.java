package io.liveoak.container.handlers;

import io.liveoak.container.ResourceErrorResponse;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ResourceResponse;
import io.liveoak.container.codec.EncodingResult;
import io.liveoak.container.codec.IncompatibleMediaTypeException;
import io.liveoak.container.codec.MediaTypeMatcher;
import io.liveoak.container.codec.ResourceCodecManager;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.async.Resource;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.xnio.channels.StreamSinkChannel;

/**
 * @author Bob McWhirter
 */
public class ResourceResponseHandler implements HttpHandler {

    public ResourceResponseHandler(ResourceCodecManager codecManager) {
        this.codecManager = codecManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        ResourceRequest request = exchange.getAttachment(ResourceRequest.ATTACHMENT_KEY);
        ResourceResponse response = exchange.getAttachment(ResourceResponse.ATTACHMENT_KEY);

        int responseStatusCode = 0;
        String responseMessage = null;

        boolean shouldEncodeState = false;
        switch (response.responseType()) {
            case CREATED:
                responseStatusCode = HttpResponseStatus.CREATED.code();
                responseMessage = HttpResponseStatus.CREATED.reasonPhrase();
                shouldEncodeState = true;
                break;
            case READ:
                responseStatusCode = HttpResponseStatus.OK.code();
                responseMessage = HttpResponseStatus.OK.reasonPhrase();
                shouldEncodeState = true;
                break;
            case UPDATED:
                responseStatusCode = HttpResponseStatus.OK.code();
                responseMessage = HttpResponseStatus.OK.reasonPhrase();
                shouldEncodeState = true;
                break;
            case DELETED:
                responseStatusCode = HttpResponseStatus.OK.code();
                responseMessage = HttpResponseStatus.OK.reasonPhrase();
                shouldEncodeState = true;
                break;
            case ERROR:
                if (response instanceof ResourceErrorResponse) {
                    switch (((ResourceErrorResponse) response).errorType()) {
                        case NOT_AUTHORIZED:
                            responseStatusCode = HttpResponseStatus.FORBIDDEN.code();
                            responseMessage = HttpResponseStatus.FORBIDDEN.reasonPhrase();
                            break;
                        case NOT_ACCEPTABLE:
                            responseStatusCode = HttpResponseStatus.NOT_ACCEPTABLE.code();
                            responseMessage = HttpResponseStatus.NOT_ACCEPTABLE.reasonPhrase();
                            break;
                        case NO_SUCH_RESOURCE:
                            responseStatusCode = HttpResponseStatus.NOT_FOUND.code();
                            responseMessage = HttpResponseStatus.NOT_FOUND.reasonPhrase();
                            break;
                        case RESOURCE_ALREADY_EXISTS:
                            responseStatusCode = HttpResponseStatus.NOT_ACCEPTABLE.code();
                            responseMessage = HttpResponseStatus.NOT_ACCEPTABLE.reasonPhrase();
                            break;
                        case CREATE_NOT_SUPPORTED:
                            responseStatusCode = HttpResponseStatus.METHOD_NOT_ALLOWED.code();
                            responseMessage = "Create not supported";
                            break;
                        case READ_NOT_SUPPORTED:
                            responseStatusCode = HttpResponseStatus.METHOD_NOT_ALLOWED.code();
                            responseMessage = "Read not supported";
                            break;
                        case UPDATE_NOT_SUPPORTED:
                            responseStatusCode = HttpResponseStatus.METHOD_NOT_ALLOWED.code();
                            responseMessage = "Update not supported";
                            break;
                        case DELETE_NOT_SUPPORTED:
                            responseStatusCode = HttpResponseStatus.METHOD_NOT_ALLOWED.code();
                            responseMessage = "Delete not supported";
                            break;
                        case INTERNAL_ERROR:
                            responseStatusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code();
                            responseMessage = HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase();
                            break;
                    }
                }
                break;
        }

        HttpResponseStatus responseStatus = null;

        EncodingResult encodingResult = null;
        if (shouldEncodeState) {
            MediaTypeMatcher matcher = response.inReplyTo().mediaTypeMatcher();
            try {
                encodingResult = encodeState(request.requestContext(), matcher, response.resource());
            } catch (IncompatibleMediaTypeException e) {
                e.printStackTrace();
                //responseStatus = new HttpResponseStatus(HttpResponseStatus.NOT_ACCEPTABLE.code(), e.getMessage());
                exchange.setResponseCode(HttpResponseStatus.NOT_ACCEPTABLE.code());
                exchange.setResponseContentLength(0L);
                exchange.endExchange();
                return;
            } catch (Throwable e) {
                e.printStackTrace();
                //responseStatus = new HttpResponseStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase());
                //response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus);
                //response.headers().add(HttpHeaders.Names.CONTENT_LENGTH, 0);
                //out.add(response);
                exchange.setResponseCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
                exchange.setResponseContentLength(0);
                exchange.endExchange();
                return;
            }
        }

        //responseStatus = new HttpResponseStatus(responseStatusCode, responseMessage);

        try {
            exchange.setResponseCode(responseStatusCode);
            if (encodingResult != null) {
                ByteBuf content = encodingResult.encoded();
                exchange.setResponseContentLength(content.readableBytes());
                exchange.getResponseHeaders().put(new HttpString("Location"), response.resource().uri().toString());
                exchange.getResponseHeaders().put(new HttpString("Content-Type"), encodingResult.mediaType().toString());
                StreamSinkChannel channel = exchange.getResponseChannel();
                channel.writeFinal(content.nioBuffer());
            } else {
                exchange.setResponseContentLength(0);
            }
            exchange.endExchange();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected EncodingResult encodeState(RequestContext ctx, MediaTypeMatcher mediaTypeMatcher, Resource resource) throws Exception {
        return this.codecManager.encode(ctx, mediaTypeMatcher, resource);
    }

    private ResourceCodecManager codecManager;
}
