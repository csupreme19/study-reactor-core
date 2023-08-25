import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscription;
import publisher.MyEventListener;
import publisher.MyEventProcessor;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
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
        ints.subscribe(System.out::println,
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Done"));
        /*
        1
        2
        3
        Error: java.lang.RuntimeException: Got to 4
         */
    }

    @Test
    @DisplayName("플럭스 구독하기(완료)")
    void subscribe2() {
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(System.out::println,
                error -> System.err.println("Error: " + error),
                () -> System.out.println("Done"));
        /*
        1
        2
        3
        4
        Done
         */
    }

    @Test
    @DisplayName("플럭스 구독하기(구독 구현)")
    void subscriber3() {
        SampleSubscriber<Integer> ss = new SampleSubscriber<>();
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(ss);

        /*
        Subscribed
        1
        2
        3
        4
         */
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

        /*
        request of 1
        Cancelling after having received 1
         */
    }

    @Test
    @DisplayName("동기식 플럭스 생성(generate)")
    void fluxGenerate() {
        Flux<String> flux = Flux.generate(
                // 초기값
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3 * state);
                    if (state == 10) sink.complete();
                    return state + 1;
                }
        );

        // 구독하는 시점에 generator에서 정의한대로 자동 발행
        flux.subscribe(System.out::println);

        /*
        3 x 0 = 0
        3 x 1 = 3
        3 x 2 = 6
        3 x 3 = 9
        3 x 4 = 12
        3 x 5 = 15
        3 x 6 = 18
        3 x 7 = 21
        3 x 8 = 24
        3 x 9 = 27
        3 x 10 = 30
         */
    }

    @Test
    @DisplayName("비동기식 멀티쓰레드 플럭스 생성(create)")
    void fluxCreate() {
        MyEventProcessor myEventProcessor = new MyEventProcessor();

        Flux<String> bridge = Flux.create(sink -> {
            myEventProcessor.register(
                    new MyEventListener<String>() {
                        @Override
                        public void onDataChunk(List<String> chunk) {
                            for (String s : chunk) {
                                sink.next(s);
                            }
                        }

                        @Override
                        public void processComplete() {
                            sink.complete();
                        }

                        @Override
                        public void processError(Throwable e) {
                            sink.error(e);
                        }
                    }
            );
        });

        // 구독 이후 발행되면 데이터 확인 가능
        bridge.subscribe(
                item -> System.out.println("Received: " + item),
                error -> System.err.println("Error received: " + error),
                () -> System.out.println("Stream completed")
        );

        myEventProcessor.newDataChunk(Arrays.asList("item1", "item2"));
        myEventProcessor.dataComplete();

        /*
        Received: item1
        Received: item2
        Stream completed
         */
    }

    @Test
    @DisplayName("비동기 싱글쓰레드 플럭스 생성(push) ")
    void fluxPush() {
        MyEventProcessor myEventProcessor = new MyEventProcessor();

        Flux<String> bridge = Flux.push(sink -> {
            myEventProcessor.register(
                    new MyEventListener<String>() {
                        @Override
                        public void onDataChunk(List<String> chunk) {
                            for (String s : chunk) {
                                sink.next(s);
                            }
                        }

                        @Override
                        public void processComplete() {
                            sink.complete();
                        }

                        @Override
                        public void processError(Throwable e) {
                            sink.error(e);
                        }
                    }
            );
        });

        bridge.subscribe(
                item -> System.out.println("Received: " + item),
                error -> System.err.println("Error received: " + error),
                () -> System.out.println("Stream completed")
        );

        myEventProcessor.newDataChunk(List.of("item1", "item2", "item3"));
        myEventProcessor.dataError();

        /*
        Received: item1
        Received: item2
        Received: item3
        Error received: java.lang.RuntimeException
         */
    }

    @Test
    @DisplayName("플럭스 취소, 오류 처리")
    void fluxClean() {
        MyEventProcessor myEventProcessor = new MyEventProcessor();

        Flux<String> bridge = Flux.create(sink -> {
            myEventProcessor.register(
                    new MyEventListener<String>() {
                        @Override
                        public void onDataChunk(List<String> chunk) {
                            for (String s : chunk) {
                                sink.next(s);
                            }
                        }

                        @Override
                        public void processComplete() {
                            sink.complete();
                        }

                        @Override
                        public void processError(Throwable e) {
                            sink.error(e);
                        }
                    }
            );
            sink.onRequest(n -> System.out.println("Flux request " + n))
                    .onCancel(() -> System.out.println("Flux cancelled"))
                    .onDispose(() -> System.out.println("Flux disposed"));
        });

        // 구독 이후 발행되면 데이터 확인 가능
        bridge.subscribe(
                item -> System.out.println("Received: " + item),
                error -> System.err.println("Error received: " + error),
                () -> System.out.println("Stream completed")
        );

        myEventProcessor.newDataChunk(Arrays.asList("item1", "item2"));
        myEventProcessor.dataComplete();

        /*
        Flux request 9223372036854775807
        Received: item1
        Received: item2
        Stream completed
        Flux disposed
         */
    }

    @Test
    @DisplayName("플럭스 핸들링(동기식)")
    void fluxHandle() {
        // Flux.generate와 유사하게 one-by-one emission
        Flux<String> alphabet = Flux.just(-1, 30, 13, 9, 20)
                .handle((i, sink) -> {
                    String letter = alphabet(i);
                    if (letter != null)
                        sink.next(letter);
                });

        alphabet.subscribe(System.out::println);

        /*
        M
        I
        T
         */
    }

    private String alphabet(int letterNumber) {
        if (letterNumber < 1 || letterNumber > 26) {
            return null;
        }
        int letterIndexAscii = 'A' + letterNumber - 1;
        return "" + (char) letterIndexAscii;
    }

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
