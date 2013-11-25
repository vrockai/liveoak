package io.liveoak.container;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.liveoak.stomp.common.StompFrameDecoder;
import io.liveoak.stomp.common.StompFrameEncoder;
import io.liveoak.stomp.common.StompMessageDecoder;
import io.liveoak.stomp.common.StompMessageEncoder;
import io.liveoak.stomp.server.StompServerContext;
import io.liveoak.stomp.server.protocol.ConnectHandler;
import io.liveoak.stomp.server.protocol.DisconnectHandler;
import io.liveoak.stomp.server.protocol.ReceiptHandler;
import io.liveoak.stomp.server.protocol.SendHandler;
import io.liveoak.stomp.server.protocol.SubscribeHandler;
import io.liveoak.stomp.server.protocol.UnsubscribeHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author Bob McWhirter
 */
public class StompServer implements NetworkServer {

    public StompServer(ResourceServer server, String host, int port) throws UnknownHostException {
        this(server, InetAddress.getByName(host), port);
    }

    public StompServer(ResourceServer server, InetAddress host, int port) {
        this.server = server;
        this.host = host;
        this.port = port;
        this.group = new NioEventLoopGroup();
    }

    @Override
    public void start() throws Exception {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .channel(NioServerSocketChannel.class)
                .group(this.group)
                .localAddress(this.host, this.port)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();

                        StompServerContext serverContext = new ContainerStompServerContext(server.container());

                        pipeline.addLast(new StompFrameDecoder());
                        pipeline.addLast(new StompFrameEncoder());
                        // handle frames
                        pipeline.addLast(new ConnectHandler(serverContext));
                        pipeline.addLast(new DisconnectHandler(serverContext));
                        pipeline.addLast(new SubscribeHandler(serverContext));
                        pipeline.addLast(new UnsubscribeHandler(serverContext));
                        // convert some frames to messages
                        pipeline.addLast(new ReceiptHandler());
                        pipeline.addLast(new StompMessageDecoder());
                        pipeline.addLast(new StompMessageEncoder(true));
                        // handle messages
                        pipeline.addLast(new SendHandler(serverContext));
                        // catch errors, return an ERROR message.
                        pipeline.addLast(new ErrorHandler());

                    }
                });
        ChannelFuture future = serverBootstrap.bind();
        future.sync();
    }

    @Override
    public void stop() throws Exception {
        this.group.shutdownGracefully().sync();
    }

    private ResourceServer server;
    private InetAddress host;
    private int port;
    private NioEventLoopGroup group;
}
