/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container.codec;

import io.liveoak.spi.state.ResourceState;
import io.netty.buffer.ByteBuf;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.io.InputStream;

public interface ResourceDecoder {
    ResourceState decode(InputStream in) throws IOException;
}
