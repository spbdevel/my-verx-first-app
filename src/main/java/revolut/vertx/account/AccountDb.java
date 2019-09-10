package revolut.vertx.account;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;

import java.util.function.BiFunction;

public class AccountDb {



    public void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS Account (id INTEGER IDENTITY, num varchar(100), balance INTEGER)",
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM Account", select -> {
                            if (select.failed()) {
                                fut.fail(select.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insert(
                                        new Account("Some num", 1000), connection,
                                        (v) -> insert(new Account("Another num", 500000), connection,
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

    void insert(Account accnt, SQLConnection connection, Handler<AsyncResult<?>> next) {
        String sql = "INSERT INTO Account (num, balance) VALUES ?, ?";
        JsonArray add = new JsonArray().add(accnt.getNum()).add(accnt.getBalance());
        insert(sql, add, connection, next, this::createAccount);
    }

    private Account createAccount(UpdateResult result, JsonArray arr) {
        return new Account(result.getKeys().getInteger(0),
                arr.getString(0),
                arr.getInteger(1));
    }


    void  insert(String sql, JsonArray arr, SQLConnection connection, Handler<AsyncResult<?>> next,
                 BiFunction<UpdateResult, JsonArray, Object> fun) {
        connection.updateWithParams(sql,
                arr,
                (ar) -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                        connection.close();
                        return;
                    }
                    UpdateResult result = ar.result();

                    Object apply = fun.apply(result, arr);
                    next.handle(Future.succeededFuture(apply));
                });

    }



    void select(String id, SQLConnection connection, Handler<AsyncResult<Account>> resultHandler) {
        connection.queryWithParams("SELECT * FROM Account WHERE id=?", new JsonArray().add(id), ar -> {
            if (ar.failed()) {
                resultHandler.handle(Future.failedFuture("Account not found"));
            } else {
                if (ar.result().getNumRows() >= 1) {
                    resultHandler.handle(Future.succeededFuture(new Account(ar.result().getRows().get(0))));
                } else {
                    resultHandler.handle(Future.failedFuture("Account not found"));
                }
            }
        });
    }

    void update(String id, JsonObject content, SQLConnection connection,
                        Handler<AsyncResult<Account>> resultHandler) {
        String sql = "UPDATE Account SET name=?, origin=? WHERE id=?";
        connection.updateWithParams(sql,
                new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id),
                update -> {
                    if (update.failed()) {
                        resultHandler.handle(Future.failedFuture("Cannot update the Account"));
                        return;
                    }
                    if (update.result().getUpdated() == 0) {
                        resultHandler.handle(Future.failedFuture("Account not found"));
                        return;
                    }
                    resultHandler.handle(
                            Future.succeededFuture(new Account(Integer.valueOf(id),
                                    content.getString("num"), content.getInteger("balance"))));
                });
    }


}
