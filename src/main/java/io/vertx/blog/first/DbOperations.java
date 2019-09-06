package io.vertx.blog.first;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

public class DbOperations {



    void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS Whisky (id INTEGER IDENTITY, name varchar(100), origin varchar" +
                            "(100))",
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Whisky", select -> {
                            if (select.failed()) {
                                fut.fail(select.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insert(
                                        new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay"), connection,
                                        (v) -> insert(new Whisky("Talisker 57° North", "Scotland, Island"), connection,
                                                (r) -> {
                                                    next.handle(Future.<Void>succeededFuture());
                                                    connection.close();
                                                }));
                            } else {
                                next.handle(Future.<Void>succeededFuture());
                                connection.close();
                            }
                        });

                    });
        }
    }

    void insert(Whisky whisky, SQLConnection connection, Handler<AsyncResult<Whisky>> next) {
        String sql = "INSERT INTO Whisky (name, origin) VALUES ?, ?";
        connection.updateWithParams(sql,
                new JsonArray().add(whisky.getName()).add(whisky.getOrigin()),
                (ar) -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                        connection.close();
                        return;
                    }
                    UpdateResult result = ar.result();
                    // Build a new whisky instance with the generated id.
                    Whisky w = new Whisky(result.getKeys().getInteger(0), whisky.getName(), whisky.getOrigin());
                    next.handle(Future.succeededFuture(w));
                });
    }

    void select(String id, SQLConnection connection, Handler<AsyncResult<Whisky>> resultHandler) {
        connection.queryWithParams("SELECT * FROM Whisky WHERE id=?", new JsonArray().add(id), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture("Whisky not found"));
            } else {
                if (ar.result().getNumRows() >= 1) {
                    resultHandler.handle(Future.succeededFuture(new Whisky(ar.result().getRows().get(0))));
                } else {
                    resultHandler.handle(Future.failedFuture("Whisky not found"));
                }
            }
        });
    }

    void update(String id, JsonObject content, SQLConnection connection,
                        Handler<AsyncResult<Whisky>> resultHandler) {
        String sql = "UPDATE Whisky SET name=?, origin=? WHERE id=?";
        connection.updateWithParams(sql,
                new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id),
                update -> {
                    if (update.failed()) {
                        resultHandler.handle(Future.failedFuture("Cannot update the whisky"));
                        return;
                    }
                    if (update.result().getUpdated() == 0) {
                        resultHandler.handle(Future.failedFuture("Whisky not found"));
                        return;
                    }
                    resultHandler.handle(
                            Future.succeededFuture(new Whisky(Integer.valueOf(id),
                                    content.getString("name"), content.getString("origin"))));
                });
    }


}
