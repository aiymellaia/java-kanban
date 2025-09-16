package manager;

import model.Status;
import model.Task;
import model.Epic;
import model.Subtask;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @Test
    void shouldSaveAndLoadEmptyFile() throws IOException {
        File tempFile = File.createTempFile("tasks", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loaded.getAllTasks().isEmpty(), "Список задач должен быть пустым");
        assertTrue(loaded.getAllEpics().isEmpty(), "Список эпиков должен быть пустым");
        assertTrue(loaded.getAllSubtasks().isEmpty(), "Список подзадач должен быть пустым");
    }

    @Test
    void shouldSaveAndLoadMultipleTasks() throws IOException {
        File tempFile = File.createTempFile("tasks", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        Task task = new Task("Task1", "Desc1", Status.NEW);
        manager.createTask(task);

        Epic epic = new Epic("Epic1", "DescEpic");
        manager.createEpic(epic);

        Subtask sub = new Subtask("Sub1", "DescSub", Status.NEW, epic.getId());
        manager.createSubtask(sub);

        assertEquals(1, manager.getAllTasks().size(), "Должна быть 1 задача");
        assertEquals(1, manager.getAllEpics().size(), "Должен быть 1 эпик");
        assertEquals(1, manager.getAllSubtasks().size(), "Должна быть 1 подзадача");

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(manager.getAllEpics(), loaded.getAllEpics(), "Эпики должны совпадать");
        assertEquals(manager.getAllSubtasks(), loaded.getAllSubtasks(), "Подзадачи должны совпадать");
    }
}
