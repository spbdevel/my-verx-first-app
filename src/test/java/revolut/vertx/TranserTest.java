package revolut.vertx;

import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import org.junit.Test;
import revolut.vertx.transfer.Transfer;


public class TranserTest extends BaseTest {

    @Test
    public void checkAddTransfer(TestContext context) {
        Async async = context.async();
        int sum = 100;
        int fromId = 0;
        int toId = 1;
        Long millis = System.currentTimeMillis();
        Transfer transfer = new Transfer(fromId, toId, millis, sum);
        String json = Json.encodePrettily(transfer);
        vertx.createHttpClient().post(port, "localhost", "/api/transfer")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        final Transfer res = Json.decodeValue(body.toString(), Transfer.class);
                        context.assertEquals(res.getFromId(), fromId);
                        context.assertEquals(res.getToId(), toId);
                        context.assertEquals(res.getMillis(), millis);
                        context.assertEquals(res.getSum(), sum);
                        context.assertNotNull(res.getId());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }

    @Test
    public void checkAddWrongTransfer(TestContext context) {
        Async async = context.async();
        int sum = 100;
        int fromId = 1;
        int toId = 2;
        Long millis = System.currentTimeMillis();
        Transfer transfer = new Transfer(fromId, toId, millis, sum);
        String json = Json.encodePrettily(transfer);
        vertx.createHttpClient().post(port, "localhost", "/api/transfer")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", Integer.toString(json.length()))
                .handler(response -> {
                    context.assertNotEquals(response.statusCode(), 201);
                    context.assertEquals(response.statusCode(), 500);
                    async.complete();
                })
                .write(json)
                .end();
    }

}
