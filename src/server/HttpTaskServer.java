package server;

import com.sun.net.httpserver.HttpServer;
import manager.InMemoryTaskManager;
import manager.TaskManager;
import server.handler.*;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpTaskServer {

    private final TaskManager manager;
    private HttpServer server;
    private static final int PORT = 8080;

    public HttpTaskServer(TaskManager manager) {
        this.manager = manager;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);

        server.createContext("/tasks", new TasksHandler(manager));
        server.createContext("/subtasks", new SubtasksHandler(manager));
        server.createContext("/epics", new EpicsHandler(manager));
        server.createContext("/history", new HistoryHandler(manager));
        server.createContext("/prioritized", new PrioritizedHandler(manager));

        server.start();
        System.out.println("HTTP сервер запущен на порту " + PORT);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("HTTP сервер остановлен");
        }
    }

    public static void main(String[] args) throws IOException {
        TaskManager manager = new InMemoryTaskManager();
        HttpTaskServer server = new HttpTaskServer(manager);
        server.start();
    }
}
