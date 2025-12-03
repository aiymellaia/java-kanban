package server;

import manager.InMemoryTaskManager;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.Subtask;
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
public class HttpTaskServerSubtaskTest {

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
    public void testAddSubtask() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 1", "Description");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask subtask = new Subtask("Subtask 1", "Desc", Status.NEW, epicId);
        subtask.setDuration(Duration.ofMinutes(15));
        subtask.setStartTime(LocalDateTime.now());

        String json = HttpTaskServer.getGson().toJson(subtask);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(201, response.statusCode());
        List<Subtask> subtasks = manager.getAllSubtasks();
        assertEquals(1, subtasks.size());
        assertEquals("Subtask 1", subtasks.get(0).getName());
    }

    @Test
    public void testGetSubtaskById() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 2", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask subtask = new Subtask("GetSubtask", "Desc", Status.NEW, epicId);
        manager.createSubtask(subtask);
        int id = manager.getAllSubtasks().get(0).getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("GetSubtask"));
    }

    @Test
    public void testGetSubtaskByIdNotFound() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/999"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(404, response.statusCode());
    }

    @Test
    public void testDeleteSubtask() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 3", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask subtask = new Subtask("DeleteSubtask", "Desc", Status.NEW, epicId);
        manager.createSubtask(subtask);
        int id = manager.getAllSubtasks().get(0).getId();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(manager.getAllSubtasks().isEmpty());
    }

    @Test
    public void testGetAllSubtasks() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 4", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask sub1 = new Subtask("Sub1", "Desc", Status.NEW, epicId);
        Subtask sub2 = new Subtask("Sub2", "Desc", Status.NEW, epicId);
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("Sub1"));
        assertTrue(response.body().contains("Sub2"));
    }

    @Test
    public void testAddSubtaskWithOverlap() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 5", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask sub1 = new Subtask("Sub1", "Desc", Status.NEW, epicId);
        sub1.setStartTime(LocalDateTime.now());
        sub1.setDuration(Duration.ofMinutes(60));
        String json1 = HttpTaskServer.getGson().toJson(sub1);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        Subtask sub2 = new Subtask("Sub2", "Desc", Status.NEW, epicId);
        sub2.setStartTime(sub1.getStartTime().plusMinutes(30));
        sub2.setDuration(Duration.ofMinutes(30));
        String json2 = HttpTaskServer.getGson().toJson(sub2);

        HttpRequest request2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        assertEquals(406, response2.statusCode(), "Код должен быть 406 при пересечении задач");
    }

    @Test
    public void testAddSubtaskWithOverlapAdvanced() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic Advanced", "Desc");
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
        sub1.setDuration(Duration.ofMinutes(60));
        String sub1Json = HttpTaskServer.getGson().toJson(sub1);

        HttpRequest request1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(sub1Json))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, response1.statusCode());

        Subtask subOverlapFull = new Subtask("SubOverlapFull", "Desc", Status.NEW, epicId);
        subOverlapFull.setStartTime(sub1.getStartTime().plusMinutes(10));
        subOverlapFull.setDuration(Duration.ofMinutes(30));
        String jsonOverlapFull = HttpTaskServer.getGson().toJson(subOverlapFull);

        HttpRequest requestOverlapFull = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonOverlapFull))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> responseOverlapFull = client.send(requestOverlapFull, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, responseOverlapFull.statusCode(), "Должен быть 406 при полном пересечении");

        Subtask subOverlapPartial = new Subtask("SubOverlapPartial", "Desc", Status.NEW, epicId);
        subOverlapPartial.setStartTime(sub1.getStartTime().plusMinutes(50));
        subOverlapPartial.setDuration(Duration.ofMinutes(20));
        String jsonOverlapPartial = HttpTaskServer.getGson().toJson(subOverlapPartial);

        HttpRequest requestOverlapPartial = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonOverlapPartial))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> responseOverlapPartial = client.send(requestOverlapPartial, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, responseOverlapPartial.statusCode(), "Должен быть 406 при частичном пересечении");

        Subtask subNoTime = new Subtask("SubNoTime", "Desc", Status.NEW, epicId);
        String jsonNoTime = HttpTaskServer.getGson().toJson(subNoTime);

        HttpRequest requestNoTime = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonNoTime))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> responseNoTime = client.send(requestNoTime, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, responseNoTime.statusCode(), "Подзадача без времени должна создаться");

        Subtask subNullDuration = new Subtask("SubNullDuration", "Desc", Status.NEW, epicId);
        subNullDuration.setStartTime(sub1.getStartTime().plusMinutes(200));
        subNullDuration.setDuration(null);
        String jsonNullDuration = HttpTaskServer.getGson().toJson(subNullDuration);

        HttpRequest requestNullDuration = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonNullDuration))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> responseNullDuration = client.send(requestNullDuration, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, responseNullDuration.statusCode(), "Подзадача с null длительностью должна создаться");
    }

    @Test
    public void testAddSubtaskAndOverlap() throws IOException, InterruptedException {
        Epic epic = new Epic("Epic 2", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        Subtask sub1 = new Subtask("Sub1", "Desc", Status.NEW, epicId);
        sub1.setStartTime(LocalDateTime.now());
        sub1.setDuration(Duration.ofMinutes(60));
        String json1 = HttpTaskServer.getGson().toJson(sub1);
        HttpRequest req1 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json1))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        assertEquals(201, res1.statusCode());

        Subtask sub2 = new Subtask("SubOverlap", "Desc", Status.NEW, epicId);
        sub2.setStartTime(sub1.getStartTime().plusMinutes(30));
        sub2.setDuration(Duration.ofMinutes(30));
        String json2 = HttpTaskServer.getGson().toJson(sub2);
        HttpRequest req2 = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/subtasks"))
                .POST(HttpRequest.BodyPublishers.ofString(json2))
                .header("Content-Type", "application/json")
                .build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        assertEquals(406, res2.statusCode());
    }

    @Test
    public void testGetSubtasksOfEpicEmpty() throws IOException, InterruptedException {
        Epic epic = new Epic("EpicEmpty", "Desc");
        manager.createEpic(epic);
        int epicId = manager.getAllEpics().get(0).getId();

        HttpRequest get = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:8080/epics/" + epicId + "/subtasks"))
                .GET()
                .build();
        HttpResponse<String> response = client.send(get, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }
}
