package server;

import manager.InMemoryTaskManager;
import manager.TaskManager;
import model.Task;
import model.Status;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpTaskServerTest {

    private TaskManager manager;
    private HttpTaskServer server;
    private HttpClient client;

    @BeforeAll
    public void setupServer() throws IOException, InterruptedException {
        manager = new InMemoryTaskManager();
        server = new HttpTaskServer(manager);
        server.start();
        client = HttpClient.newHttpClient();

        Thread.sleep(500);
    }

    @AfterAll
    public void stopServer() {
        server.stop();
    }

    @BeforeEach
    public void clearData() {
        manager.getAllTasks().forEach(task -> manager.deleteTaskById(task.getId()));
        manager.getAllEpics().forEach(epic -> manager.deleteEpicById(epic.getId()));
        manager.getAllSubtasks().forEach(subtask -> manager.deleteSubtaskById(subtask.getId()));
    }

    @Test
    public void testAddTask() throws IOException, InterruptedException {
        Task task = new Task("Test Task", "Description", Status.NEW);
        task.setDuration(Duration.ofMinutes(10));
        task.setStartTime(LocalDateTime.now());

        String taskJson = GsonAdapters.createGson().toJson(task);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(taskJson))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode(), "Код ответа должен быть 201");
        List<Task> tasksFromManager = manager.getAllTasks();
        assertEquals(1, tasksFromManager.size(), "Должна создаться одна задача");
        assertEquals("Test Task", tasksFromManager.get(0).getName(), "Имя задачи должно совпадать");
    }

    @Test
    public void testGetTaskById() throws IOException, InterruptedException {
        Task task = new Task("GetTask", "Desc", Status.NEW);
        task.setDuration(Duration.ofMinutes(5));
        task.setStartTime(LocalDateTime.now());
        manager.createTask(task);

        int id = manager.getAllTasks().get(0).getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Код ответа должен быть 200");
        assertTrue(response.body().contains("GetTask"), "Тело ответа должно содержать имя задачи");
    }

    @Test
    public void testDeleteTask() throws IOException, InterruptedException {
        Task task = new Task("DeleteTask", "Desc", Status.NEW);
        task.setDuration(Duration.ofMinutes(5));
        task.setStartTime(LocalDateTime.now());
        manager.createTask(task);

        int id = manager.getAllTasks().get(0).getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Код ответа должен быть 200");
        assertTrue(manager.getAllTasks().isEmpty(), "Задача должна быть удалена");
    }

    @Test
    public void testGetAllTasks() throws IOException, InterruptedException {
        Task task1 = new Task("Task1", "Desc", Status.NEW);
        task1.setDuration(Duration.ofMinutes(5));
        task1.setStartTime(LocalDateTime.now());

        Task task2 = new Task("Task2", "Desc", Status.NEW);
        task2.setDuration(Duration.ofMinutes(10));
        task2.setStartTime(LocalDateTime.now().plusMinutes(10));

        manager.createTask(task1);
        manager.createTask(task2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode(), "Код ответа должен быть 200");
        assertTrue(response.body().contains("Task1"), "Ответ должен содержать Task1");
        assertTrue(response.body().contains("Task2"), "Ответ должен содержать Task2");
    }

    @Test
    public void testAddAndGetTask() throws IOException, InterruptedException {
        Task task = new Task("Task 1", "Desc");
        task.setDuration(Duration.ofMinutes(10));
        task.setStartTime(LocalDateTime.now());
        String json = GsonAdapters.createGson().toJson(task);

        HttpRequest post = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> responsePost = client.send(post, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, responsePost.statusCode());

        int id = manager.getAllTasks().get(0).getId();

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + id))
                .GET()
                .build();
        HttpResponse<String> responseGet = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, responseGet.statusCode());
        assertTrue(responseGet.body().contains("Task 1"));
    }

    @Test
    public void testGetAllTasksEmpty() throws IOException, InterruptedException {
        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body(), "Должен вернуться пустой список");
    }

    @Test
    public void testDeleteTaskNotFound() throws IOException, InterruptedException {
        HttpRequest delete = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .DELETE()
                .build();
        HttpResponse<String> response = client.send(delete, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode());
    }

    @Test
    public void testGetTaskByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/999"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(404, response.statusCode(), "Код должен быть 404 для несуществующей задачи");
    }

    @Test
    public void testAddTaskWithOverlap() throws IOException, InterruptedException {
        Task task1 = new Task("Task1", "Desc");
        task1.setStartTime(LocalDateTime.now());
        task1.setDuration(Duration.ofMinutes(60));
        manager.createTask(task1);

        Task task2 = new Task("TaskOverlap", "Desc");
        task2.setStartTime(task1.getStartTime().plusMinutes(30));
        task2.setDuration(Duration.ofMinutes(30));

        String json = GsonAdapters.createGson().toJson(task2);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response.statusCode(), "Код должен быть 406 при пересечении задач");
    }
}
