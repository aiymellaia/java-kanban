package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Epic;
import server.GsonAdapters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class EpicsHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public EpicsHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = GsonAdapters.createGson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            switch (method) {
                case "GET" -> handleGet(exchange, path);
                case "POST" -> handlePost(exchange, path);
                case "DELETE" -> handleDelete(exchange, path);
                default -> sendMethodNotAllowed(exchange);
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendServerError(exchange);
        }
    }

    private void handleGet(HttpExchange exchange, String path) throws IOException {
        if ("/epics".equals(path)) {
            List<Epic> epics = manager.getAllEpics();
            sendResponse(exchange, 200, gson.toJson(epics));
        } else if (path.matches("/epics/\\d+/subtasks")) {
            int epicId = extractIdFromPath(path);
            Epic epic = manager.getEpicById(epicId);
            if (epic == null) {
                sendResponse(exchange, 404, "{\"error\":\"Epic not found\"}");
                return;
            }
            sendResponse(exchange, 200, gson.toJson(manager.getSubtasksOfEpic(epicId)));
        } else if (path.matches("/epics/\\d+")) {
            int id = extractIdFromPath(path);
            Epic epic = manager.getEpicById(id);
            if (epic == null) {
                sendResponse(exchange, 404, "{\"error\":\"Epic not found\"}");
            } else {
                sendResponse(exchange, 200, gson.toJson(epic));
            }
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (!"/epics".equals(path)) {
            sendNotFound(exchange);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Epic epic = gson.fromJson(body, Epic.class);

        manager.createEpic(epic);
        sendResponse(exchange, 201, gson.toJson(epic));
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (!path.matches("/epics/\\d+")) {
            sendNotFound(exchange);
            return;
        }

        int id = extractIdFromPath(path);
        manager.deleteEpicById(id);
        sendResponse(exchange, 200, "{\"status\":\"Epic deleted\"}");
    }

    private void sendResponse(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
