package manager;

import model.Epic;
import model.Subtask;
import model.Task;

import java.util.List;

public interface TaskManager {
    List<Task> getAllTasks();
    void removeAllTasks();
    Task getTaskById(int id);
    void createTask(Task task);
    void updateTask(Task task);
    void deleteTaskById(int id);

    List<Epic> getAllEpics();
    void removeAllEpics();
    Epic getEpicById(int id);
    void createEpic(Epic epic);
    void updateEpic(Epic epic);
    void deleteEpicById(int id);

    List<Subtask> getAllSubtasks();
    void removeAllSubtasks();
    Subtask getSubtaskById(int id);
    void createSubtask(Subtask subtask);
    void updateSubtask(Subtask subtask);
    void deleteSubtaskById(int id);

    List<Subtask> getSubtasksOfEpic(int epicId);

    List<Task> getHistory();
}

