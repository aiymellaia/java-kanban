package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Epic;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public EpicsHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/epics".equals(path)) {
                List<Epic> epics = manager.getAllEpics();
                sendText(exchange, gson.toJson(epics));
                return;
            }

            if ("GET".equals(method) && path.matches("/epics/\\d+/subtasks")) {
                int epicId = extractIdFromPath(path);
                Epic epic = manager.getEpicById(epicId);

                if (epic == null) {
                    sendNotFound(exchange);
                    return;
                }

                sendText(exchange, gson.toJson(manager.getSubtasksOfEpic(epicId)));
                return;
            }

            if ("POST".equals(method) && "/epics".equals(path)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                Epic epic = gson.fromJson(body, Epic.class);

                if (epic.getId() != 0) {
                    sendNotFound(exchange);
                    return;
                }

                manager.createEpic(epic);
                sendText(exchange, gson.toJson(epic));
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/epics")) {
                String query = exchange.getRequestURI().getQuery();

                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));

                    if (manager.getEpicById(id) != null) {
                        manager.deleteEpicById(id);
                        sendText(exchange, "{\"status\":\"Epic deleted\"}");
                    } else {
                        sendNotFound(exchange);
                    }
                } else {
                    sendNotFound(exchange);
                }
                return;
            }

            sendNotFound(exchange);

        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }

    private int extractIdFromPath(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[2]);
    }
}
