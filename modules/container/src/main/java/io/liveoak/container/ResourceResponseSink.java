package io.liveoak.container;

/**
 * @author Bob McWhirter
 */
public interface ResourceResponseSink {
    void accept(ResourceResponse response);
}
