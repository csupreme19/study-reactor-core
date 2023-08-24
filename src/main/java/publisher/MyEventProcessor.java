package publisher;

import java.util.ArrayList;
import java.util.List;

public class MyEventProcessor {

    private List<MyEventListener<String>> listeners = new ArrayList<>();

    public void register(MyEventListener<String> listener) {
        listeners.add(listener);
    }

    public void newDataChunk(List<String> chunk) {
        for (MyEventListener<String> listener : listeners) {
            listener.onDataChunk(chunk);
        }
    }

    public void dataComplete() {
        for (MyEventListener<String> listener : listeners) {
            listener.processComplete();
        }
    }

    public void dataError() {
        for (MyEventListener<String> listener : listeners) {
            listener.processError(new RuntimeException());
        }
    }
}

