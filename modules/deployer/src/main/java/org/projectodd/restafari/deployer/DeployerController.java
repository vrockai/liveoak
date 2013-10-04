package org.projectodd.restafari.deployer;

import org.projectodd.restafari.spi.*;

/**
 * @author: lball@redhat.com
 */
public class DeployerController implements ResourceController {
    @Override
    public void initialize(ControllerContext context) throws InitializationException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void destroy() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getResource(RequestContext context, String collectionName, String id, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void getResources(RequestContext context, String collectionName, Pagination pagination, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void createResource(RequestContext context, String collectionName, Resource resource, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void updateResource(RequestContext context, String collectionName, String id, Resource resource, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void deleteResource(RequestContext context, String collectionName, String id, Responder responder) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
