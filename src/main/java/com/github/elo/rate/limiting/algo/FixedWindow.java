package com.github.elo.rate.limiting.algo;

import com.github.elo.rate.limiting.LimiterResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class FixedWindow<T> implements RateLimiter<T> {

    private Map<T, Window> counter = new ConcurrentHashMap<>();
    private final int limit;
    private final Duration duration;

    private ReentrantLock lock = new ReentrantLock();

    public FixedWindow(int limit, Duration duration) {
        this.limit = limit;
        this.duration = duration;
    }


    @Override
    public LimiterResponse<T> accept(T partner) {
        LocalDateTime requestTime = LocalDateTime.now();
        while (counter.get(partner) == null) {
            Window firstRequest = new Window(requestTime, 1);
            Window inMapValue = counter.putIfAbsent(partner, firstRequest);
            if (inMapValue == null) {
                return new LimiterResponse<>(true, partner, requestTime);
            }
        }
        lock.lock();
        try {
            counter.compute(partner, (k, v) -> {
                //check that we still need to increment counter
                if (v.counter > limit)
                    return v;

                if (v.startTime.plus(duration).isAfter(requestTime)) {
                    v.counter++;
                } else {
                    return new Window(requestTime, 1);
                }
                return v;
            });
            Window window = counter.get(partner);
            return new LimiterResponse<>(window.counter <= limit, partner, requestTime);
        } finally {
            lock.unlock();
        }
    }

    public static class Window {
        private LocalDateTime startTime;
        private int counter;

        Window(LocalDateTime startTime, int counter) {
            this.startTime = startTime;
            this.counter = counter;
        }
    }
}
