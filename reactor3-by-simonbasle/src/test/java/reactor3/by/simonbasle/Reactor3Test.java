package reactor3.by.simonbasle;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@Slf4j
public class Reactor3Test {

  private static List<String> words =
      Arrays.asList("the", "quick", "brown", "fox", "jumped", "over", "the", "lazy", "dog");

  @Test
  public void _1_1_words() {
    Flux<String> fewWords = Flux.just("Hello", "World");
    Flux<String> manyWords = Flux.fromIterable(words);

    fewWords.subscribe(log::info); // [main] thread
    log.info("");
    manyWords.subscribe(log::info); // [main] thread
  }

  @Test
  public void _1_2_missing_letters() {
    Flux<String> manyLetters =
        Flux.fromIterable(words)
            .flatMap(word -> Flux.fromArray(word.split("")))
            .distinct()
            .sort()
            .zipWith(
                Flux.range(1, Integer.MAX_VALUE),
                (string, count) -> String.format("%2d. %s", count, string));
    manyLetters.subscribe(log::info);
  }

  @Test
  public void _1_3_letters() {
    Mono<String> s = Mono.just("s");
    Flux<String> manyLetters =
        Flux.fromIterable(words)
            .flatMap(word -> Flux.fromArray(word.split("")))
            .concatWith(s) // add "s"
            .distinct()
            .sort()
            .zipWith(
                Flux.range(1, Integer.MAX_VALUE),
                (string, count) -> String.format("%2d. %s", count, string));
    manyLetters.subscribe(log::info);
  }

  @Test
  public void _2_1_missing_world() {
    Flux<String> helloPauseWorld =
        Mono.just("Hello").concatWith(Mono.just("world").delaySubscription(Duration.ofMillis(500)));
    // 在主线程里对事件源进行订阅无法完成更加复杂的异步操作,
    // 主要是因为在订阅完成之后, 控制权会马上返回到主线程, 并退出整个程序
    helloPauseWorld.subscribe(log::info); // Only print "Hello" because it delays
  }

  @Test
  public void _2_2_hello_world() {
    Flux<String> helloPauseWorld =
        Mono.just("Hello").concatWith(Mono.just("world").delaySubscription(Duration.ofMillis(500)));
    // toItetable and toStream will block
    helloPauseWorld.toStream().forEach(log::info);
  }

  @Test
  public void _3_first_emitting() {
    Mono<String> a = Mono.just("oops I'm late").delaySubscription(Duration.ofMillis(450));
    Flux<String> b =
        Flux.just("let's get", "the party", "started").delayElements(Duration.ofMillis(400));
    Flux.first(a, b).toIterable().forEach(log::info); // b is first
  }

  private Flux<String> alphabet5(char from) {
    // will stop at "z"
    return Flux.range(from, 5).map(i -> "" + (char) i.intValue()).take(Math.min(5, 'z' - from + 1));
  }

  private Mono<String> withDelay(String value, int delaySeconds) {
    return Mono.just(value).delaySubscription(Duration.ofSeconds(delaySeconds));
  }

  @Test
  public void _4_1_alphabet5_limits_to_z() {
    Reactor3Test test = new Reactor3Test();
    StepVerifier.create(test.alphabet5('x')).expectNext("x", "y", "z").expectComplete().verify();
  }

  @Test
  public void _4_2_alphabet5_limits_to_z_is_alphabetical_char() {
    Reactor3Test test = new Reactor3Test();
    StepVerifier.create(test.alphabet5('x'))
        .consumeNextWith(c -> assertTrue(c.matches("[a-z]"), "first is alphabetic"))
        .consumeNextWith(c -> assertTrue(c.matches("[a-z]"), "second is alphabetic"))
        .consumeNextWith(c -> assertTrue(c.matches("[a-z]"), "third is alphabetic"))
        .expectComplete()
        .verify();
  }

  @Test
  public void _4_3_verify_with_delay() {
    Reactor3Test test = new Reactor3Test();
    Duration testDuration =
        StepVerifier.withVirtualTime(() -> test.withDelay("foo", 30))
            .expectSubscription()
            .thenAwait(Duration.ofSeconds(10))
            .expectNoEvent(Duration.ofSeconds(10))
            .thenAwait(Duration.ofSeconds(10))
            .expectNext("foo")
            .expectComplete()
            .verify();
    log.info(testDuration.toMillis() + "ms"); // 84ms
  }
}
