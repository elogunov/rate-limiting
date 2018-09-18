package com.github.elo.rate.limiting.algo;

import com.github.elo.rate.limiting.LimiterResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public abstract class RateLimiterTest {

    protected abstract RateLimiter<String> getInstance();

    @RepeatedTest(50)
    void acceptAll() throws ExecutionException, InterruptedException {
        RateLimiter<String> limiter = getInstance();
        ExecutorService service = Executors.newFixedThreadPool(100);
        List<Future<LimiterResponse>> testPartner = Stream.generate(() ->
                (Callable<LimiterResponse>) () -> limiter.accept("testPartner"))
                .limit(100).map(service::submit).collect(toList());


        for(Future cf: testPartner) {
            LimiterResponse o = (LimiterResponse) cf.get();
            Assertions.assertTrue(o.isAccept());
        }
    }

    @RepeatedTest(50)
    void acceptFirst100() throws ExecutionException, InterruptedException {
        RateLimiter<String> limiter = getInstance();
        ExecutorService service = Executors.newFixedThreadPool(200);
        List<Future<LimiterResponse>> testPartner = Stream.generate(() ->
                (Callable<LimiterResponse>) () -> limiter.accept("testPartner"))
                .limit(200).map(service::submit).collect(toList());


        int acceptedCounter = 0;
        for(Future cf: testPartner) {
            LimiterResponse o = (LimiterResponse) cf.get();
            if(o.isAccept()) acceptedCounter++;
        }
        Assertions.assertEquals(100, acceptedCounter);
    }

    @RepeatedTest(50)
    void multiplePartners() throws ExecutionException, InterruptedException {
        RateLimiter<String> limiter = getInstance();
        ExecutorService service = Executors.newFixedThreadPool(200);
        List<Callable<LimiterResponse>> partner1 = Stream.generate(() ->
                (Callable<LimiterResponse>) () -> limiter.accept("partner1"))
                .limit(60).collect(toList());

        List<Callable<LimiterResponse>> partner2 = Stream.generate(() ->
                (Callable<LimiterResponse>) () -> limiter.accept("partner2"))
                .limit(30).collect(toList());
        List<Callable<LimiterResponse>> partner3 = Stream.generate(() ->
                (Callable<LimiterResponse>) () -> limiter.accept("partner3"))
                .limit(110).collect(toList());

        partner1.addAll(partner2);
        partner1.addAll(partner3);
        List<Future<LimiterResponse>> futures = service.invokeAll(partner1);

        int acceptedCounter1 = 0;
        int acceptedCounter2 = 0;
        int acceptedCounter3 = 0;
        for(Future cf: futures) {
            LimiterResponse o = (LimiterResponse) cf.get();
            if(o.isAccept() && o.getPartner() == "partner1") acceptedCounter1++;
            if(o.isAccept() && o.getPartner() == "partner2") acceptedCounter2++;
            if(o.isAccept() && o.getPartner() == "partner3") acceptedCounter3++;
        }
        Assertions.assertEquals(60, acceptedCounter1);
        Assertions.assertEquals(30, acceptedCounter2);
        Assertions.assertEquals(100, acceptedCounter3);
    }

    /**
     * The test case shows the worst case for the FixedWindow algorithm,
     * when we can have doubled amount of accepted request.
     * The scenario is to have  the 1st request in the beginning of the interval
     * and all other accepted requests in the 2nd part of the duration interval.
     * Then have all except 1 request in the first half of the next interval.
     * All of them will be accepted by FixedWindow and declined by other algorithms.
     */
    @RepeatedTest(5)
    void worstCaseForFixedWindow() throws Exception {
        int acceptedCounter = getAcceptedCounter();
        Assertions.assertEquals(101, acceptedCounter);
    }

    protected int getAcceptedCounter() throws Exception {
        List<LimiterResponse<String>> call = worstCaseForFixedWindowCallable().call();
        int acceptedCounter = 0;
        for(LimiterResponse o: call) {
            if(o.isAccept()) acceptedCounter++;
        }
        return acceptedCounter;
    }

    private Callable<List<LimiterResponse<String>>> worstCaseForFixedWindowCallable() {
        RateLimiter<String> limiter = getInstance();
        Callable<List<LimiterResponse<String>>> postponed = () -> Stream.generate(() ->
                (Callable<LimiterResponse<String>>) () -> limiter.accept("partner1"))
                .limit(99).map(Executors.newFixedThreadPool(99)::submit).map(limiterResponseFuture -> {
                    try {
                        return limiterResponseFuture.get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new AssertionError(e);
                    }
                }).collect(toList());

        return () -> {
            LimiterResponse<String> current = limiter.accept("partner1");
            ScheduledFuture<List<LimiterResponse<String>>> schedule1 = Executors.newScheduledThreadPool(99)
                    .schedule(postponed, 5, TimeUnit.SECONDS);
            ScheduledFuture<List<LimiterResponse<String>>> schedule2 = Executors.newScheduledThreadPool(99)
                    .schedule(postponed, 10, TimeUnit.SECONDS);
            List<LimiterResponse<String>> limiterResponses2 = schedule2.get();
            List<LimiterResponse<String>> limiterResponses1 = schedule1.get();
            limiterResponses1.addAll(limiterResponses2);
            limiterResponses1.add(current);
            return limiterResponses1;
        };
    }
}
