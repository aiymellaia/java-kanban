package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Subtask;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SubtasksHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public SubtasksHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/subtasks".equals(path)) {
                List<Subtask> subtasks = manager.getAllSubtasks();
                sendText(exchange, gson.toJson(subtasks));
                return;
            }

            if ("POST".equals(method) && "/subtasks".equals(path)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                Subtask subtask = gson.fromJson(body, Subtask.class);

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

                sendText(exchange, gson.toJson(subtask));
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/subtasks")) {
                String query = exchange.getRequestURI().getQuery();

                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));

                    if (manager.getSubtaskById(id) != null) {
                        manager.deleteSubtaskById(id);
                        sendText(exchange, "{\"status\":\"Subtask deleted\"}");
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
}
