package revolut.vertx.transfer;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import revolut.vertx.base.DbBase;

public class TransferDb extends DbBase <Transfer> {


    public TransferDb() {
        super("SELECT * FROM Transfer",
                "CREATE TABLE IF NOT EXISTS Transfer (id INTEGER IDENTITY, toId INTEGER NOT NULL," +
                        " fromId INTEGER NOT NULL, millis BIGINT NOT NULL, summary integer NOT NULL)");
    }



    void insert(Transfer transfer, SQLConnection connection, Handler<AsyncResult<?>> next) {
        String sql = "INSERT INTO Transfer (fromId, toId, millis, summary) VALUES ?, ?, ?, ?";
        JsonArray add = new JsonArray().
                add(transfer.getFromId()).
                add(transfer.getToId()).
                add(transfer.getMillis()).
                add(transfer.getSum());
        insert(sql, add, connection, next, this::createTransfer);
    }

    private Transfer createTransfer(UpdateResult result, JsonArray arr) {
        return new Transfer(result.getKeys().getInteger(0),
                arr.getInteger(0),
                arr.getInteger(1),
                arr.getLong(2),
                arr.getInteger(3));
    }




    void update(String id, JsonObject content, SQLConnection connection,
                       Handler<AsyncResult<Transfer>> resultHandler) {
        String sql = "UPDATE Transfer SET fromId=?, toId=?, millis=?, summary=? WHERE id=?";
        connection.updateWithParams(sql,
                new JsonArray().add(content.getInteger("fromId")).
                        add(content.getInteger("toId")).
                        add(content.getLong("millis")).
                        add(content.getInteger("sum")).
                        add(id),
                update -> updt(id, content, resultHandler, update)
                );
    }


    void select(String id, SQLConnection connection, Handler<AsyncResult<Transfer>> resultHandler) {
        connection.queryWithParams("SELECT * FROM Transfer WHERE id=?",
                new JsonArray().add(id), ar -> slct(resultHandler, ar));
    }




    protected Transfer initObject(AsyncResult<ResultSet> ar) {
        return new Transfer(ar.result().getRows().get(0));
    }


    protected Transfer initObject(String id, JsonObject content) {
        return new Transfer(Integer.valueOf(id),
                content.getInteger("fromId"), content.getInteger("toId"),
                content.getLong("millis"), content.getInteger("summary"));
    }
}
