package io.liveoak.container.handlers;

import java.io.InputStream;

import io.liveoak.container.DefaultResourceParams;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ReturnFieldsImpl;
import io.liveoak.container.codec.MediaTypeMatcher;
import io.liveoak.container.codec.ResourceCodecManager;
import io.liveoak.security.impl.AuthConstants;
import io.liveoak.spi.MediaType;
import io.liveoak.spi.Pagination;
import io.liveoak.spi.RequestType;
import io.liveoak.spi.ResourceParams;
import io.liveoak.spi.ResourcePath;
import io.liveoak.spi.ReturnFields;
import io.liveoak.spi.Sorting;
import io.liveoak.spi.state.ResourceState;
import io.netty.handler.codec.http.HttpMethod;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.xnio.channels.StreamSourceChannel;

/**
 * @author Bob McWhirter
 */
public class ResourceRequestDecodingHandler implements HttpHandler {

    public ResourceRequestDecodingHandler(HttpHandler next, ResourceCodecManager codecManager) {
        this.next = next;
        this.codecManager = codecManager;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }
        try {
            exchange.startBlocking();

            String path = exchange.getRequestPath();

            int lastDotLoc = path.lastIndexOf('.');

            String extension = null;

            if (lastDotLoc > 0) {
                extension = path.substring(lastDotLoc + 1);
            }

            HeaderValues acceptHeaderValues = exchange.getRequestHeaders().get( "Accept" );

            String acceptHeader = "application/json";
            if (acceptHeaderValues != null ) {
                acceptHeader = acceptHeaderValues.getFirst();
            }
            MediaTypeMatcher mediaTypeMatcher = new MediaTypeMatcher(acceptHeader, extension);

            String authToken = getAuthorizationToken(exchange);

            ResourceParams params = DefaultResourceParams.instance(exchange.getQueryParameters());

            String method = exchange.getRequestMethod().toString();

            System.err.println( "EXCHANGE: " + exchange );

            if ("POST".equals(method)) {
                HeaderValues contentTypeHeader = exchange.getRequestHeaders().get("Content-Type");
                MediaType contentType = new MediaType(contentTypeHeader.getFirst());
                ResourceRequest request = new ResourceRequest.Builder(RequestType.CREATE, new ResourcePath(path))
                        .resourceParams(params)
                        .mediaTypeMatcher(mediaTypeMatcher)
                        .requestAttribute(AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken)
                        .resourceState(decodeState(contentType, exchange.getInputStream()))
                        .build();
                exchange.putAttachment(ResourceRequest.ATTACHMENT_KEY, request);
            } else if ("GET".equals(method)) {
                ResourceRequest request = new ResourceRequest.Builder(RequestType.READ, new ResourcePath(path))
                        .resourceParams(params)
                        .mediaTypeMatcher(mediaTypeMatcher)
                        .requestAttribute(AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken)
                        .pagination(decodePagination(params))
                        .returnFields(decodeReturnFields(params))
                        .sorting(decodeSorting(params))
                        .build();
                exchange.putAttachment(ResourceRequest.ATTACHMENT_KEY, request);
            } else if ("PUT".equals(method)) {
                HeaderValues contentTypeHeader = exchange.getRequestHeaders().get("Content-Type");
                MediaType contentType = new MediaType(contentTypeHeader.getFirst());
                ResourceRequest request = new ResourceRequest.Builder(RequestType.UPDATE, new ResourcePath(path))
                        .resourceParams(params)
                        .mediaTypeMatcher(mediaTypeMatcher)
                        .requestAttribute(AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken)
                        .resourceState(decodeState(contentType, exchange.getInputStream()))
                        .build();
                exchange.putAttachment(ResourceRequest.ATTACHMENT_KEY, request);
            } else if ("DELETE".equals(method)) {
                ResourceRequest request = new ResourceRequest.Builder(RequestType.DELETE, new ResourcePath(path))
                        .resourceParams(params)
                        .mediaTypeMatcher(mediaTypeMatcher)
                        .requestAttribute(AuthConstants.ATTR_AUTHORIZATION_TOKEN, authToken)
                        .build();
                exchange.putAttachment(ResourceRequest.ATTACHMENT_KEY, request);
            }

            this.next.handleRequest(exchange);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    protected String getAuthorizationToken(HttpServerExchange exchange) {
        HeaderValues authorizationHeaders = exchange.getRequestHeaders().get("Authorization");
        if (authorizationHeaders == null) {
            return null;
        }

        String[] authorization = authorizationHeaders.getFirst().split(" ");
        if (authorization.length != 2 || !authorization[0].equalsIgnoreCase("Bearer")) {
            System.err.println("Authorization header is invalid or it's of different type than 'Bearer'. Ignoring");
            return null;
        } else {
            return authorization[1];
        }
    }


    private ReturnFields decodeReturnFields(ResourceParams params) {
        String fieldsValue = params.value("fields");
        ReturnFieldsImpl returnFields = null;
        if (fieldsValue != null && !"".equals(fieldsValue)) {
            returnFields = new ReturnFieldsImpl(fieldsValue);
        } else {
            returnFields = new ReturnFieldsImpl("*");
        }

        String expandValue = params.value("expand");

        if (expandValue != null && !"".equals(expandValue)) {
            returnFields = returnFields.withExpand(expandValue);
        }

        return returnFields;
    }

    protected ResourceState decodeState(MediaType mediaType, InputStream content) throws Exception {
        return codecManager.decode(mediaType, content);
    }

    protected Pagination decodePagination(ResourceParams params) {
        int offset = limit(params.intValue("offset", 0), 0, Integer.MAX_VALUE);
        int limit = limit(params.intValue("limit", Pagination.DEFAULT_LIMIT), 0, Pagination.MAX_LIMIT);

        return new Pagination() {
            public int offset() {
                return offset;
            }

            public int limit() {
                return limit;
            }
        };
    }

    protected Sorting decodeSorting(ResourceParams params) {
        String spec = params.value("sort");
        if (spec != null) {
            return new Sorting(spec);
        }
        return null;
    }

    private static int limit(int value, int lower, int upper) {
        if (value < lower) {
            return lower;
        } else if (value > upper) {
            return upper;
        }
        return value;
    }

    private HttpHandler next;
    private ResourceCodecManager codecManager;
}
