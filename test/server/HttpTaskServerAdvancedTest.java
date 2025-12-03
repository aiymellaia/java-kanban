package server;

import manager.InMemoryTaskManager;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HttpTaskServerAdvancedTest {

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
    public void testGetSubtasksOfEpic() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic X", "Epic Desc");
        String epicJson = HttpTaskServer.getGson().toJson(epic);

        HttpRequest epicRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics"))
                .POST(HttpRequest.BodyPublishers.ofString(epicJson))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> epicResponse = client.send(epicRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, epicResponse.statusCode());

        int epicId = manager.getAllEpics().get(0).getId();

        Subtask sub1 = new Subtask("Sub1", "Desc", Status.NEW, epicId);
        sub1.setStartTime(LocalDateTime.now());
        sub1.setDuration(Duration.ofMinutes(30));
        String sub1Json = HttpTaskServer.getGson().toJson(sub1);

        Subtask sub2 = new Subtask("Sub2", "Desc", Status.NEW, epicId);
        sub2.setStartTime(LocalDateTime.now().plusMinutes(40));
        sub2.setDuration(Duration.ofMinutes(20));
        String sub2Json = HttpTaskServer.getGson().toJson(sub2);

        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(sub1Json))
                .header("Content-Type", "application/json")
                .build();
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(sub2Json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res1.statusCode());
        assertEquals(201, res2.statusCode());

        HttpRequest getSubtasksRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/" + epicId + "/subtasks"))
                .GET()
                .build();
        HttpResponse<String> getSubtasksResponse = client.send(getSubtasksRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, getSubtasksResponse.statusCode());
        assertTrue(getSubtasksResponse.body().contains("Sub1"));
        assertTrue(getSubtasksResponse.body().contains("Sub2"));
    }

    @Test
    public void testHistoryEndpoint() throws IOException, InterruptedException {
        // Создаём задачу
        Task task = new Task("HistoryTask", "Desc");
        manager.createTask(task);
        int taskId = manager.getAllTasks().get(0).getId();

        HttpRequest getTask = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks/" + taskId))
                .GET()
                .build();
        client.send(getTask, HttpResponse.BodyHandlers.ofString());

        HttpRequest historyRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/history"))
                .GET()
                .build();
        HttpResponse<String> historyResponse = client.send(historyRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, historyResponse.statusCode());
        assertTrue(historyResponse.body().contains("HistoryTask"));
    }

    @Test
    public void testPrioritizedTasksEndpoint() throws IOException, InterruptedException {
        Task task1 = new Task("Task1", "Desc");
        task1.setStartTime(LocalDateTime.now().plusMinutes(50));
        task1.setDuration(Duration.ofMinutes(20));
        manager.createTask(task1);

        Task task2 = new Task("Task2", "Desc");
        task2.setStartTime(LocalDateTime.now());
        task2.setDuration(Duration.ofMinutes(15));
        manager.createTask(task2);

        Epic epic = new Epic("EpicPrioritized", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask sub = new Subtask("SubtaskPrior", "Desc", Status.NEW, epicId);
        sub.setStartTime(LocalDateTime.now().plusMinutes(30));
        sub.setDuration(Duration.ofMinutes(10));
        manager.createSubtask(sub);

        HttpRequest prioritizedRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/prioritized"))
                .GET()
                .build();
        HttpResponse<String> prioritizedResponse = client.send(prioritizedRequest, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, prioritizedResponse.statusCode());

        String body = prioritizedResponse.body();
        assertTrue(body.indexOf("Task2") < body.indexOf("SubtaskPrior"));
        assertTrue(body.indexOf("SubtaskPrior") < body.indexOf("Task1"));
    }

    @Test
    public void testHistoryContainsAllTypes() throws IOException, InterruptedException {
        Task task = new Task("TaskHistory", "Desc");
        manager.createTask(task);
        Epic epic = new Epic("EpicHistory", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();
        Subtask sub = new Subtask("SubHistory", "Desc", Status.NEW, epicId);
        manager.createSubtask(sub);

        for (Task t : manager.getAllTasks()) sendGet("/tasks/" + t.getId());
        for (Epic e : manager.getAllEpics()) sendGet("/epics/" + e.getId());
        for (Subtask s : manager.getAllSubtasks()) sendGet("/subtasks/" + s.getId());

        HttpResponse<String> history = sendGet("/history");
        assertEquals(200, history.statusCode());
        assertTrue(history.body().contains("TaskHistory"));
        assertTrue(history.body().contains("EpicHistory"));
        assertTrue(history.body().contains("SubHistory"));
    }

    @Test
    public void testPrioritizedWithNullStartTime() throws IOException, InterruptedException {
        Task task1 = new Task("TaskEarly", "Desc");
        task1.setStartTime(LocalDateTime.now());
        manager.createTask(task1);

        Task task2 = new Task("TaskNull", "Desc");
        task2.setStartTime(null);
        manager.createTask(task2);

        HttpResponse<String> response = sendGet("/prioritized");
        assertEquals(200, response.statusCode());
        assertTrue(response.body().indexOf("TaskEarly") < response.body().indexOf("TaskNull"));
    }

    private HttpResponse<String> sendGet(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080" + path))
                .GET()
                .build();
        return client.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    public void testUnsupportedMethod() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/tasks"))
                .method("PUT", HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(405, response.statusCode(), "Код должен быть 405 для неподдерживаемого метода");
    }
}
