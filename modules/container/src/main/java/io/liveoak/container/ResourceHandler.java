/*
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at http://www.eclipse.org/legal/epl-v10.html
 */
package io.liveoak.container;

import io.liveoak.container.responders.CreateResponder;
import io.liveoak.container.responders.DeleteResponder;
import io.liveoak.container.responders.ReadResponder;
import io.liveoak.container.responders.UpdateResponder;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ResourceHandler extends SimpleChannelInboundHandler<ResourceRequest> {

    public ResourceHandler(DefaultContainer container) {
        this.container = container;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ResourceRequest msg) throws Exception {
        new ResourceRequestDispatcher(this.container).dispatch(msg, (response) -> {
            ctx.writeAndFlush( response );
        });
    }

    private DefaultContainer container;

}
