package manager;

import model.Task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryHistoryManager implements HistoryManager {

    private static class Node {
        Task task;
        Node prev;
        Node next;

        public Node(Node prev, Task task, Node next){
            this.task = task;
            this.prev = prev;
            this.next = next;
        }
    }
    private Node head;
    private Node tail;

    private final Map<Integer, Node> history = new HashMap();

    private void linkLast(Task task) {
        Node newNode = new Node(tail, task, null);
        if (tail != null)
            tail.next = newNode;
        else
            head = newNode;
        
        tail = newNode;
        history.put(task.getId(), newNode);
    }

    private List<Task> getTasks() {
        List<Task> tasks = new ArrayList<>();
        Node current = head;
        while(current != null) {
            tasks.add(current.task);
            current = current.next;
        }
        return tasks;
    }

    @Override
    public void add(Task task) {
        if (task == null)
            return;

        if (history.containsKey(task.getId())) {
            remove(task.getId());
        }

        linkLast(task);
    }

    @Override
    public void remove(int id) {
        Node node = history.get(id);
        if (node != null) {
            removeNode(node);
            history.remove(id);
        }
    }

    private void removeNode(Node node) {
        if (node == null)
            return;

        if (node.prev != null)
            node.prev.next = node.next;
        else
            head = node.next;

        if (node.next != null)
            node.next.prev = node.prev;
        else
            tail = node.prev;
    }

    @Override
    public List<Task> getHistory() {
        return getTasks();
    }
}
