package com.github.elo.rate.limiting.algo;

import com.github.elo.rate.limiting.LimiterResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingLog<T> implements RateLimiter<T> {

    private Map<T,LinkedBlockingQueue<LocalDateTime>> slidingLog = new ConcurrentHashMap<>();

    private final int limit;
    private final Duration duration;

    private ReentrantLock lock = new ReentrantLock();

    public SlidingLog(int limit, Duration duration) {
        this.limit = limit;
        this.duration = duration;
    }

    @Override
    public LimiterResponse<T> accept(T partner) {
        LocalDateTime requestTime = LocalDateTime.now();
        while (slidingLog.get(partner) == null) {
            LinkedBlockingQueue<LocalDateTime> inMapValue
                    = slidingLog.putIfAbsent(partner, new LinkedBlockingQueue<>(limit));
            if (inMapValue == null) {
                return new LimiterResponse<>(slidingLog.get(partner).offer(requestTime), partner, requestTime);
            }
        }
        lock.lock();
        try {
            LinkedBlockingQueue<LocalDateTime> localDateTimes = slidingLog.get(partner);
            while (localDateTimes.peek() == null) {
                //waiting while the first request add itself to the queue
            }
            while (localDateTimes.peek() != null) {
                LocalDateTime startTime = slidingLog.get(partner).peek();
                if (startTime.plus(duration).isAfter(requestTime)) {
                    return new LimiterResponse<>(slidingLog.get(partner).offer(requestTime), partner, requestTime);
                } else {
                    slidingLog.get(partner).poll();
                }
            }
            return new LimiterResponse<>(slidingLog.get(partner).offer(requestTime), partner, requestTime);
        } finally {
            lock.unlock();
        }
    }
}
