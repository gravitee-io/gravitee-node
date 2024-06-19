package io.gravitee.node.container.spring.env;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.functions.Supplier;
import io.reactivex.rxjava3.schedulers.TestScheduler;
import io.reactivex.rxjava3.subscribers.TestSubscriber;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class WatchablePropertyResolverSubscriberTest {

    @Test
    public void shouldRepeatFlowableWhenItsCompleted() {
        int secondsDelay = 1;
        TestScheduler testScheduler = new TestScheduler();

        Supplier<Flowable<String>> flowableSupplier = () -> Flowable.just("1", "2");
        TestSubscriber<String> test = new AbstractGraviteePropertySource.FlowableRepeater<>(flowableSupplier)
            .repeatFlowable(secondsDelay, testScheduler)
            .test();

        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS);
        test.assertNotComplete();
        test.assertValueCount(10);
        testScheduler.advanceTimeBy(3, TimeUnit.SECONDS);
        test.assertValueCount(16);
        test.assertNotComplete();
    }
}
