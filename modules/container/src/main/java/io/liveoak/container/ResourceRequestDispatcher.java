package io.liveoak.container;

import io.liveoak.container.responders.CreateResponder;
import io.liveoak.container.responders.DeleteResponder;
import io.liveoak.container.responders.ReadResponder;
import io.liveoak.container.responders.UpdateResponder;

/**
 * @author Bob McWhirter
 */
public class ResourceRequestDispatcher {

    public ResourceRequestDispatcher(DefaultContainer container) {
        this.container = container;
    }

    public void dispatch(ResourceRequest request, ResourceResponseSink sink) throws Exception {
        String firstSegment = request.resourcePath().head();

        switch (request.requestType()) {
            case CREATE:
                new CreateResponder(container.workerPool(), container, request, sink).doRead(firstSegment, container);
                break;
            case READ:
                new ReadResponder(container.workerPool(), container, request, sink).doRead(firstSegment, container);
                break;
            case UPDATE:
                new UpdateResponder(container.workerPool(), container, request, sink).doRead(firstSegment, container);
                break;
            case DELETE:
                new DeleteResponder(container.workerPool(), container, request, sink).doRead(firstSegment, container);
                break;
        }
    }

    private DefaultContainer container;
}
