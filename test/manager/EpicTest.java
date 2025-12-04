package manager;

import model.Epic;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EpicTest {
    @Test
    void equalEpics() {
        Epic epic1 = new Epic("Эпик", "Описание");
        Epic epic2 = new Epic("Другой эпик", "Другое описание");
        epic1.setId(3);
        epic2.setId(3);
        assertEquals(epic1, epic2);
    }
}
