package magit.engine;

public class Change {
    public Item ours = null;
    public ChangeType oursType = null;
    public String sha1Common = null;
    public Item theirs = null;
    public ChangeType theirsType = null;
    public String path;


    public Change(String path) {
        this.path = path;
    }


}
