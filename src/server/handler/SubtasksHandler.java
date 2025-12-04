package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Subtask;
import server.GsonAdapters;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public SubtasksHandler(TaskManager manager) {
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
        if ("/subtasks".equals(path)) {
            List<Subtask> subtasks = manager.getAllSubtasks();
            sendResponse(exchange, 200, gson.toJson(subtasks));
        } else if (path.matches("/subtasks/\\d+")) {
            int id = extractIdFromPath(path);
            Subtask subtask = manager.getSubtaskById(id);
            if (subtask == null) {
                sendNotFound(exchange);
            } else {
                sendResponse(exchange, 200, gson.toJson(subtask));
            }
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if (!"/subtasks".equals(path)) {
            sendNotFound(exchange);
            return;
        }

        InputStream is = exchange.getRequestBody();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        Subtask subtask = gson.fromJson(body, Subtask.class);

        try {
            if (subtask.getId() == 0) {
                manager.createSubtask(subtask);
            } else {
                if (manager.getSubtaskById(subtask.getId()) != null) {
                    manager.updateSubtask(subtask);
                } else {
                    sendNotFound(exchange);
                    return;
                }
            }

            sendResponse(exchange, 201, gson.toJson(subtask));

        } catch (IllegalArgumentException e) {
            sendResponse(exchange, 406, "{\"error\":\"Subtask overlaps with existing tasks\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (!path.matches("/subtasks/\\d+")) {
            sendNotFound(exchange);
            return;
        }

        int id = extractIdFromPath(path);
        if (manager.getSubtaskById(id) != null) {
            manager.deleteSubtaskById(id);
        }
        sendResponse(exchange, 200, "{\"status\":\"Subtask deleted\"}");
    }

    private void sendResponse(HttpExchange exchange, int code, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
