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
    }

    @Test
    @DisplayName("플럭스 구독하기(완료)")
    void subscribe2() {
        Flux<Integer> ints = Flux.range(1, 4);
        ints.subscribe(System.out::println,
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
    }

    @Test
    @DisplayName("실행 쓰레드 설정")
    void publishOn() throws InterruptedException {
        Scheduler s = Schedulers.newParallel("parellel-scheduler", 4);  // 4개 쓰레드 생성

        final Flux<String> flux = Flux
                .range(1, 2)
                .map(i -> 10 + i)       // 첫번째 map은 실제 호출되는 쓰레드에서 실행
                .publishOn(s)           // 생성한 쓰레드 사용
                .map(i -> "value " + i);    // 두번째 map은 publishOn에서 설정한 쓰레드 사용

        /**
         * 실제 호출은 마지막 map 이후이므로 설정한 쓰레드로 보임
         * [parellel-scheduler-1] value 11
         * [parellel-scheduler-1] value 12
         */
        Thread t = new Thread(() -> flux.subscribe(data -> System.out.printf("[%s] %s%n", Thread.currentThread().getName(), data)));

        t.start();
        t.join();
    }
}
