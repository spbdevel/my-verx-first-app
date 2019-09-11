package revolut.vertx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import revolut.vertx.account.Account;

import java.io.Serializable;
import java.util.function.BiFunction;

public abstract class DbBase <T extends Serializable>{
    final String SELECT_FROM;
    final String sql;

    public DbBase(String SELECT_FROM, String sql) {
        this.SELECT_FROM = SELECT_FROM;
        this.sql = sql;
    }

    public void createSomeData(AsyncResult<SQLConnection> result, Handler<AsyncResult<Void>> next, Future<Void> fut) {
        if (result.failed()) {
            fut.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
            connection.execute(
                    sql,
                    ar -> {
                        if (ar.failed()) {
                            fut.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query(SELECT_FROM, select -> {
                            if (select.failed()) {
                                fut.fail(select.cause());
                                connection.close();
                                return;
                            }
                            if (select.result().getNumRows() == 0) {
                                insrt(next, connection);
                            } else {
                                next.handle(Future.<Void>succeededFuture());
                                connection.close();
                            }
                        });

                    });
        }
    }

    protected void slct(Handler<AsyncResult<T>> resultHandler, AsyncResult<ResultSet> ar) {
        if (ar.failed()) {
            resultHandler.handle(Future.failedFuture("not found"));
        } else {
            if (ar.result().getNumRows() >= 1) {
                resultHandler.handle(Future.succeededFuture(initObject(ar)));
            } else {
                resultHandler.handle(Future.failedFuture("not found"));
            }
        }
    }

    protected void insrt(Handler<AsyncResult<Void>> next, SQLConnection connection) {
        next.handle(Future.succeededFuture());
        connection.close();
    }


    protected void  insert(String sql, JsonArray arr, SQLConnection connection, Handler<AsyncResult<?>> next,
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



    protected void updt(String id, JsonObject content, Handler<AsyncResult<T>> resultHandler, AsyncResult<UpdateResult> update) {
        if (update.failed()) {
            resultHandler.handle(Future.failedFuture("Cannot update"));
            return;
        }
        if (update.result().getUpdated() == 0) {
            resultHandler.handle(Future.failedFuture("not found"));
            return;
        }
        resultHandler.handle(
                Future.succeededFuture(initObject(id, content)));
    }


    abstract protected T initObject(AsyncResult<ResultSet> ar);

    abstract protected T initObject(String id, JsonObject conent);
}
