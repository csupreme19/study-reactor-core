import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

@DisplayName("4.5 쓰레드와 스케줄러")
public class Ex4_5 {

    @Test
    @DisplayName("실행 쓰레드 확인")
    void thread() throws InterruptedException {
        final Mono<String> mono = Mono.just("hello ");

        Thread t = new Thread(() -> mono
                .map(msg -> msg + "thread ")
                .subscribe(v -> {
                    System.out.println(v + Thread.currentThread().getName());
                }));

        t.start();
        t.join();

        // hello thread Thread-0
    }

    @Test
    @DisplayName("실행 쓰레드 설정(publishOn)")
    void publishOn() throws InterruptedException {
        Scheduler s = Schedulers.newParallel("parellel-scheduler", 4);  // 4개 쓰레드 생성

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)       // 첫번째 map은 실제 호출되는 쓰레드에서 실행
                .publishOn(s)           // 생성한 쓰레드 사용
                .map(i -> "value " + i);    // 두번째 map은 publishOn에서 설정한 쓰레드 사용

        Thread t = new Thread(() -> flux.subscribe(data -> System.out.printf("[%s] %s%n", Thread.currentThread().getName(), data)));

        t.start();
        t.join();   // 외부 쓰레드 종료 전 현재 테스트의 main 쓰레드가 종료될 수 있기 때문에 설정

        /*
         [parellel-scheduler-1] value 11
         [parellel-scheduler-1] value 12
         */
    }

    @Test
    @DisplayName("실행 쓰레드 설정(subscribeOn)")
    void subscribeOn() throws InterruptedException {
        Scheduler s = Schedulers.newParallel("parallel-scheduler", 4);

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)       // s 쓰레드 4개 중 하나에서 실행
                .subscribeOn(s)           // 모든 시퀀스를 설정한 쓰레드에서 실행
                .map(i -> "value " + i);    // s 쓰레드 4개 중 하나에서 실행

        Thread t = new Thread(() -> flux.subscribe(data -> System.out.printf("[%s] %s%n", Thread.currentThread().getName(), data)));

        t.start();
        t.join();

        /*
         [parellel-scheduler-1] value 11
         [parellel-scheduler-1] value 12
         */
    }

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
}
