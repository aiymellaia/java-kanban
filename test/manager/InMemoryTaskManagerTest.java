package manager;

import model.Status;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import model.Epic;
import model.Subtask;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {

    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void addNewTask_shouldStoreAndReturnTask() {
        Task task = new Task("Test Task", "Test Description", Status.NEW);
        manager.createTask(task);

        Task savedTask = manager.getTaskById(task.getId());

        assertNotNull(savedTask, "Задача не найдена.");
        assertEquals(task, savedTask, "Задачи не совпадают.");

        List<Task> allTasks = manager.getAllTasks();

        assertNotNull(allTasks, "Список задач не должен быть пустым.");
        assertEquals(1, allTasks.size(), "Неверное количество задач.");
        assertEquals(task, allTasks.get(0), "Задача в списке не совпадает.");
    }

    @Test
    void updateSubtaskStatus_shouldUpdateEpicStatus() {
        Epic epic = new Epic("Эпик", "Описание эпика");
        manager.createEpic(epic);

        Subtask sub1 = new Subtask("Подзадача 1", "Описание 1", Status.NEW, epic.getId());
        Subtask sub2 = new Subtask("Подзадача 2", "Описание 2", Status.NEW, epic.getId());

        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        Epic savedEpic = manager.getEpicById(epic.getId());
        assertEquals(Status.NEW, savedEpic.getStatus(), "Статус эпика должен быть NEW");

        sub1.setStatus(Status.DONE);
        manager.updateSubtask(sub1);

        savedEpic = manager.getEpicById(epic.getId());
        assertEquals(Status.IN_PROGRESS, savedEpic.getStatus(), "Статус эпика должен быть IN_PROGRESS");

        sub2.setStatus(Status.DONE);
        manager.updateSubtask(sub2);

        savedEpic = manager.getEpicById(epic.getId());
        assertEquals(Status.DONE, savedEpic.getStatus(), "Статус эпика должен быть DONE");
    }

    @Test
    void deleteEpic_shouldAlsoRemoveSubtasks() {
        Epic epic = new Epic("Эпик", "Переезд");
        manager.createEpic(epic);

        Subtask sub1 = new Subtask("Упаковать вещи", "Коробки", Status.NEW, epic.getId());
        Subtask sub2 = new Subtask("Перевезти", "Грузовик", Status.NEW, epic.getId());

        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        assertEquals(2, manager.getAllSubtasks().size());
        manager.deleteEpicById(epic.getId());

        assertNull(manager.getEpicById(epic.getId()));
        assertEquals(0, manager.getAllSubtasks().size());
    }
    @Test
    void tasksWithSameId_shouldBeEqual() {
        Task task1 = new Task("Оригинал", "Описание 1", Status.NEW);
        Task task2 = new Task("Другое имя", "Описание 2", Status.IN_PROGRESS);

        task1.setId(100);
        task2.setId(100);

        assertEquals(task1, task2, "Задачи с одинаковым id должны считаться равными");
    }

    @Test
    void epicCannotBeItsOwnSubtask() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Subtask invalidSubtask = new Subtask("Ошибка", "Подзадача с тем же id", Status.NEW, epic.getId());
        invalidSubtask.setId(epic.getId());

        try {
            manager.createSubtask(invalidSubtask);
            fail("Ожидалось исключение IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Подзадача не может ссылаться на эпик с таким же id, как у неё самой.", e.getMessage());
        }
    }

    @Test
    void subtaskCannotBeEpicOfItself() {
        Subtask subtask = new Subtask("Подзадача", "Описание", Status.NEW, 10);
        subtask.setId(10);

        try {
            manager.createSubtask(subtask);
            fail("Ожидалось исключение IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Подзадача не может ссылаться на эпик с таким же id, как у неё самой.", e.getMessage());
        }
    }


    @Test
    void createEpic_shouldInitializeWithEmptySubtaskListAndStatusNEW() {
        Epic epic = new Epic("Эпик", "Описание");
        manager.createEpic(epic);

        Epic savedEpic = manager.getEpicById(epic.getId());

        assertNotNull(savedEpic);
        assertEquals(0, savedEpic.getSubtaskIds().size());
        assertEquals(Status.NEW, savedEpic.getStatus());
    }
}

