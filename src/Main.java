import manager.FileBackedTaskManager;
import model.*;

import java.io.File;

public class Main {
    public static void main(String[] args) {
        File file = new File("tasks.csv");

        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        Task task = new Task("Сходить в магазин", "Купить хлеб", Status.NEW);
        manager.createTask(task);

        Epic epic = new Epic("Переезд", "Собрать вещи");
        manager.createEpic(epic);

        Subtask sub = new Subtask("Упаковать коробки", "Сложить одежду", Status.NEW, epic.getId());
        manager.createSubtask(sub);

        System.out.println("Файл сохранён в: " + file.getAbsolutePath());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(file);

        System.out.println("Задачи после загрузки:");
        System.out.println(loaded.getAllTasks());
        System.out.println(loaded.getAllEpics());
        System.out.println(loaded.getAllSubtasks());
    }
}
