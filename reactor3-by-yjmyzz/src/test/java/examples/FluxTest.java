package examples;

import static java.lang.System.err;
import static java.lang.System.out;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

public class FluxTest {
  // 如何阅读Reactor的Java doc.
  // 1,2,3...这些颜色各异的小圆，代表正常发射出来的数据；(对应onNext方法)
  // 上右黑色的竖线表示发送完成；(对应onComplete方法)
  // 如果发射过程中出现异常，竖线用大红叉叉表示;（对应onError方法）

  @Test
  public void fluxJust() { // fromArray, fromIterable, range
    Flux.just("1", "A", 3).subscribe(out::println);
  }

  @Test
  public void fluxInterval() throws InterruptedException {
    // send after duration
    Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).subscribe(out::println);
    Thread.sleep(5000); // total 10 numbers = 5000 / 500
  }

  @Test
  public void fluxEmpty() { // empty方法几乎啥都不干，就发一个结束消息完事
    // do nothing
    Flux.empty().subscribe(out::println);
  }

  @Test
  public void fluxNever() { // empty里面至少还有一个结束消息, 而never则是真的啥都没有
    Flux.never().subscribe(out::println);
  }

  @Test
  public void fluxError() {
    Assertions.assertThrows(Exception.class,
        () -> Flux.error(new Exception("a wo,something is wrong!")).subscribe(out::println));
  }

  @Test
  public void fluxGenerate() {
    // 前面的几个方法，开发者不用显式的调用complete，而generate则需要调用，否则序列就不会终止
    Flux.generate(i -> { // i is SynchronousSink
      i.next("AAAAA");
      // java.lang.IllegalStateException: More than one call to onNext
      // i.next("BBBBB"); // 注意generate中next只能调用1次
      i.complete(); // must call complete
    }).subscribe(out::println);
  }

  @Test
  public void fluxGenerateFromFunction() {
    final Random random = new Random();
    Flux.generate(ArrayList::new, (list, item) -> {
      Integer value = random.nextInt(100);
      list.add(value);
      item.next(value);
      if (list.size() == 10) {
        item.complete();
      }
      return list;
    }).subscribe(out::println);
  }

  @Test
  public void fluxCreate() {
    // create方法则没有next的调用次数限制
    Flux.create(i -> { // create is allowed to accept multiple .next()
      i.next("A");
      i.next("B");
      i.complete();
    }).subscribe(out::println);
  }

  @Test
  public void fluxCreate2() {
    Flux.create(item -> {
      for (int i = 0; i < 10; i++) {
        item.next(i);
      }
    }).subscribe(out::println);
  }

  @Test
  public void fluxBufferByCount() {
    // 第1段的意思是，0-9这10个数字，每次缓存3个，等3个数攒齐后，才输出
    Flux.range(0, 10).buffer(3).subscribe(out::println);
    //    [0, 1, 2]
    //    [3, 4, 5]
    //    [6, 7, 8]
    //    [9]
  }

  @Test
  public void fluxBufferByTime() throws InterruptedException {
    // flux每隔1秒，产生1个递增数字，而缓冲区每3秒才算充满，相当于每凑足2个数字后，才输出
    Flux.interval(Duration.of(1, ChronoUnit.SECONDS))
        .bufferTimeout(3, Duration.of(2, ChronoUnit.SECONDS))
        .subscribe(out::println);
    Thread.sleep(12000);
    //    [0, 1, 2]
    //    [3, 4, 5]
    //    [6, 7, 8]
    //    [9, 10]
  }

  @Test
  public void fluxFilter() {
    Flux.range(0, 10).filter(c -> c % 2 == 0).subscribe(out::println);
  }

  @Test
  public void fluxZip() {
    // 这里有一个木桶原则，即 元素最少的"组"，决定了最后输出的"组"个数。
    Flux.just("A", "B").zipWith(Flux.just("1", "2", "3")).subscribe(out::println);
//    [A,1]
//    [B,2]
  }

  @Test
  public void fluxTake() {
    Flux.range(1, 10).take(3).subscribe(out::println);

    out.println("--------------");
    Flux.range(1, 10).takeLast(3).subscribe(out::println);

    out.println("--------------");
    // takeWhile 是先判断条件是否成立，然后再决定是否取元素（换言之，如果一开始条件不成立，就直接终止了）；
    Flux.range(1, 10).takeWhile(c -> c > 1 && c < 5).subscribe(out::println);

    out.println("--------------");
    // takeUntil 是先取元素，直到遇到条件成立，才停下. 1不符合, 所以取2; 2符合, 所以停止, 同时也取出来2
    Flux.range(1, 10).takeUntil(c -> c > 1 && c < 5).subscribe(out::println);

    out.println("--------------");
    // takeUntilOther 则是先取元素，直到别一个Flux序列产生元素
    Flux.range(1, 4).takeUntilOther(Flux.never()).subscribe(out::println);
  }

  @Test
  public void reduce() {
    // reduce相当于把1到10累加求和，reduceWith则是先指定一个起始值，然后在这个起始值基础上再累加。（tips: 除了累加，还可以做阶乘）
    Flux.range(1, 10).reduce((x, y) -> x + y).subscribe(out::println);
    Flux.range(1, 10).reduce(Integer::sum).subscribe(out::println);

    // starting value == 10
    Flux.range(1, 10).reduceWith(() -> 10, (x, y) -> x + y).subscribe(out::println);
    Flux.range(1, 10).reduceWith(() -> 10, Integer::sum).subscribe(out::println);
  }

  @Test
  public void merge() {
    // merge就是将把多个Flux"按元素实际产生的顺序"合并
    Flux.merge(
        // 交错
        Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).take(5),
        // delay 0.6s
        Flux.interval(Duration.of(1000, ChronoUnit.MILLIS), Duration.of(500, ChronoUnit.MILLIS))
            .take(5)
    ).toStream().forEach(out::println);
  }

  @Test
  public void mergeSequential() {
    // mergeSequential则是按多个Flux"被订阅的顺序"来合并
    Flux.mergeSequential(
        // 这10个先
        Flux.interval(Duration.of(1000, ChronoUnit.MILLIS), Duration.of(500, ChronoUnit.MILLIS))
            .take(10),
        Flux.interval(Duration.of(500, ChronoUnit.MILLIS)).take(5)
    ).toStream().forEach(out::println);
  }

  @Test
  public void combineLatest() {
    // 该操作会将所有流中的最新产生的元素合并成一个新的元素，作为返回结果流中的元素。
    // 只要其中任何一个流中产生了新的元素，合并操作就会被执行一次。
    Flux.combineLatest(
        Arrays::toString,
        Flux.interval(Duration.of(10, ChronoUnit.SECONDS)).take(3),
        Flux.just("A", "B")// 首先完成, 最后的元素是B, 所以都是[x, B]
    ).toStream().forEach(out::println);
    //[0, B]
    //[1, B]
    //[2, B]
    out.println("------------------");

    Flux.combineLatest(
        Arrays::toString,
        Flux.just(0, 1), // 首先完成, 最后的元素是1, 所以都是[1, x]
        Flux.just("A", "B")
    ).toStream().forEach(out::println);
    //[1, A]
    //[1, B]
    out.println("------------------");

    Flux.combineLatest(
        Arrays::toString,
        Flux.interval(Duration.of(1, ChronoUnit.SECONDS)).take(2),
        // 首先完成
        Flux.interval(Duration.of(10, ChronoUnit.SECONDS)).take(2)
    ).toStream().forEach(out::println);
    //[1, 0]
    //[1, 1]
  }

  @Test
  public void first() {
    Flux.first(
        Flux.fromArray(new String[]{"A", "B"}), // 只取第1个Flux的元素
        Flux.just(1, 2, 3)
    ).subscribe(out::println);
    // A
    // B
  }

  @Test
  public void map() {
    // map相当于把一种类型的元素序列，转换成另一种类型
    Flux.just('A', 'B', 'C').map(a -> (int) (a)).subscribe(out::println);
  }

  @Test
  public void subscribe() {
    Flux.just("A", "B", "C")
        .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
        .concatWith(Mono.just("D")) // it will stop as there is a exception
        .subscribe(out::println, err::println);
  }

  @Test
  public void onErrorReturn() {
    Flux.just("A", "B", "C")
        // 异常并停止
        .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
        .concatWith(Mono.just("D")) // it will stop as there is a exception
        .onErrorReturn("X") // the exception then return "X"
        .subscribe(out::println, err::println); // 这样不会输出错误信息
    // A
    // B
    // C
    // X
  }

  @Test
  public void onErrorResume() {
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
        .subscribe(out::println, err::println);
    // A
    // B
    // C
    // X
    // Y
    // Z
  }

  @Test
  public void retry() {
    Flux.just("A", "B", "C")
        .concatWith(Flux.error(new IndexOutOfBoundsException("下标越界啦！")))
        .retry(1) // retry 1 more time before throw actual exception
        .subscribe(out::println, err::println); // 会输出错误信息
  }

  @Test
  public void schedules() { // Schedulers提供了如下几种调用策略
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
        .forEach(out::println);
  }

  @Test
  public void step() { // 异步处理，通常是比较难测试的，reactor提供了StepVerifier工具来进行测试
    StepVerifier.create(Flux.just(1, 2)
        .concatWith(Mono.error(new IndexOutOfBoundsException("test")))
        .onErrorReturn(3))
        .expectNext(1)
        .expectNext(2)
        .expectNext(3)
        .verifyComplete();
  }

  @Test
  public void stepTest2() { // 模拟时间流逝
    // Flux.interval这类延时操作，如果延时较大，比如几个小时之类的，要真实模拟的话，效率很低，
    // StepVerifier提供了withVirtualTime方法，来模拟加快时间的流逝
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
  public void publisher() { // 记录日志
    Flux.just(1, 0)
        .map(c -> 1 / c)
        .log("log-category")
        .subscribe(out::println);
  }

  @Test
  public void publisherCheckpoint() {
    Flux.just(1, 0)
        .map(c -> 1 / c)
        .checkpoint("XXXXXXXXXXXXXXXXXXXXXXXXXX") // 好似这个String没啥用...
        .subscribe(out::println);
  }
}
