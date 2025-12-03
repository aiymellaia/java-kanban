package server.handler;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import manager.TaskManager;
import model.Task;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TasksHandler extends BaseHttpHandler implements HttpHandler {

    private final TaskManager manager;
    private final Gson gson;

    public TasksHandler(TaskManager manager) {
        this.manager = manager;
        this.gson = new Gson();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("GET".equals(method) && "/tasks".equals(path)) {
                List<Task> tasks = manager.getAllTasks();
                String response = gson.toJson(tasks);
                sendText(exchange, response);
                return;
            }

            if ("POST".equals(method) && "/tasks".equals(path)) {
                InputStream is = exchange.getRequestBody();
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Task task = gson.fromJson(body, Task.class);

                if (task.getId() == 0) {
                    manager.createTask(task);
                } else {
                    if (manager.getTaskById(task.getId()) != null) {
                        manager.updateTask(task);
                    } else {
                        sendNotFound(exchange);
                        return;
                    }
                }

                sendText(exchange, gson.toJson(task));
                return;
            }

            if ("DELETE".equals(method) && path.startsWith("/tasks")) {
                String query = exchange.getRequestURI().getQuery(); // ?id=123
                if (query != null && query.startsWith("id=")) {
                    int id = Integer.parseInt(query.substring(3));
                    if (manager.getTaskById(id) != null) {
                        manager.deleteTaskById(id);
                        sendText(exchange, "{\"status\":\"Task deleted\"}");
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
