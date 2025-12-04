package manager;

import model.Status;
import model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {

    private HistoryManager historyManager;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
    }

    @Test
    void add_shouldAddTasksToHistory() {
        Task task = new Task("Test", "Description", Status.NEW);
        historyManager.add(task);

        List<Task> history = historyManager.getHistory();

        assertNotNull(history, "История не должна быть null.");
        assertEquals(1, history.size(), "История должна содержать одну задачу.");
        assertEquals(task, history.get(0), "Задача в истории неверна.");
    }


    @Test
    void duplicateTaskShouldMoveToEnd() {
        Task task1 = new Task("Task", "Описание", Status.NEW);
        task1.setId(1);

        Task task2 = new Task("Another", "Описание", Status.NEW);
        task2.setId(2);

        historyManager.add(task1);
        historyManager.add(task2);
        historyManager.add(task1);

        List<Task> history = historyManager.getHistory();

        assertEquals(2, history.size(), "История должна содержать только 2 задачи (без дублей).");
        assertEquals(task2, history.get(0), "Первая задача должна быть task2.");
        assertEquals(task1, history.get(1), "Последней должна быть повторно добавленная task1.");
    }

    @Test
    void remove_shouldRemoveTaskFromHistory() {
        Task task = new Task("Task", "Описание", Status.NEW);
        task.setId(1);

        historyManager.add(task);
        historyManager.remove(1);

        List<Task> history = historyManager.getHistory();
        assertTrue(history.isEmpty(), "История должна быть пустой после удаления.");
    }

    @Test
    void add_shouldIgnoreNullTasks() {
        historyManager.add(null);
        List<Task> history = historyManager.getHistory();
        assertTrue(history.isEmpty(), "История не должна содержать null-задачи.");
    }

    @Test
    void remove_shouldDoNothingIfTaskNotInHistory() {
        Task task = new Task("Task", "Описание", Status.NEW);
        task.setId(1);
        historyManager.add(task);

        historyManager.remove(2);

        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size(), "История не должна измениться.");
        assertEquals(task, history.get(0));
    }

}
