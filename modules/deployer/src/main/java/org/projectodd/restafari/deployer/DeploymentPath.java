package org.projectodd.restafari.deployer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchService;

import static java.nio.file.FileSystems.*;

/**
 * Represents a deployment directory for mBaaS applications.
 *
 * @author Lance Ball lball@redhat.com
 */
public class DeploymentPath {

    private final WatchService watchService;

    /**
     * Create a new instance that watches the directory path provided for changes,
     * signalling the DeploymentController to PUT, POST or DELETE a {@linkplain DeploymentResource}
     *
     * @param dir The directory path on disk where deployments will be placed
     * @throws IOException if there is an error finding or reading dir
     */
    public DeploymentPath(String dir) throws IOException {
        watchService = getDefault().newWatchService();
        Path path = getDefault().getPath(dir);
    }
}
