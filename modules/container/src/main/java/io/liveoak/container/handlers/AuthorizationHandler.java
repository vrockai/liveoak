package io.liveoak.container.handlers;

import io.liveoak.container.DefaultRequestContext;
import io.liveoak.container.ResourceErrorResponse;
import io.liveoak.container.ResourceRequest;
import io.liveoak.container.ResourceResponse;
import io.liveoak.security.impl.AuthServicesHolder;
import io.liveoak.security.impl.DefaultSecurityContext;
import io.liveoak.security.impl.SimpleLogger;
import io.liveoak.security.spi.AuthToken;
import io.liveoak.security.spi.AuthorizationRequestContext;
import io.liveoak.security.spi.AuthorizationService;
import io.liveoak.security.spi.TokenManager;
import io.liveoak.security.spi.TokenValidationException;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.SecurityContext;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * @author Bob McWhirter
 */
public class AuthorizationHandler implements HttpHandler {

    // TODO: replace with real logging
    private static final SimpleLogger log = new SimpleLogger(AuthorizationHandler.class);

    // TODO: Should be removed...
    static {
        try {
            AuthServicesHolder.getInstance().registerClassloader(AuthorizationHandler.class.getClassLoader());
            AuthServicesHolder.getInstance().registerDefaultPolicies();
        } catch (Throwable e) {
            log.error("Error occured during initialization of AuthorizationService", e);
            throw e;
        }
    }

    public AuthorizationHandler(HttpHandler next) {
        this.next = next;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        ResourceRequest req = exchange.getAttachment(ResourceRequest.ATTACHMENT_KEY);

        if (req == null) {
            this.next.handleRequest(exchange);
            return;
        }

        AuthToken token = null;
        AuthorizationService authService = AuthServicesHolder.getInstance().getAuthorizationService();
        TokenManager tokenManager = AuthServicesHolder.getInstance().getTokenManager();
        RequestContext reqContext = req.requestContext();

        boolean authorized = false;

        try {
            token = tokenManager.getAndValidateToken(reqContext);
            if (authService.isAuthorized(new AuthorizationRequestContext(token, reqContext))) {
                establishSecurityContext(token, reqContext);
                authorized = true;
            }
        } catch (TokenValidationException e) {
            String message = "Error when obtaining token: " + e.getMessage();
            log.warn(message);
            if (log.isTraceEnabled()) {
                log.trace(message, e);
            }
        }

        if (!authorized) {
            exchange.putAttachment(ResourceResponse.ATTACHMENT_KEY, new ResourceErrorResponse(req, ResourceErrorResponse.ErrorType.NOT_AUTHORIZED));
        }
        this.next.handleRequest(exchange);
    }

    protected void establishSecurityContext(AuthToken token, RequestContext reqContext) {
        // Looks like a hack...
        if (reqContext instanceof DefaultRequestContext) {
            SecurityContext securityContext = DefaultSecurityContext.createFromAuthToken(token);
            ((DefaultRequestContext) reqContext).setSecurityContext(securityContext);
        } else {
            log.warn("Can't establish securityContext to RequestContext " + reqContext);
        }
    }

    private HttpHandler next;
}
