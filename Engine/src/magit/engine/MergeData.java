package magit.engine;

import java.util.Map;

public class MergeData {
    private String message;
    private Map<String, Change> changes;

    public MergeData(String message, Map<String, Change> changes) {
        this.message = message;
        this.changes = changes;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, Change> getChanges() {
        return changes;
    }

}
