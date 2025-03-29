package gitlet;

import java.io.Serializable;
import java.util.HashMap;

public class StageInfo implements Serializable {

    //private String currChild;
    private HashMap<String, String> filesAdded;
    private HashMap<String, String> filesDeleted;
    //private HashSet<String> initial = new HashSet<String>();

    public StageInfo() {
        filesAdded = new HashMap<String, String>();
        filesDeleted = new HashMap<String, String>();
    }

    public void setFilesToAdd(HashMap<String, String> map) {
        filesAdded = map;
    }

    public HashMap<String, String> getFilesToAdd() {
        return filesAdded;
    }

    public void setFilesToRemove(HashMap<String, String> map) {
        filesDeleted = map;
    }

    public HashMap<String, String> getFilesToRemove() {
        return filesDeleted;
    }

    public void clearStage() {
        filesAdded = new HashMap<String, String>();
        filesDeleted = new HashMap<String, String>();
    }

}
