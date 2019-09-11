package revolut.vertx.transfer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;
import revolut.vertx.account.Account;
import revolut.vertx.account.AccountDb;

import java.util.List;
import java.util.stream.Collectors;

public class TransferRoutes {

    private final JDBCClient jdbc;

    private final TransferDb dbOperations = new TransferDb();
    private final AccountDb accountDb = new AccountDb();

    public TransferRoutes(JDBCClient jdbc) {
        this.jdbc = jdbc;
    }

    public void addOne(RoutingContext routingContext) {
        final Transfer  transfer = Json.decodeValue(routingContext.getBodyAsString(),
                Transfer .class);

        //validate accounts existfirst
        int fromId = transfer.getFromId();
        int toId = transfer.getToId();

        Handler<AsyncResult<?>> transferHandler = (r) -> {
            jdbc.getConnection(ar -> {
                SQLConnection connection = ar.result();

                dbOperations.insert(transfer, connection, (r1) ->
                        routingContext.response()
                                .setStatusCode(201)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(r1.result())));
                connection.close();
            });
        };


        jdbc.getConnection(ar -> {
            SQLConnection connection = ar.result();
            Handler<AsyncResult<Account>> bothExistHandler = result -> {
                if (result.succeeded()) {
                    transferHandler.handle(result);
                } else {
                    routingContext.fail(result.cause());
                }
                connection.close();
            };

            Handler<AsyncResult<Account>> toExistHandler = result -> {
                if (result.succeeded()) {
                    accountDb.select(String.valueOf(toId), connection, bothExistHandler);
                } else {
                    routingContext.fail(result.cause());
                }
                connection.close();
            };
            accountDb.select(String.valueOf(fromId), connection, toExistHandler);
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
                connection.execute("DELETE FROM Transfer WHERE id='" + id + "'",
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
            connection.query("SELECT * FROM Transfer", result -> {
                List<Transfer> transfers = result.result().getRows().stream().map(Transfer::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(transfers));
                connection.close();
            });
        };
        jdbc.getConnection(asyncResultHandler);
    }

}
