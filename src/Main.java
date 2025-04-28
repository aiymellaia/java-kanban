public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        Task task = new Task("Переезд", "Собрать вещи", Status.NEW);
        manager.createTask(task);

        Epic epic = new Epic("Переезд в новую квартиру", "Организация");
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("Упаковать книги", "Коробка для книг", Status.NEW, epic.getId());
        manager.createSubtask(subtask1);

        Subtask subtask2 = new Subtask("Упаковать одежду", "Коробка для одежды", Status.NEW, epic.getId());
        manager.createSubtask(subtask2);

        task.setStatus(Status.IN_PROGRESS);
        manager.updateTask(task);

        subtask1.setStatus(Status.DONE);
        manager.updateSubtask(subtask1);

        subtask2.setStatus(Status.IN_PROGRESS);
        manager.updateSubtask(subtask2);

        System.out.println("\n--- Все задачи ---");
        System.out.println(manager.getAllTasks());

        System.out.println("\n--- Все эпики ---");
        System.out.println(manager.getAllEpics());

        System.out.println("\n--- Все подзадачи эпика ---");
        System.out.println(manager.getSubtasksOfEpic(epic.getId()));

        manager.deleteTaskById(task.getId());
        manager.deleteEpicById(epic.getId());

        System.out.println("\n--- Все задачи после удаления ---");
        System.out.println(manager.getAllTasks());

        System.out.println("\n--- Все эпики после удаления ---");
        System.out.println(manager.getAllEpics());

        System.out.println("\n--- Все подзадачи после удаления эпика ---");
        System.out.println(manager.getAllSubtasks());
    }
}

