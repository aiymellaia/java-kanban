package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;

import java.io.IOException;

public class HistoryHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson = new Gson();

    public HistoryHandler(TaskManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                sendText(exchange, gson.toJson(manager.getHistory()));
                return;
            }

            sendNotFound(exchange);

        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }
}
