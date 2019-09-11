package revolut.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public abstract class BaseTest {

  protected Vertx vertx;
  protected Integer port;

  @Before
  public void setUp(TestContext context) throws IOException {
    vertx = Vertx.vertx();

    ServerSocket socket = new ServerSocket(0);
    port = socket.getLocalPort();
    socket.close();

    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject()
            .put("http.port", port)
            .put("url", "jdbc:hsqldb:mem:test?shutdown=true")
            .put("driver_class", "org.hsqldb.jdbcDriver")
        );

    // We pass the options as the second parameter of the deployVerticle method.
    vertx.deployVerticle(TheVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }


}
