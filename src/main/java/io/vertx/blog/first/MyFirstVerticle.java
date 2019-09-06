package io.vertx.blog.first;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but you can
 * implement them in JavaScript, Groovy or even Ruby.
 */
public class MyFirstVerticle extends AbstractVerticle  {

  private Logger log = LoggerFactory.getLogger(MyFirstVerticle.class);

  private JDBCClient jdbc;

  private DbOperations dbOperations = new DbOperations();


  private JsonObject config;

  /**
   * This method is called when the verticle is deployed. It creates a HTTP server and registers a simple request
   * handler.
   * <p/>
   * Notice the `listen` method. It passes a lambda checking the port binding result. When the HTTP server has been
   * bound on the port, it call the `complete` method to inform that the starting has completed. Else it reports the
   * error.
   *
   * @param fut the future
   */
  @Override
  public void start(Future<Void> fut)  throws Exception {
    readConfig();
    // Create a JDBC client
    jdbc = JDBCClient.createShared(vertx, this.config, "My-Whisky-Collection");
    log.info("jdbc", jdbc);
    startBackend(
        (connection) -> dbOperations.createSomeData(connection,
            (nothing) -> startWebApp(
                (http) -> completeStartup(http, fut)
            ), fut
        ), fut);
  }

  private void readConfig() throws URISyntaxException, IOException {
    this.config = config();

    //for development run
    if(config.getString("url") == null) {
      URL resource = getClass().getClassLoader().getResource("my-it-config.json");
      URI uri = resource.toURI();
      Path path = Paths.get(uri);
      String s = new String(Files.readAllBytes(path));
      this.config = new JsonObject(s);
    }
  }

  private void startBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> fut) {
    Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar -> {
      if (ar.failed()) {
        log.info("jdbc failed");
        fut.fail(ar.cause());
      } else {
        log.info("jdbc connection done");
        next.handle(Future.succeededFuture(ar.result()));
      }
    };
    jdbc.getConnection(asyncResultHandler);
  }

  private void startWebApp(Handler<AsyncResult<HttpServer>> next) {
    // Create a router object.
    Router router = Router.router(vertx);

    log.info("start web");

    // Bind "/" to our hello message.
    router.route("/").handler(routingContext -> {
      HttpServerResponse response = routingContext.response();
      response
          .putHeader("content-type", "text/html")
          .end("<h1>Hello from my first Vert.x 3 application</h1>");
    });

    router.route("/assets/*").handler(StaticHandler.create("assets"));

    router.get("/api/whiskies").handler(this::getAll);
    router.route("/api/whiskies*").handler(BodyHandler.create());
    router.post("/api/whiskies").handler(this::addOne);
    router.get("/api/whiskies/:id").handler(this::getOne);
    router.put("/api/whiskies/:id").handler(this::updateOne);
    router.delete("/api/whiskies/:id").handler(this::deleteOne);

      log.info("creating server, port {}", config.getInteger("http.port", 8080));

      // Create the HTTP server and pass the "accept" method to the request handler.
      vertx
          .createHttpServer()
          .requestHandler(router::accept)
          .listen(
              // Retrieve the port from the configuration,
              // default to 8080.
              config.getInteger("http.port", 8080),
              next::handle
          );
  }

  private void completeStartup(AsyncResult<HttpServer> http, Future<Void> fut) {
    if (http.succeeded()) {
      fut.complete();
    } else {
      fut.fail(http.cause());
    }
  }


  @Override
  public void stop() throws Exception {
    // Close the JDBC client.
    jdbc.close();
  }

  private void addOne(RoutingContext routingContext) {
    final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(),
            Whisky.class);

    jdbc.getConnection(ar -> {
      // Read the request's content and create an instance of Whisky.
      SQLConnection connection = ar.result();
      dbOperations.insert(whisky, connection, (r) ->
          routingContext.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(r.result())));
          connection.close();
    });

  }

  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      jdbc.getConnection(ar -> {
        // Read the request's content and create an instance of Whisky.
        SQLConnection connection = ar.result();
        dbOperations.select(id, connection, result -> {
          if (result.succeeded()) {
            routingContext.response()
                .setStatusCode(200)
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result.result()));
          } else {
            routingContext.response()
                .setStatusCode(404).end();
          }
          connection.close();
        });
      });
    }
  }

  private void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar ->
              dbOperations.update(id, json, ar.result(), (whisky) -> {
                if (whisky.failed()) {
                  routingContext.response().setStatusCode(404).end();
                } else {
                  routingContext.response()
                          .putHeader("content-type", "application/json; charset=utf-8")
                          .end(Json.encodePrettily(whisky.result()));
                }
                ar.result().close();
              });
      jdbc.getConnection(asyncResultHandler);
    }
  }

  private void deleteOne(RoutingContext routingContext) {
    String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar -> {
        SQLConnection connection = ar.result();
        connection.execute("DELETE FROM Whisky WHERE id='" + id + "'",
                result -> {
                  routingContext.response().setStatusCode(204).end();
                  connection.close();
                });
      };
      jdbc.getConnection(asyncResultHandler);
    }
  }

  private void getAll(RoutingContext routingContext) {
    Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar -> {
      SQLConnection connection = ar.result();
      connection.query("SELECT * FROM Whisky", result -> {
        List<Whisky> whiskies = result.result().getRows().stream().map(Whisky::new).collect(Collectors.toList());
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(whiskies));
        connection.close();
      });
    };
    jdbc.getConnection(asyncResultHandler);
  }

}
