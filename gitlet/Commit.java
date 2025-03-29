package gitlet;


import java.io.File;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import static gitlet.Utils.join;
import static gitlet.Utils.readObject;

/** Represents a gitlet commit object.
 *
 *  does at a high level.
 *
 *  @author Srini
 */
public class Commit  implements Serializable {
    /**
     *
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used.
     */

    /**
     * The message of this Commit.
     */
    private String message;
    private String par1CommitID;
    private String par2CommitID;
    private String dateStr;
    private DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd hh:mm:ss yyyy");
    private String commitID;
    private HashMap<String, String> trackedFiles = new HashMap<String, String>();

    public Commit(String m, String parentCommitID1, String parentCommitID2) {
        message = m;
        par1CommitID = parentCommitID1;
        par2CommitID = parentCommitID2;
        //chi = child;
        if (parentCommitID1 == null) {
            dateStr = "Thu Jan 01 00:00:00 1970";
            commitID = Utils.sha1(message, dateStr);
        } else {
            dateStr = dateFormat.format(new Date());
            commitID = Utils.sha1(message, dateStr, par1CommitID);
        }
    }
    public String getDate() {
        return dateStr + " -0800";
    }

    public String getM() {
        return message;
    }

    public String getPar1CommitID() {
        return par1CommitID;
    }

    public String getPar2CommitID() {
        return par2CommitID;
    }

    public String getCommitID() {
        return commitID;
    }

    public HashMap<String, String> getTrackedFiles() {
        return trackedFiles;
    }

    public void setTrackedFiles() {
        File cwd = new File(System.getProperty("user.dir"));
        File gitletDir = join(cwd, ".gitlet");
        File stageFile = Utils.join(gitletDir, "staging");

        Commit parCommit = readObject(Utils.join(gitletDir, "commits/"
                + par1CommitID), Commit.class);
        StageInfo stage = readObject(stageFile, StageInfo.class);

        HashMap<String, String> parentFiles = parCommit.getTrackedFiles();
        HashMap<String, String> stagedForDelFiles = stage.getFilesToRemove();
        HashMap<String, String> stagedForAddFiles = stage.getFilesToAdd();

        trackedFiles = parentFiles;

        for (HashMap.Entry<String, String> fileEntryToAdd : stagedForAddFiles.entrySet()) {
            if (trackedFiles.containsKey(fileEntryToAdd.getKey())) {
                trackedFiles.replace(fileEntryToAdd.getKey(), fileEntryToAdd.getValue());
            } else {
                trackedFiles.put(fileEntryToAdd.getKey(), fileEntryToAdd.getValue());
            }
        }
        for (HashMap.Entry<String, String> fileEntryToAdd : stagedForDelFiles.entrySet()) {
            trackedFiles.remove(fileEntryToAdd.getKey());
        }

    }

}
