import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import subscriber.SampleSubscriber;

import java.util.Arrays;
import java.util.List;

@DisplayName("4.3 간단한 모노 플럭스 생성 후 구독해보기")
public class Ex4 {

    @Test
    @DisplayName("플럭스 생성하기")
    void flux1() {
        Flux<String> seq1 = Flux.just("foo", "bar", "foobar");

        List<String> iterable = Arrays.asList("foo", "bar", "foobar");
        Flux<String> seq2 = Flux.fromIterable(iterable);

        Mono<String> noData = Mono.empty();
        Mono<String> data = Mono.just("foo");

        Flux<Integer> numbersFromFiveToSeven = Flux.range(5, 3);
    }

    @Test
    @DisplayName("플럭스 구독하기(에러)")
    void subscribe1() {
        Flux<Integer> ints = Flux.range(1, 4)
                .map(i -> {
                    if (i <= 3) return i;
                    throw new RuntimeException("Got to 4");
                });
        ints.subscribe(i -> System.out.println(i),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Done"));
    }

    @Test
    @DisplayName("플럭스 구독하기(완료)")
    void subscribe2() {
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(i -> System.out.println(i),
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Done"));
    }

    @Test
    @DisplayName("플럭스 구독하기(구독 구현)")
    void subscriber3() {
        SampleSubscriber<Integer> ss = new SampleSubscriber<>();
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(ss);
    }

    @Test
    @DisplayName("백프레셔 발생(1건)")
    void backPressure1() {
        Flux.range(1, 10)
                .doOnRequest(r -> System.out.println("request of " + r))
                .subscribe(new BaseSubscriber<Integer>() {
                    @Override
                    protected void hookOnSubscribe(Subscription subscription) {
                        request(1);
                    }

                    @Override
                    protected void hookOnNext(Integer value) {
                        // 1건만 처리후 취소
                        System.out.println("Cancelling after having received " + value);
                        cancel();
                    }
                });
    }

}
