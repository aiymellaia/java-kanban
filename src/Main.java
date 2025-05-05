import manager.Managers;
import manager.TaskManager;
import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;

public class Main {
    public static void main(String[] args) {
        TaskManager manager = Managers.getDefault();

        Task task1 = new Task("Задача 1", "Описание 1", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание 2", Status.IN_PROGRESS);
        manager.createTask(task1);
        manager.createTask(task2);

        Epic epic = new Epic("Эпик 1", "Описание эпика");
        manager.createEpic(epic);

        Subtask sub1 = new Subtask("Подзадача 1", "Описание подзадачи", Status.NEW, epic.getId());
        Subtask sub2 = new Subtask("Подзадача 2", "Описание подзадачи", Status.DONE, epic.getId());
        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        manager.getTaskById(task1.getId());
        manager.getTaskById(task2.getId());
        manager.getEpicById(epic.getId());
        manager.getSubtaskById(sub1.getId());
        manager.getSubtaskById(sub2.getId());

        System.out.println("История просмотров:");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }
    }
}
