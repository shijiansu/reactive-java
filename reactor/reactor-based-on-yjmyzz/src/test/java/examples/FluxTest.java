package examples;

import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

public class FluxTest {

    @Test
    public void fluxJustTest() {
        Flux.just("1", "A", 3).subscribe(System.out::println);
    }

    @Test
    public void fluxIntervalTest() throws InterruptedException {
        // send after duration
        Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).subscribe(System.out::println);
        Thread.sleep(5000); // total 10 numbers = 5000 / 500
    }

    @Test
    public void fluxEmptyTest() {
        // do nothing
        Flux.empty().subscribe(System.out::println);
    }

    @Test
    public void fluxErrorTest() {
        Flux.error(new Exception("a wo,something is wrong!")).subscribe(System.out::println);
    }

    @Test
    public void fluxGenerateTest() {
        Flux.generate(i -> { // i is SynchronousSink
            i.next("AAAAA");
            // java.lang.IllegalStateException: More than one call to onNext
            // i.next("BBBBB"); // 注意generate中next只能调用1次
            i.complete(); // must call complete
        }).subscribe(System.out::println);
    }

    @Test
    public void fluxGenerateFromFunctionTest() {
        final Random random = new Random();
        Flux.generate(ArrayList::new, (list, item) -> {
            Integer value = random.nextInt(100);
            list.add(value);
            item.next(value);

            if (list.size() == 10) {
                item.complete();
            }
            return list;
        }).subscribe(System.out::println);
    }

    @Test
    public void fluxCreateTest() {
        Flux.create(i -> { // create is allowed to accept multiple .next()
            i.next("A");
            i.next("B");
            i.complete();
        }).subscribe(System.out::println);
    }

    @Test
    public void fluxCreate2Test() {
        Flux.create(item -> {
            for (int i = 0; i < 10; i++) {
                item.next(i);
            }
        }).subscribe(System.out::println);
    }

    @Test
    public void fluxBufferByCountTest() {
        // 第1段的意思是，0-9这10个数字，每次缓存3个，等3个数攒齐后，才输出
        Flux.range(0, 10).buffer(3).subscribe(System.out::println);
    }

    @Test
    public void fluxBufferByTimeTest() throws InterruptedException {
        // flux每隔1秒，产生1个递增数字，而缓冲区每3秒才算充满，相当于每凑足2个数字后，才输出
        Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
                .bufferTimeout(3, Duration.of(2, ChronoUnit.SECONDS))
                .subscribe(System.out::println);
        Thread.sleep(12000); // total 12 numbers
    }

    @Test
    public void fluxFilterTest() {
        Flux.range(0, 10).filter(c -> c % 2 == 0).subscribe(System.out::println);
    }

    @Test
    public void fluxZipTest() {
        Flux.just("A", "B").zipWith(Flux.just("1", "2", "3")).subscribe(System.out::println);
    }

    @Test
    public void fluxTakeTest() {
        Flux.range(1, 10).take(3).subscribe(System.out::println);

        System.out.println("--------------");
        Flux.range(1, 10).takeLast(3).subscribe(System.out::println);

        System.out.println("--------------");
        // takeWhile 是先判断条件是否成立，然后再决定是否取元素（换言之，如果一开始条件不成立，就直接终止了）；
        Flux.range(1, 10).takeWhile(c -> c > 1 && c < 5).subscribe(System.out::println);

        System.out.println("--------------");
        // takeUntil 是先取元素，直到遇到条件成立，才停下. 1不符合, 所以取2; 2符合, 所以停止, 同时也取出来2
        Flux.range(1, 10).takeUntil(c -> c > 1 && c < 5).subscribe(System.out::println);

        System.out.println("--------------");
        // takeUntilOther 则是先取元素，直到别一个Flux序列产生元素
        Flux.range(1, 4).takeUntilOther(Flux.never()).subscribe(System.out::println);
    }

    @Test
    public void reduceTest() {
        Flux.range(1, 10).reduce((x, y) -> x + y).subscribe(System.out::println);
        // starting value == 10
        Flux.range(1, 10).reduceWith(() -> 10, (x, y) -> x + y).subscribe(System.out::println);
    }

    @Test
    public void mergeTest() {
        // merge就是将把多个Flux"按元素实际产生的顺序"合并
        Flux.merge(
                Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).take(5),
                // delay 0.6s
                Flux.interval(Duration.of(600, ChronoUnit.MILLIS), Duration.of(500, ChronoUnit.MILLIS)).take(5)
        ).toStream().forEach(System.out::println);
    }

    @Test
    public void mergeSequentialTest() {
        // mergeSequential则是按多个Flux"被订阅的顺序"来合并
        Flux.mergeSequential(
                Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).take(5),
                // delay 0.6s
                Flux.interval(Duration.of(600, ChronoUnit.MILLIS), Duration.of(500, ChronoUnit.MILLIS)).take(5)
        ).toStream().forEach(System.out::println);
    }

    @Test
    public void combineLatestTest() {
        Flux.combineLatest(
                Arrays::toString,
                Flux.interval(Duration.of(10, ChronoUnit.SECONDS)).take(3),
                Flux.just("A", "B")// complete first, last element is B
        ).toStream().forEach(System.out::println);

        System.out.println("------------------");

        Flux.combineLatest(
                Arrays::toString,
                Flux.just(0, 1), // complete first, last element is 1
                Flux.just("A", "B")
        ).toStream().forEach(System.out::println);

        System.out.println("------------------");

        Flux.combineLatest(
                Arrays::toString,
                Flux.interval(Duration.of(1, ChronoUnit.SECONDS)).take(2), // complete first, last element is 1
                Flux.interval(Duration.of(10, ChronoUnit.SECONDS)).take(2)
        ).toStream().forEach(System.out::println);
    }

    @Test
    public void firstTest() {
        Flux.first(
                Flux.fromArray(new String[]{"A", "B"}), // complete first, take this Flux
                Flux.just(1, 2, 3)
        ).subscribe(System.out::println);
    }

    @Test
    public void mapTest() {
        Flux.just('A', 'B', 'C').map(a -> (int) (a)).subscribe(System.out::println);
    }

    @Test
    public void subscribeTest() {
        Flux.just("A", "B", "C")
                .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
                .concatWith(Mono.just("D")) // it will stop as there is a exception
                .subscribe(System.out::println, System.err::println);
    }

    @Test
    public void onErrorReturnTest() {
        Flux.just("A", "B", "C")
                .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
                .concatWith(Mono.just("D")) // it will stop as there is a exception
                .onErrorReturn("X") // the exception then return "X"
                .subscribe(System.out::println, System.err::println);
    }

    @Test
    public void onErrorResumeTest() {
        Flux.just("A", "B", "C")
                .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
                .onErrorResume(e -> {
                    // still able to do more actions
                    if (e instanceof IndexOutOfBoundsException) {
                        return Flux.just("X", "Y", "Z");
                    } else {
                        return Mono.empty();
                    }
                })
                .subscribe(System.out::println, System.err::println);
    }

    @Test
    public void retryTest() {
        Flux.just("A", "B", "C")
                .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
                .retry(1) // retry 1 more time before throw actual exception
                .subscribe(System.out::println, System.err::println);
    }

    @Test
    public void schedulesTest() {
        // 6.1 Schedulers.immediate() - 使用当前线程
        // 6.2 Schedulers.elastic() - 使用线程池
        // 6.3 Schedulers.newElastic("test1") - 使用(新)线程池(可以指定名称，更方便调试)
        // 6.4 Schedulers.single() - 单个线程
        // 6.5 Schedulers.newSingle("test2") - (新)单个线程(可以指定名称，更方便调试)
        // 6.6 Schedulers.parallel() - 使用并行处理的线程池（取决于CPU核数)
        // 6.7 Schedulers.newParallel("test3")  - 使用并行处理的线程池（取决于CPU核数，可以指定名称，方便调试)
        // 6.8 Schedulers.fromExecutorService(Executors.newScheduledThreadPool(5)) - 使用Executor（这个最灵活)
        Flux.fromArray(new String[]{"A", "B", "C", "D"})
                .publishOn(Schedulers.newSingle("TEST-SINGLE", true))
                .map(x -> String.format("[%s]: %s", Thread.currentThread().getName(), x))
                .toStream()
                .forEach(System.out::println);
    }

    @Test
    public void stepTest() {
        StepVerifier.create(Flux.just(1, 2)
                .concatWith(Mono.error(new IndexOutOfBoundsException("test")))
                .onErrorReturn(3))
                .expectNext(1)
                .expectNext(2)
                .expectNext(3)
                .verifyComplete();
    }

    @Test
    public void stepTest2() {
        // mock interval
        StepVerifier.withVirtualTime(() -> Flux.interval(Duration.of(10, ChronoUnit.MINUTES),
                Duration.of(5, ChronoUnit.SECONDS))
                .take(2))
                // 期待流被订阅，然后
                .expectSubscription()
                // 期望10分钟内，无任何事件（即：验证Flux先暂停10分钟），然后
                .expectNoEvent(Duration.of(10, ChronoUnit.MINUTES))
                // 等5秒钟，这时已经生成了数字0
                .thenAwait(Duration.of(5, ChronoUnit.SECONDS))
                // 期待0L
                .expectNext(0L)
                .thenAwait(Duration.of(5, ChronoUnit.SECONDS))
                .expectNext(1L)
                .verifyComplete();
    }

    @Test
    public void publisherTest() {
        Flux.just(1, 0)
                .map(c -> 1 / c)
                .log("MY-TEST")
                .subscribe(System.out::println);
    }

    @Test
    public void publisherCheckpointTest() {
        Flux.just(1, 0)
                .map(c -> 1 / c)
                .checkpoint("AAA")
                .subscribe(System.out::println);
    }
}
