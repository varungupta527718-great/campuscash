package com.studentexpensetracker;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamTokenizer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Runnable examples of the Java concepts in the course outline. */
public final class JavaConceptWorkshop {
    private JavaConceptWorkshop() { }

    public static void run(List<Expense> records) {
        System.out.println("\n--- Java course concepts using expense records ---");
        stringAndArrayExamples();
        collectionExamples(records);
        sortingAndStreamExamples(records);
        exceptionExamples();
        testCaseExamples();
        threadExamples();
        fileAndObjectStreamExamples(records);
    }

    private static void stringAndArrayExamples() {
        String literal = "Food";
        String anotherLiteral = "Food";
        String separateObject = new String("Food");
        StringBuilder report = new StringBuilder("Category: ").append(literal).append("; length: ").append(literal.length());
        StringBuffer sharedText = new StringBuffer("Monthly report");
        sharedText.append(" - ").append(DateTools.monthName(DateTools.today()));
        int[][] first = {{1, 2}, {3, 4}};
        int[][] second = {{5, 6}, {7, 8}};
        int[][] sum = new int[2][2];
        int[][] product = new int[2][2];
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 2; col++) {
                sum[row][col] = first[row][col] + second[row][col];
                for (int common = 0; common < 2; common++) product[row][col] += first[row][common] * second[common][col];
            }
        }
        Integer wrapperCount = sum[0][0];
        System.out.println("String pool literal == literal: " + (literal == anotherLiteral));
        System.out.println("Separate String compares by content with equals(): " + literal.equals(separateObject));
        System.out.println(report + "; " + sharedText + "; wrapper Integer=" + wrapperCount);
        System.out.println("2D matrix addition:");
        printMatrix(sum);
        System.out.println("2D matrix multiplication:");
        printMatrix(product);
        System.out.println("String search/sub-string/concat: " + literal.contains("oo") + "; " + literal.substring(0, 2) + "; " + literal.concat(" category"));
    }

    private static void printMatrix(int[][] matrix) {
        for (int[] row : matrix) {
            for (int col = 0; col < row.length; col++) System.out.print((col == 0 ? "" : " ") + row[col]);
            System.out.println();
        }
    }

    private static void collectionExamples(List<Expense> records) {
        ArrayList<Expense> arrayRecords = new ArrayList<>(records);
        LinkedList<Expense> linkedRecords = new LinkedList<>(records);
        HashSet<String> anyOrder = new HashSet<>();
        LinkedHashSet<String> insertionOrder = new LinkedHashSet<>();
        TreeSet<String> sorted = new TreeSet<>();
        HashMap<String, Double> hashTotals = new HashMap<>();
        LinkedHashMap<String, Double> insertionTotals = new LinkedHashMap<>();
        TreeMap<String, Double> monthTotals = new TreeMap<>();
        Queue<Expense> queue = new ArrayDeque<>();
        Deque<Expense> deque = new ArrayDeque<>();
        PriorityQueue<Expense> largestFirst = new PriorityQueue<>(new Comparator<Expense>() {
            public int compare(Expense left, Expense right) { return Double.compare(right.getAmount(), left.getAmount()); }
        });
        for (Expense expense : records) {
            queue.offer(expense);
            deque.addLast(expense);
            largestFirst.offer(expense);
            if (!"Income".equals(expense.getType())) {
                String category = expense.getCategory();
                anyOrder.add(category);
                insertionOrder.add(category);
                sorted.add(category);
                addToTotal(hashTotals, category, expense.getAmount());
                addToTotal(insertionTotals, category, expense.getAmount());
                addToTotal(monthTotals, DateTools.monthKey(expense.getDate()), expense.getAmount());
            }
        }
        int count = 0;
        Iterator<Expense> iterator = arrayRecords.iterator();
        while (iterator.hasNext()) { iterator.next(); count++; }
        ListIterator<Expense> backwards = linkedRecords.listIterator(linkedRecords.size());
        if (backwards.hasPrevious()) backwards.previous();
        System.out.println("ArrayList rows=" + count + "; LinkedList rows=" + linkedRecords.size());
        System.out.println("HashSet=" + anyOrder + "; LinkedHashSet=" + insertionOrder + "; TreeSet=" + sorted);
        System.out.println("HashMap totals=" + hashTotals + "; LinkedHashMap totals=" + insertionTotals + "; TreeMap by month=" + monthTotals);
        System.out.println("Queue=" + queue.size() + "; Deque=" + deque.size() + "; largest transaction=" + (largestFirst.peek() == null ? "none" : largestFirst.peek().getTitle()));
    }

    private static void addToTotal(Map<String, Double> totals, String key, double amount) {
        Double old = totals.get(key);
        totals.put(key, (old == null ? 0 : old) + amount);
    }

    private static void sortingAndStreamExamples(List<Expense> records) {
        List<Expense> byDate = new ArrayList<>(records);
        Collections.sort(byDate); // Uses Expense.compareTo.
        List<Expense> byAmount = new ArrayList<>(records);
        Collections.sort(byAmount, new Comparator<Expense>() {
            public int compare(Expense left, Expense right) { return Double.compare(right.getAmount(), left.getAmount()); }
        });
        List<String> categories = records.stream()
                .filter(expense -> !"Income".equals(expense.getType()))
                .map(Expense::getCategory).distinct().sorted().skip(0).limit(6).collect(Collectors.toList());
        System.out.println("Comparable order by date: " + recordIds(byDate));
        System.out.println("Comparator order by amount: " + recordIds(byAmount));
        Expense sample = records.isEmpty() ? new PersonalExpense(1, "Practice", 1, "Practice", DateTools.today()) : records.get(0);
        System.out.println("Stream filter/map/distinct/sorted/skip/limit: " + categories);
        System.out.println("Abstract type and runtime object: " + sample.getType() + "; class static version=" + Expense.recordVersion());
        System.out.println("Generic first record: " + firstOrNull(records) + "; generic collection size=" + new RecordBox<>(records).size());
        List<String> sortedNames = new ArrayList<>();
        sortedNames.addAll(new TreeSet<>(Arrays.asList("Books", "Food", "Transport")));
        System.out.println("Bounded generic maximum / extends wildcard: " + maximum(sortedNames));
        if (!byAmount.isEmpty()) System.out.println("Collections.max by amount: " + Collections.max(byAmount, new Comparator<Expense>() {
            public int compare(Expense left, Expense right) { return Double.compare(left.getAmount(), right.getAmount()); }
        }).getTitle());
    }

    private static List<Long> recordIds(List<Expense> records) {
        List<Long> ids = new ArrayList<>();
        for (Expense expense : records) ids.add(expense.getId());
        return ids;
    }
    private static <T> T firstOrNull(List<T> values) { return values.isEmpty() ? null : values.get(0); }
    private static <T extends Comparable<? super T>> T maximum(List<? extends T> values) {
        T largest = null;
        for (T value : values) if (largest == null || value.compareTo(largest) > 0) largest = value;
        return largest;
    }
    private static final class RecordBox<T> {
        private final List<T> values;
        RecordBox(Collection<? extends T> items) { values = new ArrayList<>(items); }
        int size() { return values.size(); }
    }

    private static void exceptionExamples() {
        try { checkPositiveAmount(-1); }
        catch (IllegalArgumentException e) { System.out.println("Input validation caught an unchecked exception: " + e.getMessage()); }
        try { readExampleFile("missing-course-example.txt"); }
        catch (IOException e) { System.out.println("Checked exception can be caught or passed with throws: " + e.getClass().getSimpleName()); }
    }
    private static void checkPositiveAmount(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive.");
    }
    private static void readExampleFile(String fileName) throws IOException {
        try (FileReader reader = new FileReader(fileName)) { reader.read(); }
    }

    private static void testCaseExamples() {
        double[] noAmounts = {};
        double[] oneAmount = {12.50};
        double emptyResult = addAmounts(noAmounts);
        double oneResult = addAmounts(oneAmount);
        System.out.println("Test case: empty input; expected=0.00; actual=" + MoneyTools.format(emptyResult) + "; pass=" + (emptyResult == 0));
        System.out.println("Test case: one amount; expected=12.50; actual=" + MoneyTools.format(oneResult) + "; pass=" + (oneResult == 12.50));
    }

    private static double addAmounts(double[] amounts) {
        double total = 0;
        for (double amount : amounts) total += amount;
        return total;
    }

    private static void threadExamples() {
        System.out.println("A program is code; a process is a running program; a thread is one execution path inside it.");
        System.out.println("Multiprogramming lets the CPU switch among ready programs; multithreading gives one program multiple execution paths.");
        System.out.println("Concurrency overlaps tasks; parallelism runs work at the same time on separate processors.");
        Runnable work = new Runnable() { public void run() { System.out.println("Runnable task executed on " + Thread.currentThread().getName() + " in state " + Thread.currentThread().getState()); } };
        Thread task = new Thread(work, "tracker-demo");
        System.out.println("Thread before start: " + task.getState());
        work.run(); // run() calls the task here, on the current thread.
        task.start();
        try { task.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("Thread after join: " + task.getState());

        Thread timed = new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(10000); }
                catch (InterruptedException e) { System.out.println("Timed worker received interrupt()."); }
            }
        }, "timed-worker");
        timed.setDaemon(true);
        timed.setPriority(Thread.MIN_PRIORITY);
        System.out.println("Daemon worker priority=" + timed.getPriority() + "; before start=" + timed.getState());
        timed.start();
        waitForState(timed, Thread.State.TIMED_WAITING);
        System.out.println("Timed sleep state=" + timed.getState());
        timed.interrupt();
        try { timed.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Object lock = new Object();
        Thread blocked = new Thread(new Runnable() { public void run() { synchronized (lock) { System.out.println("Blocked worker acquired the monitor."); } } }, "blocked-worker");
        synchronized (lock) {
            blocked.start();
            waitForState(blocked, Thread.State.BLOCKED);
            System.out.println("Waiting for synchronized monitor state=" + blocked.getState());
        }
        try { blocked.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        Counter unsafe = new Counter();
        Runnable addMany = new Runnable() { public void run() { for (int i = 0; i < 20000; i++) unsafe.addOne(); } };
        Thread one = new Thread(addMany);
        Thread two = new Thread(addMany);
        one.start(); two.start();
        try { one.join(); two.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("Shared data needs synchronization; example counter=" + unsafe.getValue());
        SafeCounter safe = new SafeCounter();
        Runnable safeWork = new Runnable() { public void run() { for (int i = 0; i < 10000; i++) safe.addOne(); } };
        Thread first = new Thread(safeWork); Thread second = new Thread(safeWork);
        first.start(); second.start();
        try { first.join(); second.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        System.out.println("Synchronized counter result=" + safe.getValue() + " (expected 20000)");
        waitNotifyExample();
        notifyExample();
    }

    private static void waitNotifyExample() {
        MessageBox box = new MessageBox();
        Thread consumer = new Thread(new Runnable() { public void run() { System.out.println("Consumer received: " + box.take()); } });
        consumer.start();
        waitForState(consumer, Thread.State.WAITING);
        System.out.println("Guarded consumer waiting state=" + consumer.getState());
        box.put("producer supplied a report");
        try { consumer.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void notifyExample() {
        OneShotSignal signal = new OneShotSignal();
        Thread waiter = new Thread(new Runnable() { public void run() { signal.waitForSignal(); } });
        waiter.start();
        waitForState(waiter, Thread.State.WAITING);
        signal.sendSignal();
        try { waiter.join(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private static void waitForState(Thread thread, Thread.State wanted) {
        int attempts = 0;
        while (thread.getState() != wanted && thread.isAlive() && attempts < 1000) {
            try { Thread.sleep(1); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            attempts++;
        }
    }

    private static final class Counter { private int value; void addOne() { value++; } int getValue() { return value; } }
    private static final class SafeCounter { private int value; synchronized void addOne() { value++; } synchronized int getValue() { return value; } }
    private static final class MessageBox {
        private String message;
        private boolean ready;
        synchronized void put(String value) {
            message = value;
            ready = true;
            notifyAll();
        }
        synchronized String take() {
            while (!ready) try { wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return "interrupted"; }
            ready = false;
            return message;
        }
    }
    private static final class OneShotSignal {
        private boolean signaled;
        synchronized void waitForSignal() {
            while (!signaled) try { wait(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
            System.out.println("Single waiting thread released by notify().");
        }
        synchronized void sendSignal() { signaled = true; notify(); }
    }

    private static void fileAndObjectStreamExamples(List<Expense> records) {
        File folder = new File("data/course-demo");
        if (!folder.exists() && !folder.mkdirs()) { System.out.println("Could not create the course demo folder."); return; }
        File report = new File(folder, "report.txt");
        File savedRecords = new File(folder, "transactions.bin");
        File copiedRecords = new File(folder, "transactions-copy.bin");
        try {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(report))) {
                writer.write("Transaction count: " + records.size());
            }
            try (BufferedReader reader = new BufferedReader(new FileReader(report))) {
                StreamTokenizer tokenizer = new StreamTokenizer(reader);
                while (tokenizer.nextToken() != StreamTokenizer.TT_EOF) {
                    if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) System.out.println("StreamTokenizer number=" + tokenizer.nval);
                }
            }
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(savedRecords))) {
                output.writeObject(new ArrayList<>(records));
            }
            copyBytes(savedRecords, copiedRecords);
            try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(copiedRecords))) {
                Object restored = input.readObject();
                if (restored instanceof List<?>) System.out.println("Object serialization restored transaction count=" + ((List<?>) restored).size());
            }
            System.out.println("FileReader/Writer, byte streams, tokenizer and serialization examples saved under " + folder.getPath());
        } catch (IOException | ClassNotFoundException e) { System.out.println("File example failed: " + e.getMessage()); }
    }

    private static void copyBytes(File source, File destination) throws IOException {
        try (FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(destination)) {
            int nextByte;
            while ((nextByte = input.read()) != -1) output.write(nextByte);
        }
    }
}
