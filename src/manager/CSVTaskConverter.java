package manager;

import model.*;

import java.util.StringJoiner;

public class CSVTaskConverter {

    public static String taskToString(Task task) {
        StringJoiner joiner = new StringJoiner(",");
        joiner.add(String.valueOf(task.getId()))
                .add(task.getType().name())
                .add(task.getName())
                .add(task.getStatus().name())
                .add(task.getDescription());

        if (task.getType() == TaskType.SUBTASK) {
            joiner.add(String.valueOf(((Subtask) task).getEpicId()));
        } else {
            joiner.add("");
        }
        return joiner.toString();
    }

    public static Task fromString(String line) {
        String[] fields = line.split(",");
        int id = Integer.parseInt(fields[0]);
        TaskType type = TaskType.valueOf(fields[1]);
        String name = fields[2];
        Status status = Status.valueOf(fields[3]);
        String description = fields[4];

        switch (type) {
            case TASK:
                Task task = new Task(name, description);
                task.setId(id);
                task.setStatus(status);
                return task;
            case EPIC:
                Epic epic = new Epic(name, description);
                epic.setId(id);
                epic.setStatus(status);
                return epic;
            case SUBTASK:
                int epicId = Integer.parseInt(fields[5]);
                Subtask subtask = new Subtask(name, description, epicId);
                subtask.setId(id);
                subtask.setStatus(status);
                return subtask;
            default:
                throw new IllegalArgumentException("Неизвестный тип задачи: " + type);
        }
    }
}
