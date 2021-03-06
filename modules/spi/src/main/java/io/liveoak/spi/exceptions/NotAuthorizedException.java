/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.spi.exceptions;

import io.liveoak.spi.state.ResourceState;

/**
 * @author Bob McWhirter
 */
public class NotAuthorizedException extends ResourceException {

    public NotAuthorizedException(String path) {
        super(path, "Authentication required for '" + path + "'");
    }

    public NotAuthorizedException(String path, String message) {
        super(path, message);
    }

    public NotAuthorizedException(String path, ResourceState state) {
        super(path, state);
    }
}
