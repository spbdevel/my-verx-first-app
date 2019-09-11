package revolut.vertx.account;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import revolut.vertx.base.DbBase;

public class AccountDb extends DbBase <Account> {


    public AccountDb() {
        super("SELECT * FROM Account",
                "CREATE TABLE IF NOT EXISTS Account (id INTEGER IDENTITY, num varchar(100), balance INTEGER NOT NULL)");
    }




    protected void insrt(Handler<AsyncResult<Void>> next, SQLConnection connection) {
        insert(
                new Account("Some num", 1000), connection,
                (v) -> insert(new Account("Another num", 500000), connection,
                        (r) -> {
                            next.handle(Future.succeededFuture());
                            connection.close();
                        }));
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




    void update(String id, JsonObject content, SQLConnection connection,
                       Handler<AsyncResult<Account>> resultHandler) {
        String sql = "UPDATE Account SET name=?, origin=? WHERE id=?";
        connection.updateWithParams(sql,
                new JsonArray().add(content.getString("name")).add(content.getString("origin")).add(id),
                update -> updt(id, content, resultHandler, update)
                );
    }


    public void select(String id, SQLConnection connection, Handler<AsyncResult<Account>> resultHandler) {
        connection.queryWithParams("SELECT * FROM Account WHERE id=?",
                new JsonArray().add(id), ar -> slct(resultHandler, ar));
    }


    protected Account initObject(AsyncResult<ResultSet> ar) {
        return new Account(ar.result().getRows().get(0));
    }


    protected Account initObject(String id, JsonObject content) {
        return new Account(Integer.valueOf(id),
                content.getString("num"), content.getInteger("balance"));
    }
}
