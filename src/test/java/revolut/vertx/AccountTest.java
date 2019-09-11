package revolut.vertx;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Assert;
import org.junit.Test;
import revolut.vertx.account.Account;

import static org.junit.Assert.fail;


public class AccountTest  extends BaseTest {

  @Test
  public void checkAddAccount(TestContext context) {
    Async async = context.async();
    final String json = Json.encodePrettily(new Account("accnt1", 10000));
    vertx.createHttpClient().post(port, "localhost", "/api/account")
        .putHeader("content-type", "application/json")
        .putHeader("content-length", Integer.toString(json.length()))
        .handler(response -> {
          context.assertEquals(response.statusCode(), 201);
          context.assertTrue(response.headers().get("content-type").contains("application/json"));
          response.bodyHandler(body -> {
            final Account account = Json.decodeValue(body.toString(), Account.class);
            context.assertEquals(account.getNum(), "accnt1");
            context.assertEquals(account.getBalance(), 10000);
            context.assertNotNull(account.getId());
            async.complete();
          });
        })
        .write(json)
        .end();
  }


  @Test
  public void checkAddAccount1(TestContext context) {
      try {
          new Account("accnt1", -10000);
          fail();
      } catch (Exception e) {
          Assert.assertTrue(true);
      }
  }

}
