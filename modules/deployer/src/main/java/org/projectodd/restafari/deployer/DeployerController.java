package org.projectodd.restafari.deployer;

import org.projectodd.restafari.spi.*;

import java.util.ArrayList;
import java.util.Collection;

/**
 * @author lball@redhat.com
 */
public class DeployerController implements ResourceController {
    @Override
    public void initialize(ControllerContext context) throws InitializationException {
        // Read controller context config and monitor a deployments directory
    }

    @Override
    public void destroy() {
        // Is this just undeploying a controller?
    }

    @Override
    public void getResource(RequestContext context, String collectionName, String id, Responder responder) {
        responder.resource(new DeploymentResource());
    }

    @Override
    public void getResources(RequestContext context, String collectionName, Pagination pagination, Responder responder) {
        // Return the current deployments as a set of resources
        Collection<Resource> resources = new ArrayList<Resource>();
        responder.resources(resources);
    }

    @Override
    public void createResource(RequestContext context, String collectionName, Resource resource, Responder responder) {
        // Deploy a new controller?
    }

    @Override
    public void updateResource(RequestContext context, String collectionName, String id, Resource resource, Responder responder) {
        // Update a controller's configuration?
    }

    @Override
    public void deleteResource(RequestContext context, String collectionName, String id, Responder responder) {
        // again - just undeploy the controller?
    }
}
