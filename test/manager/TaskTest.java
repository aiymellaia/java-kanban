package manager;

import model.Status;
import model.Task;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TaskTest {
    @Test
    void equalTasks() {
        Task task1 = new Task("Имя", "Описание", Status.NEW);
        Task task2 = new Task("Другое имя", "Другое описание", Status.DONE);
        task1.setId(1);
        task2.setId(1);
        assertEquals(task1, task2);
    }
}
