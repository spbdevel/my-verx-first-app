package revolut.vertx;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import revolut.vertx.account.Account;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * This is our JUnit test for our verticle. The test uses vertx-unit, so we declare a custom runner.
 */
@RunWith(VertxUnitRunner.class)
public class BaseTest {

  protected Vertx vertx;
  protected Integer port;

  /**
   * Before executing our test, let's deploy our verticle.
   * <p/>
   * This method instantiates a new Vertx and deploy the verticle. Then, it waits in the verticle has successfully
   * completed its start sequence (thanks to `context.asyncAssertSuccess`).
   *
   * @param context the test context.
   */
  @Before
  public void setUp(TestContext context) throws IOException {
    vertx = Vertx.vertx();

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
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

  /**
   * This method, called after our test, just cleanup everything by closing the vert.x instance
   *
   * @param context the test context
   */
  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }


}
