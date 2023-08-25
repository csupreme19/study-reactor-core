import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import publisher.MyEventListener;
import publisher.MyEventProcessor;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

@DisplayName("4.4 프로그래밍적 시퀀스 생성(이벤트 정의)")
public class Ex4_4 {
    /*
    Cold Publish:
        Flux.range(), Flux.just(), Flux.generate()
        데이터를 미리 정의 후 구독하는 시점에 조회
    Hot Publish:
        Flux.create(), Flux.push():
        operator 정의 후 발행하는 시점에 데이터 조회
     */

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

}
