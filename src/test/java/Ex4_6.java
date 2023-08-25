import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.LongAdder;

@DisplayName("4.6 예외 처리")
public class Ex4_6 {

    @Test
    @DisplayName("에러 핸들링")
    void onError() {
        Flux.just(1, 2, 0)
                .map(i -> "100 / " + i + " = " + (100 / i))
                .onErrorReturn("Divided by zero 😡") // 에러 처리
                .subscribe(System.out::println);

        /*
        100 / 1 = 100
        100 / 2 = 50
        Divided by zero 😡
         */
    }

    @Test
    @DisplayName("구독시 에러 핸들링")
    void onErrorSubscribe() {
        Flux<String> flux = Flux.range(1, 5)
                .map(v -> {
                    if (v < 5) return v;
                    throw new RuntimeException("" + v);
                })
                .map(v -> {
                    return "value " + v;
                });

        flux.subscribe(value -> {
                    System.out.println(value);
                },
                error -> {
                    System.err.println(error);
                });

        /*
        value 1
        value 2
        value 3
        value 4
        java.lang.RuntimeException: 5
         */
    }

    @Test
    @DisplayName("에러 핸들링(무시)")
    void onErrorComplete() {
        Flux.just(10, 20, 30)
                .map(v -> {
                    if (v < 30) return v;
                    throw new RuntimeException("" + v);
                })
                .onErrorComplete()
                .subscribe(v -> System.out.println(v)); // 에러 발생시 무시

        /*
        10
        20
         */
    }

    @Test
    @DisplayName("에러 핸들링(우회)")
    void onErrorResume() {
        Flux.just(10, 20, 30)
                .map(v -> {
                    if (v < 30) return v;
                    throw new RuntimeException("" + v);
                })
                .onErrorResume((e) -> {
                    System.err.println(e.getMessage());
                    return Flux.error(e);
                })
                .subscribe(v -> System.out.println(v));

        /*
        10
        20
        30
        [ERROR] (main) Operator called default onErrorDropped - reactor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.RuntimeException: 30
         */
    }

    @Test
    @DisplayName("에러 핸들링(부가기능 추가)")
    void doOnError() {
        LongAdder failureStat = new LongAdder();
        Flux<String> flux =
                Flux.just("unknown")
                        .flatMap(k -> callExternalService(k)
                                // doOnError는 onError와 다르게 에러를 다운스트림으로 던진다
                                // 일반적으로 로깅, 메트릭, 부가기능 추가시 사용
                                .doOnError(e -> {
                                    failureStat.increment();
                                    System.out.println("uh oh, falling back, service failed for key " + k);
                                })
                        )
                        .onErrorResume(e ->
                                {
                                    System.out.println("error catched in onError " + e.getClass().getName());
                                    return Flux.just("default-value");
                                }
                        );

        flux.subscribe();

        /*
        uh oh, falling back, service failed for key unknown
        [ERROR] (main) Operator called default onErrorDropped - reactor.core.Exceptions$ErrorCallbackNotImplemented: java.lang.RuntimeException
         */
    }

    private Mono<String> callExternalService(String data) {
        if ("unknown".equals(data)) return Mono.error(new RuntimeException());
        return Mono.just(data);
    }
}
