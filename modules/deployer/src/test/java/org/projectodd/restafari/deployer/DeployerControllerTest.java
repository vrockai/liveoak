package org.projectodd.restafari.deployer;

import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.projectodd.restafari.container.Container;
import org.projectodd.restafari.container.SimpleConfig;
import org.projectodd.restafari.container.UnsecureServer;
import org.projectodd.restafari.deployer.DeployerController;

import java.net.InetAddress;

/**
 * @author <a href="mailto:lball@redhat.com">Lance Ball</a>
 */
public class DeployerControllerTest {

    private static SimpleConfig config;

    @BeforeClass
    public static void init() {
        config = new SimpleConfig();
    }

    @Test
    public void testDeployerController() throws Exception {
        Container container = new Container();
        container.registerResourceController("storage", new DeployerController(), config);

        UnsecureServer server = new UnsecureServer(container, InetAddress.getByName("localhost"), 8080, new NioEventLoopGroup());

        System.err.println("START SERVER");
        server.start();
        System.err.println("STARTED SERVER");

        CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet("http://localhost:8080/deployer/deployments");
        get.addHeader("Accept", "application/json");
        try {
            CloseableHttpResponse result = httpClient.execute(get);
        } finally {
            httpClient.close();
            server.stop();
        }
    }
}
