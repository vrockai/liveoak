package io.liveoak.container.zero;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Function;

import io.liveoak.common.util.FileHelper;
import io.liveoak.common.util.StringPropertyReplacer;
import io.liveoak.container.tenancy.InternalApplication;
import io.liveoak.container.tenancy.InternalApplicationRegistry;
import io.liveoak.container.zero.git.GitHelper;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.resource.BlockingResource;
import io.liveoak.spi.resource.RootResource;
import io.liveoak.spi.resource.SynchronousResource;
import io.liveoak.spi.resource.async.Resource;
import io.liveoak.spi.resource.async.Responder;
import io.liveoak.spi.state.ResourceState;
import org.eclipse.jgit.api.Git;

/**
 * @author Ken Finnigan
 */
public class LocalApplicationsResource implements RootResource, SynchronousResource, BlockingResource {

    public LocalApplicationsResource(InternalApplicationRegistry applicationRegistry, File applicationsDirectory) {
        this.applicationRegistry = applicationRegistry;
        this.applicationsDirectory = applicationsDirectory;
    }

    public void parent(Resource parent) {
        this.parent = parent;
    }

    @Override
    public Resource parent() {
        return this.parent;
    }

    @Override
    public String id() {
        return "applications";
    }

    @Override
    public void createMember(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        File localDir;
        String localPath = (String) state.getProperty("localPath");

        // Ensure 'localPath' exists and points to a LiveOak application
        if (localPath == null || localPath.length() == 0) {
            responder.invalidRequest(String.format(INVALID_REQUEST_MESSAGE, localPath));
            return;
        } else {
            localPath = StringPropertyReplacer.replaceProperties(localPath, System.getProperties());
            localDir = new File(localPath);
            if (!localDir.exists()) {
                responder.invalidRequest(String.format(INVALID_REQUEST_MESSAGE, localPath));
                return;
            } else if (!(new File(localDir, "application.json")).exists()) {
                responder.invalidRequest(String.format(INVALID_REQUEST_MESSAGE, localPath));
                return;
            }
        }

        // Copy from 'localPath' to application path
        String id = state.id();
        File installDir = new File(this.applicationsDirectory, id);
        final String copyFromPath = localPath;
        final Git gitRepo = GitHelper.initRepo(installDir);

        try {
            FileHelper.copy(localDir, installDir, true, path -> path.getFileName().toString().equals(".git") ? true : false);
        } catch (IOException e) {
            responder.internalError(e);
        }

        try {
            InternalApplication app = this.applicationRegistry.createApplication(id, (String) state.getProperty("name"), installDir, d -> {
                try {
                    GitHelper.addAllAndCommit(gitRepo, ctx.securityContext().getUser(), "Import LiveOak application from: " + copyFromPath);
                    gitRepo.close();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            responder.resourceCreated(app.resource());
        } catch (Exception e) {
            responder.internalError(e);
        }
    }

    @Override
    public void readMember(RequestContext ctx, String id, Responder responder) throws Exception {
        responder.readNotSupported(this);
    }

    @Override
    public void updateProperties(RequestContext ctx, ResourceState state, Responder responder) throws Exception {
        responder.updateNotSupported(this);
    }

    @Override
    public void delete(RequestContext ctx, Responder responder) throws Exception {
        responder.deleteNotSupported(this);
    }

    private Resource parent;
    private final InternalApplicationRegistry applicationRegistry;
    private final File applicationsDirectory;

    private static final String INVALID_REQUEST_MESSAGE = "'localPath' must contain a valid path to a LiveOak application on the system. %s is invalid.";
}
