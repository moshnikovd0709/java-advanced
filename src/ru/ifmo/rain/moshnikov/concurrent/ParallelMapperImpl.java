package ru.ifmo.rain.moshnikov.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


public class ParallelMapperImpl implements ParallelMapper {
    private final LinkedList<Task<?, ?>> tasks;
    private final List<Thread> threads;
    private boolean closed = false;

    /**
     * Thread count constructor. Create an instance of {@code ParallelMapperImpl}
     * with the indicated amount of {@code threads}.
     *
     * @param threads the number of the threads
     */
    public ParallelMapperImpl(final int threads) {
        tasks = new LinkedList<>();
        final Runnable runner = () -> {
            try {
                while (!Thread.interrupted()) {
                    nextSubTask().run();
                }
            } catch (final InterruptedException e) {
                e.printStackTrace();
            } finally {
                Thread.currentThread().interrupt();
            }
        };
        this.threads = Stream.generate(() -> new Thread(runner))
                .limit(threads)
                .collect(Collectors.toList());
        this.threads.forEach(Thread::start);
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @param f    the mapping function
     * @param args the elements to be processed
     * @param <T>  type of argument
     * @param <R>  type of resulting mapped arguments
     * @return a {@code List} of mapped arguments
     * @throws InterruptedException if calling thread was interrupted
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        if (args.isEmpty()) {
            return List.of();
        }
        final Task<T, R> task = new Task<>(f, args);
        synchronizedAdd(task);
        return task.awaitResult();
    }

    private void synchronizedAdd(final Task<?, ?> task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    private void synchronizedRemove() {
        synchronized(tasks) {
            tasks.remove();
        }
    }

    private Runnable nextSubTask() throws InterruptedException {
        synchronized(tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            final Task<?, ?> task = tasks.element();
            Runnable subTask = task.getTask();
            return () -> {
                subTask.run();
                task.taskFinished();
            };
        }
    }

    /**
     * Stops all working threads.
     */
    @Override
    public void close() {
        closed = true;
        threads.forEach(Thread::interrupt);
        joinThreads();
    }

    private void joinThreads() {
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
            } catch (final InterruptedException e) {
                e.printStackTrace();
                i--; // have to wait for current thread to finish
            }
        }
    }

    private class Task<T, R> {

        private final Queue<Runnable> seqTasks;
        private final ConcurrentList list;
        private boolean terminated = false;
        private int notFinished;
        private int notStarted;

        private Task(final Function<? super T, ? extends R> f, final List<? extends T> args) {
            seqTasks = new LinkedList<>();
            list = new ConcurrentList(args.size(), f);
            notFinished = notStarted = args.size();
            IntStream.range(0, args.size())
                    .forEach(i -> seqTasks.add(() -> list.set(i, args.get(i))));
        }

        private synchronized Runnable getTask() {
            Runnable subTask = seqTasks.remove();
            notStarted--;
            if (notStarted == 0) {
                synchronizedRemove();
            }
            return subTask;
        }

        private synchronized void taskFinished() {
            notFinished--;
            if (notFinished == 0) {
                terminate();
            }
        }

        private synchronized List<R> awaitResult() throws InterruptedException {
            while (!terminated && !closed) {
                wait();
            }
            if (!terminated) {
                terminate();
            }
            return list.get();
        }

        private synchronized void terminate() {
            terminated = true;
            notify();
        }

        private class ConcurrentList {

            private final List<R> result;
            private final Function<? super T, ? extends R> f;
            private RuntimeException re = null;

            private ConcurrentList(final int size, final Function<? super T, ? extends R> f) {
                result = new ArrayList<>(Collections.nCopies(size, null));
                this.f = f;
            }

            private void set(final int index, final T value) {
                try {
                    if (!terminated) {
                        setValue(index, f.apply(value));
                    }
                } catch (final RuntimeException e) {
                    addException(e);
                }
            }

            private synchronized void addException(RuntimeException e) {
                if (!terminated) {
                    if (re == null) {
                        re = e;
                    } else {
                        re.addSuppressed(e);
                    }
                }
            }

            private synchronized void setValue(int index, R value) {
                if (!terminated) {
                    result.set(index, value);
                }
            }

            private synchronized List<R> get() {
                if (re != null) {
                    throw re;
                }
                return result;
            }
        }
    }
}
