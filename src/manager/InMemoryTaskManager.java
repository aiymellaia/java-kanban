package manager;

import model.Epic;
import model.Status;
import model.Subtask;
import model.Task;

import java.time.LocalDateTime;
import java.util.*;

public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks = new HashMap<>();
    protected final Map<Integer, Epic> epics = new HashMap<>();
    protected final Map<Integer, Subtask> subtasks = new HashMap<>();
    protected final HistoryManager historyManager = Managers.getDefaultHistory();
    protected int nextId = 1;

    private int generateId() {
        return nextId++;
    }

    @Override
    public List<Task> getAllTasks() {
        return new ArrayList<>(tasks.values());
    }

    @Override
    public void removeAllTasks() {
        tasks.clear();
    }

    @Override
    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null) historyManager.add(task);
        return task;
    }

    @Override
    public void createTask(Task task) {
        checkTimeOverlap(task);
        task.setId(generateId());
        tasks.put(task.getId(), task);
    }

    @Override
    public void updateTask(Task updatedTask) {
        if (!tasks.containsKey(updatedTask.getId())) return;
        checkTimeOverlap(updatedTask);
        tasks.put(updatedTask.getId(), updatedTask);
    }

    @Override
    public void deleteTaskById(int id) {
        tasks.remove(id);
    }

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void removeAllEpics() {
        epics.clear();
        subtasks.clear();
    }

    @Override
    public Epic getEpicById(int id) {
        Epic epic = epics.get(id);
        if (epic != null) historyManager.add(epic);
        return epic;
    }

    @Override
    public void createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
    }

    @Override
    public void updateEpic(Epic updatedEpic) {
        if (!epics.containsKey(updatedEpic.getId())) return;
        Epic oldEpic = epics.get(updatedEpic.getId());
        updatedEpic.clearSubtasks();
        updatedEpic.getSubtaskIds().addAll(oldEpic.getSubtaskIds());
        epics.put(updatedEpic.getId(), updatedEpic);
        updateEpicStatus(updatedEpic);
        updateEpicTime(updatedEpic);
    }

    @Override
    public void deleteEpicById(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            for (int subId : epic.getSubtaskIds()) subtasks.remove(subId);
        }
    }

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void removeAllSubtasks() {
        subtasks.clear();
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            epic.setStatus(Status.NEW);
            epic.setStartTime(null);
            epic.setDuration(java.time.Duration.ZERO);
        }
    }

    @Override
    public Subtask getSubtaskById(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) historyManager.add(subtask);
        return subtask;
    }

    @Override
    public void createSubtask(Subtask subtask) {
        Epic epic = epics.get(subtask.getEpicId());
        if (epic == null) throw new IllegalArgumentException("Эпик не найден для подзадачи");

        checkTimeOverlap(subtask);

        subtask.setId(generateId());
        subtasks.put(subtask.getId(), subtask);
        epic.addSubtaskId(subtask.getId());

        updateEpicStatus(epic);
        updateEpicTime(epic);
    }

    @Override
    public void updateSubtask(Subtask updatedSubtask) {
        if (!subtasks.containsKey(updatedSubtask.getId())) return;

        checkTimeOverlap(updatedSubtask);

        subtasks.put(updatedSubtask.getId(), updatedSubtask);
        Epic epic = epics.get(updatedSubtask.getEpicId());
        if (epic != null) {
            updateEpicStatus(epic);
            updateEpicTime(epic);
        }
    }

    @Override
    public void deleteSubtaskById(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask == null) return;

        Epic epic = epics.get(subtask.getEpicId());
        if (epic != null) {
            epic.removeSubtaskId(id);
            updateEpicStatus(epic);
            updateEpicTime(epic);
        }
    }

    @Override
    public List<Subtask> getSubtasksOfEpic(int epicId) {
        List<Subtask> result = new ArrayList<>();
        Epic epic = epics.get(epicId);
        if (epic != null) {
            for (int subId : epic.getSubtaskIds()) {
                Subtask subtask = subtasks.get(subId);
                if (subtask != null) result.add(subtask);
            }
        }
        return result;
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        TreeSet<Task> prioritized = new TreeSet<>(
                Comparator.comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        prioritized.addAll(tasks.values());
        prioritized.addAll(subtasks.values());
        prioritized.addAll(epics.values());
        return new ArrayList<>(prioritized);
    }

    private void updateEpicStatus(Epic epic) {
        List<Integer> subIds = epic.getSubtaskIds();
        if (subIds.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true, allDone = true;
        for (int id : subIds) {
            Subtask s = subtasks.get(id);
            if (s != null) {
                if (s.getStatus() != Status.NEW) allNew = false;
                if (s.getStatus() != Status.DONE) allDone = false;
            }
        }

        if (allDone) epic.setStatus(Status.DONE);
        else if (allNew) epic.setStatus(Status.NEW);
        else epic.setStatus(Status.IN_PROGRESS);
    }

    private void updateEpicTime(Epic epic) {
        List<Subtask> subs = getSubtasksOfEpic(epic.getId());
        if (subs.isEmpty()) {
            epic.setStartTime(null);
            epic.setDuration(java.time.Duration.ZERO);
            return;
        }

        LocalDateTime start = null, end = null;
        java.time.Duration total = java.time.Duration.ZERO;

        for (Subtask s : subs) {
            if (s.getStartTime() != null) {
                if (start == null || s.getStartTime().isBefore(start)) start = s.getStartTime();
                LocalDateTime subEnd = s.getEndTime();
                if (end == null || (subEnd != null && subEnd.isAfter(end))) end = subEnd;
            }
            if (s.getDuration() != null) total = total.plus(s.getDuration());
        }

        epic.setStartTime(start);
        epic.setDuration(total);
    }

    private void checkTimeOverlap(Task newTask) {
        if (newTask.getStartTime() == null || newTask.getDuration() == null) return;
        LocalDateTime newStart = newTask.getStartTime();
        LocalDateTime newEnd = newTask.getEndTime();

        for (Task t : tasks.values()) {
            if (t.getStartTime() != null && t.getDuration() != null && isOverlap(newStart, newEnd, t.getStartTime(), t.getEndTime())) {
                throw new IllegalArgumentException("Задача пересекается с другой задачей");
            }
        }

        for (Subtask s : subtasks.values()) {
            if (s.getStartTime() != null && s.getDuration() != null && isOverlap(newStart, newEnd, s.getStartTime(), s.getEndTime())) {
                throw new IllegalArgumentException("Задача пересекается с другой задачей");
            }
        }
    }

    private boolean isOverlap(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        return !start1.isAfter(end2) && !start2.isAfter(end1);
    }
}
