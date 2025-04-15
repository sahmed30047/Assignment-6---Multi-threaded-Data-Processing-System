import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The Main class sets up the task processing system:
 * - It creates a BlockingQueue to hold tasks.
 * - It enqueues tasks along with special termination signals (poison pills) for the workers.
 * - It starts worker threads and waits for them to finish.
 * - Finally, it writes the results to a file.
 */
public class Main {
    // Define the poison pill value that signals no more tasks.
    public static final int POISON_PILL = -1;
    
    public static void main(String[] args) {
        // Create a thread-safe task queue using a BlockingQueue.
        BlockingQueue<Integer> taskQueue = new LinkedBlockingQueue<>();
        // Use a regular list for results and synchronize access when needed.
        List<String> results = new ArrayList<>();

        // Enqueue 20 tasks (task identifiers 1 to 20).
        for (int i = 1; i <= 20; i++) {
            taskQueue.add(i);
        }

        // We plan to start 5 worker threads.
        int numberOfWorkers = 5;
        // Add one poison pill per worker to signal that no more tasks will be added.
        for (int i = 0; i < numberOfWorkers; i++) {
            taskQueue.add(POISON_PILL);
        }

        // Start 5 worker threads.
        Thread[] workers = new Thread[numberOfWorkers];
        for (int i = 0; i < numberOfWorkers; i++) {
            workers[i] = new Thread(new Worker(i + 1, taskQueue, results));
            workers[i].start();
        }

        // Wait for all workers to complete their tasks.
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.err.println("Error waiting for threads to finish: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        // Write the task results to a file using try-with-resources for safe file handling.
        try (PrintWriter writer = new PrintWriter("results.txt")) {
            // Synchronize on the results list to prevent concurrent modifications.
            synchronized (results) {
                for (String result : results) {
                    writer.println(result);
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing results to file: " + e.getMessage());
        }

        System.out.println("All tasks completed.");
    }
}

/**
 * The Worker class implements Runnable to process tasks from the BlockingQueue.
 * Each worker:
 * - Retrieves tasks from the queue (blocking if no task is available).
 * - Processes the task (simulated with a sleep call).
 * - Writes the result to a shared results list in a thread-safe manner.
 * - Exits when it retrieves a poison pill.
 */
class Worker implements Runnable {
    private final int id;
    private final BlockingQueue<Integer> taskQueue;
    private final List<String> results;

    /**
     * Constructor to initialize a Worker.
     *
     * @param id        Unique worker identifier.
     * @param taskQueue The shared BlockingQueue from which tasks are retrieved.
     * @param results   The shared list to store task result strings.
     */
    public Worker(int id, BlockingQueue<Integer> taskQueue, List<String> results) {
        this.id = id;
        this.taskQueue = taskQueue;
        this.results = results;
    }

    @Override
    public void run() {
        try {
            while (true) {
                // Retrieve a task from the queue; blocks if no task is available.
                int task = taskQueue.take();

                // If the task is the poison pill, exit the loop.
                if (task == Main.POISON_PILL) {
                    break;
                }

                System.out.println("Worker " + id + " started task " + task);

                // Simulate task processing delay (e.g., 1 second).
                Thread.sleep(1000);

                // Create a string describing the completed task.
                String result = "Worker " + id + " completed task " + task;

                // Safely add the result to the shared collection.
                synchronized (results) {
                    results.add(result);
                }

                System.out.println(result);
            }
        } catch (InterruptedException e) {
            // Restore the interruption status and log the error if interrupted.
            System.err.println("Worker " + id + " was interrupted.");
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Worker " + id + " encountered an error: " + e.getMessage());
        }
    }
}
