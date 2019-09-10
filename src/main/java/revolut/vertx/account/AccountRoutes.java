package revolut.vertx.account;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

public class AccountRoutes {

    private final JDBCClient jdbc;

    private final AccountDb dbOperations = new AccountDb();

    public AccountRoutes(JDBCClient jdbc) {
        this.jdbc = jdbc;
    }

    public void addOne(RoutingContext routingContext) {
        final Account whisky = Json.decodeValue(routingContext.getBodyAsString(),
                Account.class);

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

    public void getOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            jdbc.getConnection(ar -> {

                SQLConnection connection = ar.result();
                dbOperations.select(id, connection, result-> {
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

    public void updateOne(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        JsonObject json = routingContext.getBodyAsJson();
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar ->
                    dbOperations.update(id, json, ar.result(), (obj) -> {
                        if (obj.failed()) {
                            routingContext.response().setStatusCode(404).end();
                        } else {
                            routingContext.response()
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(Json.encodePrettily(obj.result()));
                        }
                        ar.result().close();
                    });
            jdbc.getConnection(asyncResultHandler);
        }
    }

    public void deleteOne(RoutingContext routingContext) {
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar -> {
                SQLConnection connection = ar.result();
                connection.execute("DELETE FROM Account WHERE id='" + id + "'",
                        result -> {
                            routingContext.response().setStatusCode(204).end();
                            connection.close();
                        });
            };
            jdbc.getConnection(asyncResultHandler);
        }
    }

    public void getAll(RoutingContext routingContext) {
        Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar -> {
            SQLConnection connection = ar.result();
            connection.query("SELECT * FROM Account", result -> {
                List<Account> accounts = result.result().getRows().stream().map(Account::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(accounts));
                connection.close();
            });
        };
        jdbc.getConnection(asyncResultHandler);
    }

}
