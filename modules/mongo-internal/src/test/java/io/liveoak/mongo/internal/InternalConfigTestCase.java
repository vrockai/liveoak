package io.liveoak.mongo.internal;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.liveoak.spi.util.ObjectMapperFactory;
import io.liveoak.container.zero.extension.ZeroExtension;
import io.liveoak.mongo.config.BaseMongoConfigTest;
import io.liveoak.mongo.extension.MongoExtension;
import io.liveoak.mongo.internal.extension.MongoInternalExtension;
import io.liveoak.spi.RequestContext;
import io.liveoak.spi.exceptions.DeleteNotSupportedException;
import io.liveoak.spi.state.ResourceState;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

/**
 * @author <a href="mailto:mwringe@redhat.com">Matt Wringe</a>
 */
public class InternalConfigTestCase extends BaseMongoConfigTest  {

    static final String SYSTEM_CONFIG_PATH = "/" + ZeroExtension.APPLICATION_ID + "/system/mongo-internal/module";

    ResourceState original;

    @BeforeClass
    public static void loadExtensions() throws Exception {
        JsonNode configNode = ObjectMapperFactory.create().readTree(
                "{}");

        JsonNode instancesNode = ObjectMapperFactory.create().readTree(
                "{" +
                        "    foo: {servers: [{ host: 'localhost', port: 27018}]}," +
                        "    bar: {servers: [{ port: 27017}]}," +
                        "    baz: {}" +
                        "}");

        loadExtension("mongo", new MongoExtension(), (ObjectNode) configNode, (ObjectNode) instancesNode);

        JsonNode internalConfig = ObjectMapperFactory.create().readTree(
                "{  db: 'internal'," +
                        " servers: [{}]" +
                        "}"
        );
        loadExtension("mongo-internal", new MongoInternalExtension(), (ObjectNode)internalConfig);
    }

    @Before
    public void setup() throws Exception {
        original =  client.read(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH);
    }

    @After
    public void reset() throws Exception {
        client.update(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH, original);
    }

    @Test
    public void readConfigTest() throws Exception {
        ResourceState systemConfigState = client.read(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH);
        assertThat(systemConfigState).isNotNull();
        assertThat(systemConfigState.getProperty("db")).isEqualTo("internal");

        ResourceState server = (ResourceState)systemConfigState.getProperty("servers", true, List.class).get(0);
        assertThat(server.getProperty("host")).isEqualTo("127.0.0.1");
        assertThat(server.getProperty("port")).isEqualTo(27017);
    }

    @Test
    public void editConfigTest() throws Exception {
        ResourceState systemConfigState = client.read(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH);
        assertThat(systemConfigState).isNotNull();
        assertThat(systemConfigState.getProperty("db")).isEqualTo("internal");

        systemConfigState.putProperty("db", "somethingElse");
        ResourceState updateResponse = client.update(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH, systemConfigState);
        assertThat(updateResponse.getProperty("db")).isEqualTo("somethingElse");

        ResourceState updatedRead = client.read(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH);
        assertThat(updatedRead.getProperty("db")).isEqualTo("somethingElse");
    }

    @Test
    public void deleteConfigTest() throws Exception {
        try {
            client.delete(new RequestContext.Builder().build(), SYSTEM_CONFIG_PATH);
            fail();
        } catch (DeleteNotSupportedException e) {
            //expected
        }
    }


}
