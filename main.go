package main

import (
	"fmt"
	"log"
	"os"
	"sync"
	"time"
)

// worker processes tasks received from the tasks channel and sends
// the outcome to the results channel. It logs its progress for debugging
// and demonstration purposes.
func worker(id int, tasks <-chan int, wg *sync.WaitGroup, results chan<- string) {
	// Ensure we signal completion of this goroutine when the function exits.
	defer wg.Done()
	
	// Process each task until the tasks channel is closed.
	for task := range tasks {
		// Log that this worker has started the task.
		log.Printf("Worker %d started task %d\n", id, task)
		
		// Simulate task processing delay.
		time.Sleep(1 * time.Second)
		
		// Create a result string for the completed task.
		result := fmt.Sprintf("Worker %d completed task %d", id, task)
		
		// Send the result to the results channel.
		results <- result
		
		// Log the completed task.
		log.Println(result)
	}
}

func main() {
	// Create a buffered channel for tasks.
	// Buffering helps avoid blocking the sender if workers are momentarily busy.
	tasks := make(chan int, 20)
	// Create a buffered channel for results.
	// The buffer size is set to the number of tasks to ensure smooth processing.
	results := make(chan string, 20)

	// WaitGroup to synchronize worker completion.
	var wg sync.WaitGroup

	// Open (or create) a log file to capture the program's logging output.
	logFile, err := os.Create("results.log")
	if err != nil {
		log.Fatalf("Failed to create log file: %v", err)
	}
	// Ensure that the log file is closed when main returns.
	defer logFile.Close()
	// Direct the log package to write to our created log file.
	log.SetOutput(logFile)

	// Enqueue tasks into the tasks channel.
	for i := 1; i <= 20; i++ {
		tasks <- i
	}
	// Close the tasks channel to signal that no more tasks will be added.
	close(tasks)

	// Start 5 worker goroutines to process tasks concurrently.
	for i := 1; i <= 5; i++ {
		wg.Add(1)
		go worker(i, tasks, &wg, results)
	}

	// Use a separate goroutine to close the results channel once all workers have finished.
	go func() {
		wg.Wait()
		close(results)
	}()

	// Create the final results output file.
	finalResultsFile, err := os.Create("final_results.txt")
	if err != nil {
		log.Printf("Error creating final results file: %v", err)
		return
	}
	// Ensure that we close the file once all results are written.
	defer finalResultsFile.Close()

	// Write the results from the results channel to the final results file.
	for res := range results {
		fmt.Fprintln(finalResultsFile, res)
	}

	// Inform the user that processing is complete.
	fmt.Println("All tasks completed. Check 'results.log' and 'final_results.txt'.")
}
