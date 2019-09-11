package revolut.vertx;

import revolut.vertx.transfer.TransferDb;
import revolut.vertx.transfer.TransferRoutes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import revolut.vertx.account.AccountDb;
import revolut.vertx.account.AccountRoutes;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TheVerticle extends AbstractVerticle {

    private Logger log = LoggerFactory.getLogger(TheVerticle.class);

    private JDBCClient jdbc;

    private AccountDb accountDb = new AccountDb();
    private TransferDb transferDb = new TransferDb();


    private JsonObject config;

    @Override
    public void start(Future<Void> fut) throws Exception {
        readConfig();
        // Create a JDBC client
        jdbc = JDBCClient.createShared(vertx, this.config, "DataCollection");
        log.info("jdbc", jdbc);

        //should be called last
        Handler<AsyncResult<Void>> lastHandler = (nthng) -> startWebApp(
                (http) -> completeStartup(http, fut)
        );


/*
        Handler<AsyncResult<Void>> handler = (nothing) ->
            startBackend(
                    (conn) -> {
                        dbOperations.createSomeData(conn,
                                lastHandler, fut
                        );
                    }, fut);
*/

        Handler<AsyncResult<Void>> accntHandler = (nothing) ->
                startBackend((connection) ->
                                accountDb.createSomeData(connection, lastHandler, fut)
                        , fut);


        startBackend((connection) -> {
            transferDb.createSomeData(connection, accntHandler, fut);
        }, fut);



    }

    private void readConfig() throws URISyntaxException, IOException {
        this.config = config();

        //for development run
        if (config.getString("url") == null) {
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

        AccountRoutes accountRoutes = new AccountRoutes(jdbc);
        router.get("/api/account").handler(accountRoutes::getAll);
        router.route("/api/account*").handler(BodyHandler.create());
        router.post("/api/account").handler(accountRoutes::addOne);
        router.get("/api/account/:id").handler(accountRoutes::getOne);
        router.put("/api/account/:id").handler(accountRoutes::updateOne);
        router.delete("/api/account/:id").handler(accountRoutes::deleteOne);

        TransferRoutes transferRoutes = new TransferRoutes(jdbc);
        router.get("/api/transfer").handler(transferRoutes::getAll);
        router.route("/api/transfer*").handler(BodyHandler.create());
        router.post("/api/transfer").handler(transferRoutes::addOne);
        router.get("/api/transfer/:id").handler(transferRoutes::getOne);
        router.put("/api/transfer/:id").handler(transferRoutes::updateOne);
        router.delete("/api/transfer/:id").handler(transferRoutes::deleteOne);

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


}
