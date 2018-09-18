package com.github.elo.rate.limiting.algo;

import com.github.elo.rate.limiting.LimiterResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindow<T> implements RateLimiter<T> {

    private Map<T, LinkedBlockingDeque<Chunk>> slidingWindow = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration duration;
    private final Duration chunkDuration;
    private final int unit;

    private ReentrantLock lock = new ReentrantLock();

    public SlidingWindow(int limit, Duration duration, Duration chunkDuration) {
        if(duration.compareTo(chunkDuration)<0)
            throw new IllegalArgumentException("chunk duration cannot be less than limiter duration");
        this.limit = limit;
        this.duration = duration;
        this.chunkDuration = chunkDuration;
        this.unit = Math.toIntExact(duration.getSeconds() / chunkDuration.getSeconds());

    }

    @Override
    public LimiterResponse<T> accept(T partnerId) {
        LocalDateTime requestTime = LocalDateTime.now();
        while (slidingWindow.get(partnerId) == null) {
            LinkedBlockingDeque<Chunk> inMapValue
                    = slidingWindow.putIfAbsent(partnerId, new LinkedBlockingDeque<>(unit));
            if (inMapValue == null) {
                return new LimiterResponse<>(slidingWindow.get(partnerId).offer(new Chunk(requestTime, 1)), partnerId, requestTime);
            }
        }
        lock.lock();
        try {
            LinkedBlockingDeque<Chunk> chunks = slidingWindow.get(partnerId);
            while (chunks.peek() == null) {
                //waiting while the first request add itself to the queue
            }
            while (slidingWindow.get(partnerId).peek() != null) {
                Chunk firstChunk = slidingWindow.get(partnerId).peek();
                if (firstChunk.chunkTime.plus(duration).isAfter(requestTime)) {
                    long counter = slidingWindow
                            .get(partnerId)
                            .stream()
                            .mapToLong(chunk -> chunk.counter)
                            .sum();
                    if (counter >= limit)
                        return new LimiterResponse<>(false, partnerId, requestTime);

                    slidingWindow.compute(partnerId, (k, v) -> {
                        if (v.getLast().chunkTime.plus(chunkDuration).isAfter(requestTime)) {
                            slidingWindow.get(partnerId).getLast().counter++;
                        } else {
                            slidingWindow.get(partnerId).offer(new Chunk(requestTime, 1));
                        }
                        return v;
                    });
                    return new LimiterResponse<>(true, partnerId, requestTime);
                } else {
                    slidingWindow.get(partnerId).poll();
                }
            }
            return new LimiterResponse<>(true, partnerId, requestTime);
        } finally {
            lock.unlock();
        }
    }

    private static class Chunk {
        private LocalDateTime chunkTime;
        private int counter;

        Chunk(LocalDateTime chunkTime, int counter) {
            this.chunkTime = chunkTime;
            this.counter = counter;
        }
    }
}
