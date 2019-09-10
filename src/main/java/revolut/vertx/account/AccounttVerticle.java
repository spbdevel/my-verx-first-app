package revolut.vertx.account;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This is a verticle. A verticle is a _Vert.x component_. This verticle is implemented in Java, but you can
 * implement them in JavaScript, Groovy or even Ruby.
 */
public class AccounttVerticle extends AbstractVerticle  {

  private Logger log = LoggerFactory.getLogger(AccounttVerticle.class);

  private JDBCClient jdbc;

  private AccountDb dbOperations = new AccountDb();


  private JsonObject config;



  private void addOne(RoutingContext routingContext) {
    final Account account = Json.decodeValue(routingContext.getBodyAsString(),
            Account.class);

    jdbc.getConnection(ar -> {
      // Read the request's content and create an instance of Account.
      SQLConnection connection = ar.result();
      dbOperations.insert(account, connection, (r) ->
          routingContext.response()
              .setStatusCode(201)
              .putHeader("content-type", "application/json; charset=utf-8")
              .end(Json.encodePrettily(r.result())));
          connection.close();
    });

  }

  private Account accnt(AsyncResult<ResultSet> ar) {
    return new Account(ar.result().getRows().get(0));
  }


  private void getOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    if (id == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      jdbc.getConnection(ar -> {
        // Read the request's content and create an instance of Account.
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

  private void updateOne(RoutingContext routingContext) {
    final String id = routingContext.request().getParam("id");
    JsonObject json = routingContext.getBodyAsJson();
    if (id == null || json == null) {
      routingContext.response().setStatusCode(400).end();
    } else {
      Handler<AsyncResult<SQLConnection>> asyncResultHandler = ar ->
              dbOperations.update(id, json, ar.result(), (account) -> {
                if (account.failed()) {
                  routingContext.response().setStatusCode(404).end();
                } else {
                  routingContext.response()
                          .putHeader("content-type", "application/json; charset=utf-8")
                          .end(Json.encodePrettily(account.result()));
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
        connection.execute("DELETE FROM Account WHERE id='" + id + "'",
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
