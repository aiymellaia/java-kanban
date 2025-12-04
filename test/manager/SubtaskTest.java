package manager;

import model.Status;
import model.Subtask;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SubtaskTest {
    @Test
    void equalSubtasks() {
        Subtask sub1 = new Subtask("Подзадача 1", "Описание", Status.NEW, 1);
        Subtask sub2 = new Subtask("Подзадача 2", "Другое описание", Status.DONE, 2);
        sub1.setId(2);
        sub2.setId(2);
        assertEquals(sub1, sub2);
    }
}
