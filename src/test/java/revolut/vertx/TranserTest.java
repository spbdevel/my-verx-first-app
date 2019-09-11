package revolut.vertx;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;
import revolut.vertx.account.Account;
import revolut.vertx.transfer.Transfer;


public class TranserTest extends BaseTest {

    @Test
    public void checkAddTransfer(TestContext context) {
        Async async = context.async();
        Transfer transfer = new Transfer();
        transfer.setSum(100);
        Long millis = System.currentTimeMillis();
        transfer.setMillis(millis);
        transfer.setFromId(1);
        transfer.setToId(2);
        String json = Json.encodePrettily(transfer);
        vertx.createHttpClient().post(port, "localhost", "/api/transfer")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        final Transfer res = Json.decodeValue(body.toString(), Transfer.class);
                        context.assertEquals(res.getFromId(), 1);
                        context.assertEquals(res.getToId(), 2);
                        context.assertEquals(res.getMillis(), millis);
                        context.assertEquals(res.getSum(), 100);
                        context.assertNotNull(res.getId());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }

}
