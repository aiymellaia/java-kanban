package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Task;
import server.GsonAdapters;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TasksHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public TasksHandler(TaskManager manager) {
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
        if ("/tasks".equals(path)) {
            sendJson(exchange, manager.getAllTasks(), 200);
        } else if (path.matches("/tasks/\\d+")) {
            int id = extractIdFromPath(path);
            Task task = manager.getTaskById(id);
            if (task == null) sendNotFound(exchange);
            else sendJson(exchange, task, 200);
        } else {
            sendNotFound(exchange);
        }
    }

    private void handlePost(HttpExchange exchange, String path) throws IOException {
        if ("/tasks".equals(path)) {
            Task task = gson.fromJson(
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
                    Task.class
            );
            try {
                if (task.getId() == 0) {
                    manager.createTask(task);
                    sendJson(exchange, task, 201); // создана новая задача
                } else {
                    if (manager.getTaskById(task.getId()) != null) {
                        manager.updateTask(task);
                        sendJson(exchange, task, 201); // обновлена существующая
                    } else {
                        sendNotFound(exchange); // задача для обновления не найдена
                    }
                }
            } catch (IllegalArgumentException e) {
                sendHasInteractions(exchange); // 406
            }
        } else {
            sendNotFound(exchange);
        }
    }

    private void handleDelete(HttpExchange exchange, String path) throws IOException {
        if (path.matches("/tasks/\\d+")) {
            int id = extractIdFromPath(path);
            if (manager.getTaskById(id) != null) {
                manager.deleteTaskById(id);
                sendJson(exchange, "{\"status\":\"Task deleted\"}", 200);
            } else {
                sendNotFound(exchange);
            }
        } else {
            sendNotFound(exchange);
        }
    }

    private void sendJson(HttpExchange exchange, Object obj, int code) throws IOException {
        String json = gson.toJson(obj);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json;charset=utf-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
