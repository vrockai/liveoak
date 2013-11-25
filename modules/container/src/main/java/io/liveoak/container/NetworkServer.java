package io.liveoak.container;

/**
 * @author Bob McWhirter
 */
public interface NetworkServer {

    void start() throws Exception;
    void stop() throws Exception;
}
