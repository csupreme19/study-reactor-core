package publisher;

import java.util.ArrayList;
import java.util.List;

public class MyEventProcessor {

    private List<MyEventListener<String>> listeners = new ArrayList<>();

    public void register(MyEventListener<String> listener) {
        listeners.add(listener);
    }

    // Imagine this method gets called when a new chunk of data is available
    public void newDataChunk(List<String> chunk) {
        for (MyEventListener<String> listener : listeners) {
            listener.onDataChunk(chunk);
        }
    }

    // Imagine this method gets called when the data stream is complete
    public void dataComplete() {
        for (MyEventListener<String> listener : listeners) {
            listener.processComplete();
        }
    }
}

