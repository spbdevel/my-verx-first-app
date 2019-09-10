package revolut.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;

import java.util.function.Consumer;

public class UtiltyMain {

    public static void main(String[] args) {
        VertxOptions options = new VertxOptions().setClustered(false);
        String dir = "src/main/java/";
        System.setProperty("vertx.cwd", dir);
        String verticleID = TheVerticle.class.getName();

        Consumer<Vertx> runner = vertx ->
        {
            try {
                vertx.deployVerticle(verticleID);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        };

        Vertx vertx = Vertx.vertx(options);
        runner.accept(vertx);
    }
}
