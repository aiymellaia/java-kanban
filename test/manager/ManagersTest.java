package manager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagersTest {

    @Test
    void getDefault_shouldReturnInMemoryTaskManager() {
        TaskManager taskManager = Managers.getDefault();
        assertNotNull(taskManager, "TaskManager не должен быть null");
        assertInstanceOf(InMemoryTaskManager.class, taskManager, "Ожидается экземпляр InMemoryTaskManager");
    }

    @Test
    void getDefaultHistory_shouldReturnInMemoryHistoryManager() {
        HistoryManager historyManager = Managers.getDefaultHistory();
        assertNotNull(historyManager, "HistoryManager не должен быть null");
        assertInstanceOf(InMemoryHistoryManager.class, historyManager, "Ожидается экземпляр InMemoryHistoryManager");
    }
}
