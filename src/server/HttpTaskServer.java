package server;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import manager.TaskManager;
import manager.InMemoryTaskManager;
import model.Epic;
import model.Subtask;
import model.Task;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class HttpTaskServer {

    private final TaskManager manager;
    private HttpServer server;
    private static final int PORT = 8080;
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                @Override
                public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.format(formatter));
                }
            })
            .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
                @Override
                public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                    return LocalDateTime.parse(json.getAsString(), formatter);
                }
            })
            .registerTypeAdapter(Duration.class, new JsonSerializer<Duration>() {
                @Override
                public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext context) {
                    return new JsonPrimitive(src.toMinutes());
                }
            })
            .registerTypeAdapter(Duration.class, new JsonDeserializer<Duration>() {
                @Override
                public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
                    return Duration.ofMinutes(json.getAsLong());
                }
            })
            .create();

    public HttpTaskServer(TaskManager manager) {
        this.manager = manager;
    }

    public static Gson getGson() {
        return gson;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/tasks", this::handleTasks);
        server.createContext("/tasks/", this::handleTaskById);

        server.createContext("/subtasks", this::handleSubtasks);
        server.createContext("/subtasks/", this::handleSubtaskById);

        server.createContext("/epics", this::handleEpics);
        server.createContext("/epics/", this::handleEpicById);

        server.createContext("/history", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, gson.toJson(manager.getHistory()));
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.createContext("/prioritized", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, gson.toJson(manager.getPrioritizedTasks()));
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.start();
        System.out.println("HTTP сервер запущен на порту " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP сервер остановлен");
        }
    }

    private void handleTasks(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> sendResponse(exchange, 200, gson.toJson(manager.getAllTasks()));
            case "POST" -> {
                Task task = gson.fromJson(new String(exchange.getRequestBody().readAllBytes()), Task.class);
                try {
                    if (task.getId() == 0) {
                        manager.createTask(task);
                        sendResponse(exchange, 201, gson.toJson(task));
                    } else {
                        if (manager.getTaskById(task.getId()) != null) {
                            manager.updateTask(task);
                            sendResponse(exchange, 201, gson.toJson(task));
                        } else {
                            sendResponse(exchange, 404, "Задача не найдена для обновления");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    sendResponse(exchange, 406, "Задача пересекается с существующей");
                }
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleTaskById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = extractId(exchange.getRequestURI().getPath(), "/tasks/");
        if (idOpt.isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        int id = idOpt.get();

        switch (exchange.getRequestMethod()) {
            case "GET" -> {
                Task task = manager.getTaskById(id);
                if (task == null) sendResponse(exchange, 404, "Задача не найдена");
                else sendResponse(exchange, 200, gson.toJson(task));
            }
            case "DELETE" -> {
                Task task = manager.getTaskById(id);
                if (task == null) {
                    sendResponse(exchange, 404, "Задача не найдена");
                } else {
                    manager.deleteTaskById(id);
                    sendResponse(exchange, 200, "Задача удалена");
                }
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleSubtasks(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> sendResponse(exchange, 200, gson.toJson(manager.getAllSubtasks()));
            case "POST" -> {
                Subtask subtask = gson.fromJson(new String(exchange.getRequestBody().readAllBytes()), Subtask.class);
                try {
                    if (subtask.getId() == 0) {
                        manager.createSubtask(subtask);
                        sendResponse(exchange, 201, gson.toJson(subtask));
                    } else {
                        if (manager.getSubtaskById(subtask.getId()) != null) {
                            manager.updateSubtask(subtask);
                            sendResponse(exchange, 201, gson.toJson(subtask));
                        } else {
                            sendResponse(exchange, 404, "Подзадача не найдена для обновления");
                        }
                    }
                } catch (IllegalArgumentException e) {
                    sendResponse(exchange, 406, "Подзадача пересекается с существующей");
                }
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleSubtaskById(HttpExchange exchange) throws IOException {
        Optional<Integer> idOpt = extractId(exchange.getRequestURI().getPath(), "/subtasks/");
        if (idOpt.isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        int id = idOpt.get();

        switch (exchange.getRequestMethod()) {
            case "GET" -> {
                Subtask subtask = manager.getSubtaskById(id);
                if (subtask == null) sendResponse(exchange, 404, "Подзадача не найдена");
                else sendResponse(exchange, 200, gson.toJson(subtask));
            }
            case "DELETE" -> {
                manager.deleteSubtaskById(id);
                sendResponse(exchange, 200, "Подзадача удалена");
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleEpics(HttpExchange exchange) throws IOException {
        switch (exchange.getRequestMethod()) {
            case "GET" -> sendResponse(exchange, 200, gson.toJson(manager.getAllEpics()));
            case "POST" -> {
                Epic epic = gson.fromJson(new String(exchange.getRequestBody().readAllBytes()), Epic.class);
                manager.createEpic(epic);
                sendResponse(exchange, 201, gson.toJson(epic));
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private void handleEpicById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Optional<Integer> idOpt = extractId(path, "/epics/");
        if (idOpt.isEmpty()) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        int id = idOpt.get();

        if (path.endsWith("/subtasks")) {
            Epic epic = manager.getEpicById(id);
            if (epic == null) sendResponse(exchange, 404, "Эпик не найден");
            else sendResponse(exchange, 200, gson.toJson(manager.getSubtasksOfEpic(id)));
            return;
        }

        switch (exchange.getRequestMethod()) {
            case "GET" -> {
                Epic epic = manager.getEpicById(id);
                if (epic == null) sendResponse(exchange, 404, "Эпик не найден");
                else sendResponse(exchange, 200, gson.toJson(epic));
            }
            case "DELETE" -> {
                manager.deleteEpicById(id);
                sendResponse(exchange, 200, "Эпик удалён");
            }
            default -> exchange.sendResponseHeaders(405, -1);
        }
    }

    private Optional<Integer> extractId(String path, String prefix) {
        try {
            String s = path.substring(prefix.length());
            if (s.contains("/")) s = s.substring(0, s.indexOf("/"));
            return Optional.of(Integer.parseInt(s));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void sendResponse(HttpExchange exchange, int code, String response) throws IOException {
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public static void main(String[] args) throws IOException {
        TaskManager manager = new InMemoryTaskManager();
        HttpTaskServer server = new HttpTaskServer(manager);
        server.start();
    }
}
