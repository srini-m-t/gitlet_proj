package gitlet;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static gitlet.Utils.*;



/** Represents what a gitlet repository.
 *
 *  does at a high level.
 *
 *  @author Srini
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here
     */

    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public Repository(String[] args) {
        if (GITLET_DIR.exists()) {
            System.out.println(
                    "A Gitlet version-control system already exists in the current directory");
        } else {
            GITLET_DIR.mkdir();

            File branchFolder = Utils.join(GITLET_DIR, "branches");
            File blobFolder = Utils.join(GITLET_DIR, "blobs");
            File commitsFolder = Utils.join(GITLET_DIR, "commits");

            File stageFile = Utils.join(GITLET_DIR, "staging");
            File head = Utils.join(GITLET_DIR, "head");

            branchFolder.mkdir();
            blobFolder.mkdir();
            commitsFolder.mkdir();

            Commit initialCommit = new Commit("initial commit", null, null);

            File initialCommitFile = Utils.join(commitsFolder, initialCommit.getCommitID());
            writeObject(initialCommitFile, initialCommit);

            File masterBranchFile = Utils.join(branchFolder, "master");
            writeContents(masterBranchFile, initialCommit.getCommitID());

            StageInfo stage = new StageInfo();
            writeObject(stageFile, stage);

            writeContents(head, "master");

        }
    }

    public static void add(String[] args) {
        String fileNameStr = args[1];
        File fileToAdd = Utils.join(CWD, fileNameStr);
        if (!fileToAdd.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String fileToAddStr = readContentsAsString(fileToAdd);
        String fileBlobID = Utils.sha1(fileToAddStr);

        File blobFolder = Utils.join(GITLET_DIR, "blobs");
        File blobFile = Utils.join(blobFolder, fileBlobID);
        writeContents(blobFile, fileToAddStr);

        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = readObject(stageFile, StageInfo.class);
        HashMap<String, String> filesStagedToAdd = stage.getFilesToAdd();
        HashMap<String, String> filesStagedToRemove = stage.getFilesToRemove();

        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit currCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currCommitID), Commit.class);
        HashMap<String, String> currentlyTrackedFiles = currCommit.getTrackedFiles();

        if (filesStagedToRemove.containsKey(fileNameStr)) {
            filesStagedToRemove.remove(fileNameStr);
            stage.setFilesToRemove(filesStagedToRemove);
        }

        if (currentlyTrackedFiles.containsKey(fileNameStr)
                && currentlyTrackedFiles.get(fileNameStr).equals(fileBlobID)) {
            filesStagedToAdd.remove(fileToAddStr);
        } else if (filesStagedToAdd.containsKey(fileNameStr)) {
            filesStagedToAdd.replace(fileNameStr, fileBlobID);
        } else {
            filesStagedToAdd.put(fileNameStr, fileBlobID);
        }

        stage.setFilesToAdd(filesStagedToAdd);
        writeObject(stageFile, stage);

    }

    public static void commit(String[] args) {

        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = readObject(stageFile, StageInfo.class);
        if (stage.getFilesToRemove().isEmpty() && stage.getFilesToAdd().isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }


        if (args.length <= 1 || args[1].length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }

        String par2CommitID = null;

        if (args.length == 3) {
            par2CommitID = args[2];
        }

        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit currCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currCommitID), Commit.class);
        HashMap<String, String> currentlyTrackedFiles = currCommit.getTrackedFiles();

        Commit newCommit = new Commit(args[1], currCommitID, par2CommitID);
        newCommit.setTrackedFiles();

        File commitsFolder = Utils.join(GITLET_DIR, "commits");
        File newCommitFile = Utils.join(commitsFolder, newCommit.getCommitID());
        writeObject(newCommitFile, newCommit);

        File currBranchFile = Utils.join(GITLET_DIR, "branches/" + currBranch);
        writeContents(currBranchFile, newCommit.getCommitID());


        stage.clearStage();
        writeObject(stageFile, stage);

    }

    public static void remove(String[] args) {

        String fileNameStr = args[1];

        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = readObject(stageFile, StageInfo.class);

        HashMap<String, String> stagedToAdd = stage.getFilesToAdd();
        HashMap<String, String> stagedToRemove = stage.getFilesToRemove();

        boolean reasonToRemoveFile;

        if (stagedToAdd.containsKey(fileNameStr)) {
            stagedToAdd.remove(fileNameStr);
            stage.setFilesToAdd(stagedToAdd);
            reasonToRemoveFile = false;
        } else {
            reasonToRemoveFile = true;
        }

        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit currCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currCommitID), Commit.class);
        HashMap<String, String> currentlyTrackedFiles = currCommit.getTrackedFiles();

        if (currentlyTrackedFiles.containsKey(fileNameStr)) {
            stagedToRemove.put(fileNameStr, fileNameStr);
            stage.setFilesToRemove(stagedToRemove);
            restrictedDelete(fileNameStr);
        } else if (reasonToRemoveFile) {
            System.out.println("No reason to remove the file.");
        }

        writeObject(stageFile, stage);
    }

    public static void log() {
        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit currCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currCommitID), Commit.class);

        Commit temp = currCommit;
        String toPrintStr = "";

        while (temp.getPar1CommitID() != null) {
            toPrintStr += "===\ncommit ";
            toPrintStr += temp.getCommitID();
            if (temp.getPar2CommitID() != null) {
                toPrintStr += "\nMerge: " + temp.getPar1CommitID().substring(0, 7)
                        + " " + temp.getPar2CommitID().substring(0, 7);
            }
            toPrintStr += "\nDate: ";
            toPrintStr += temp.getDate();
            toPrintStr += "\n" + temp.getM();
            toPrintStr += "\n\n";

            temp = getCommit(temp.getPar1CommitID());
        }
        toPrintStr += "===\ncommit ";
        toPrintStr += temp.getCommitID();
        toPrintStr += "\nDate: ";
        toPrintStr += temp.getDate();
        toPrintStr += "\n" + temp.getM();
        toPrintStr += "\n";
        System.out.println(toPrintStr);

    }

    private static Commit getCommit(String commitID) {
        return readObject(Utils.join(GITLET_DIR, "commits/" + commitID), Commit.class);
    }

    public static void globalLog() {
        File commitsFolder = Utils.join(GITLET_DIR, "commits");
        List<String> commitIDs = plainFilenamesIn(commitsFolder);
        String toPrintStr = "";

        for (String commitID : commitIDs) {
            Commit temp = getCommit(commitID);

            toPrintStr += "===\ncommit ";
            toPrintStr += commitID;
            if (temp.getPar2CommitID() != null) {
                toPrintStr += "\nMerge: " + temp.getPar1CommitID().substring(0, 7)
                        + " " + temp.getPar2CommitID().substring(0, 7);
            }
            toPrintStr += "\nDate: ";
            toPrintStr += temp.getDate();
            toPrintStr += "\n" + temp.getM();
            toPrintStr += "\n\n";
        }
        toPrintStr = toPrintStr.trim();
        toPrintStr += "\n";
        System.out.println(toPrintStr);
    }

    public static void checkout(String[] args) {
        File blobFolder = Utils.join(GITLET_DIR, "blobs");
        if (args.length == 3) {
            String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
            String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                    "branches/" + currBranch));
            Commit currCommit = readObject(Utils.join(GITLET_DIR,
                    "commits/" + currCommitID), Commit.class);
            String fileNameStr = args[2];
            HashMap<String, String> filesTrackedInCommit = currCommit.getTrackedFiles();
            if (filesTrackedInCommit.containsKey(fileNameStr)) {
                restrictedDelete(fileNameStr);
                File newFile = Utils.join(CWD, fileNameStr);
                String newContent = readContentsAsString(Utils.join(blobFolder,
                        filesTrackedInCommit.get(fileNameStr)));
                writeContents(newFile, newContent);
            } else {
                System.out.println("File does not exist in that commit.");
                return;
            }
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands.");
            }
            List<String> allCommitIDs = plainFilenamesIn(Utils.join(GITLET_DIR, "commits"));
            boolean commitIdExists = false;
            String commitIDOfInterest = null;
            for (String id : allCommitIDs) {
                if (id.startsWith(args[1])) {
                    commitIDOfInterest = id;
                    commitIdExists = true;
                }
            }
            if (!commitIdExists) {
                System.out.println("No commit with that id exists.");
                return;
            }
            Commit commitOfInterest = getCommit(commitIDOfInterest);
            String fileNameStr = args[3];
            HashMap<String, String> filesTrackedInCommit = commitOfInterest.getTrackedFiles();
            if (filesTrackedInCommit.containsKey(fileNameStr)) {
                restrictedDelete(fileNameStr);
                File newFile = Utils.join(CWD, fileNameStr);
                String newContent = readContentsAsString(Utils.join(blobFolder,
                        filesTrackedInCommit.get(fileNameStr)));
                writeContents(newFile, newContent);
            } else {
                System.out.println("File does not exist in that commit.");
                return;
            }
        } else if (args.length == 2) {
            checkOutTwoArgs(args, blobFolder);
        }
    }

    private static void checkOutTwoArgs(String[] args, File blobFolder) {
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        List<String> allBranchNames = plainFilenamesIn(branchFolder);
        String branchName = args[1];
        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        if (!allBranchNames.contains(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (currBranch.equals(branchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        String newHeadCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + branchName));
        String currHeadCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit newHeadCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + newHeadCommitID), Commit.class);
        Commit currHeadCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currHeadCommitID), Commit.class);
        HashMap<String, String> newHeadCommitTrackedFiles = newHeadCommit.getTrackedFiles();
        HashMap<String, String> currHeadCommitTrackedFiles = currHeadCommit.getTrackedFiles();
        List<String> filesInCurrDir = plainFilenamesIn(CWD);
        for (HashMap.Entry<String, String> entry : newHeadCommitTrackedFiles.entrySet()) {
            if (!currHeadCommitTrackedFiles.containsKey(entry.getKey())
                    && filesInCurrDir.contains(entry.getKey())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        for (HashMap.Entry<String, String> entry : newHeadCommitTrackedFiles.entrySet()) {
            restrictedDelete(entry.getKey());
            File newFile = Utils.join(CWD, entry.getKey());
            String newContent = readContentsAsString(Utils.join(blobFolder, entry.getValue()));
            writeContents(newFile, newContent);
        }
        for (HashMap.Entry<String, String> entry : currHeadCommitTrackedFiles.entrySet()) {
            if (!newHeadCommitTrackedFiles.keySet().contains(entry.getKey())) {
                restrictedDelete(entry.getKey());
            }
        }
        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = new StageInfo();
        writeObject(stageFile, stage);
        File head = Utils.join(GITLET_DIR, "head");
        writeContents(head, branchName);
    }

    public static void find(String[] args) {
        List<String> allCommitIDs = plainFilenamesIn(Utils.join(GITLET_DIR, "commits"));
        int n = 0;
        for (String commitID : allCommitIDs) {
            Commit temp = getCommit(commitID);
            if (temp.getM().equals(args[1])) {
                n += 1;
                System.out.println(commitID);
            }
        }
        if (n == 0) {
            System.out.println("Found no commit with that message.");
        }
    }

    public static void status() {
        String currBranchName = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        File blobFolder = Utils.join(GITLET_DIR, "blobs");
        List<String> allBranchNames = plainFilenamesIn(branchFolder);
        allBranchNames.sort(null);
        System.out.println("=== Branches ===");
        for (String name : allBranchNames) {
            if (name.equals(currBranchName)) {
                System.out.println("*" + name);
            } else {
                System.out.println(name);
            }
        }
        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = readObject(stageFile, StageInfo.class);
        HashMap<String, String> stagedForAddition = stage.getFilesToAdd();
        HashMap<String, String> stagedForRemoving = stage.getFilesToRemove();
        System.out.println("\n=== Staged Files ===");
        List<String> tempSortedList = new ArrayList<String>(stagedForAddition.keySet());
        Collections.sort(tempSortedList);
        for (String fileName : tempSortedList) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Removed Files ===");
        tempSortedList = new ArrayList<String>(stagedForRemoving.keySet());
        Collections.sort(tempSortedList);
        for (String fileName : stagedForRemoving.keySet()) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        String currHeadCommitID = readContentsAsString(Utils.join(branchFolder,
                currBranchName));
        Commit currentHeadCommit = getCommit(currHeadCommitID);
        HashMap<String, String> trackedFiles = currentHeadCommit.getTrackedFiles();
        List<String> fileNamesInCWD = plainFilenamesIn(CWD);
        List<String> modButNotStaged = new ArrayList<String>();
        List<String> unTrackedFiles = new ArrayList<String>();
        statusHelp(fileNamesInCWD, trackedFiles, blobFolder, modButNotStaged,
                stagedForAddition, stagedForRemoving, unTrackedFiles);
        for (String fileName : stagedForAddition.keySet()) {
            if (!fileNamesInCWD.contains(fileName)) {
                modButNotStaged.add(fileName + " (deleted)");
            }
        }
        for (String fileName : trackedFiles.keySet()) {
            if (!stagedForRemoving.keySet().contains(fileName)
                    && !fileNamesInCWD.contains(fileName)) {
                modButNotStaged.add(fileName + " (deleted)");
            }
        }
        tempSortedList = new ArrayList<String>(modButNotStaged);
        Collections.sort(tempSortedList);
        for (String fileName : tempSortedList) {
            System.out.println(fileName);
        }
        System.out.println("\n=== Untracked Files ===");
        tempSortedList = new ArrayList<String>(unTrackedFiles);
        Collections.sort(tempSortedList);
        for (String fileName : tempSortedList) {
            System.out.println(fileName);
        }
        System.out.println();
    }

    private static void statusHelp(List<String> fileNamesInCWD,
                                   HashMap<String, String> trackedFiles,
                                   File blobFolder, List<String> modButNotStaged,
                                   HashMap<String, String> stagedForAddition,
                                   HashMap<String, String> stagedForRemoving,
                                   List<String> unTrackedFiles) {
        for (String fileName : fileNamesInCWD) {
            File fTemp = Utils.join(CWD, fileName);
            String fStr = readContentsAsString(fTemp);
            String fTrackedFileStr;
            String fStagedForAddFileStr;
            if (trackedFiles.containsKey(fileName)) {
                File fTrackedFile = Utils.join(blobFolder, trackedFiles.get(fileName));
                fTrackedFileStr = readContentsAsString(fTrackedFile);
            } else {
                fTrackedFileStr = "-999";
            }
            if (stagedForAddition.containsKey(fileName)) {
                File fStagedForAddFile = Utils.join(blobFolder, stagedForAddition.get(fileName));
                fStagedForAddFileStr = readContentsAsString(fStagedForAddFile);
            } else {
                fStagedForAddFileStr = "-999";
            }
            if (trackedFiles.keySet().contains(fileName)
                    && !fStr.equals(fTrackedFileStr)
                    && !stagedForAddition.containsKey(fileName)) {
                modButNotStaged.add(fileName + " (modified)");
            } else if (stagedForAddition.keySet().contains(fileName)
                    && !fStr.equals(fStagedForAddFileStr)) {
                modButNotStaged.add(fileName + " (modified)");
            }
            if (!trackedFiles.keySet().contains(fileName)
                    && !stagedForAddition.keySet().contains(fileName)
                    && !stagedForRemoving.keySet().contains(fileName)) {
                unTrackedFiles.add(fileName);
            }
        }
    }

    public static void createBranch(String[] args) {
        String newBranchName = args[1];
        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        String currCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        List<String> allBranchNames = plainFilenamesIn(branchFolder);

        if (allBranchNames.contains(newBranchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }

        File newBranchFile = Utils.join(branchFolder, newBranchName);
        writeContents(newBranchFile, currCommitID);
    }

    public static void removeBranch(String[] args) {
        String branchName = args[1];
        String currBranchName = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        List<String> allBranchNames = plainFilenamesIn(branchFolder);

        if (branchName.equals(currBranchName)) {
            System.out.println("Cannot remove the current branch.");
            return;
        } else if (!allBranchNames.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        } else {
            File branchFile = Utils.join(branchFolder, branchName);
            branchFile.delete();
        }

    }

    public static void reset(String[] args) {
        File blobFolder = Utils.join(GITLET_DIR, "blobs");

        File commitsFolder = Utils.join(GITLET_DIR, "commits");
        List<String> allCommitNames = plainFilenamesIn(commitsFolder);
        String givenCommitId = args[1];
        String currBranch = readContentsAsString(Utils.join(GITLET_DIR, "head"));

        if (!allCommitNames.contains(givenCommitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }

        String newHeadCommitID = givenCommitId;
        String currHeadCommitID = readContentsAsString(Utils.join(GITLET_DIR,
                "branches/" + currBranch));
        Commit newHeadCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + newHeadCommitID), Commit.class);
        Commit currHeadCommit = readObject(Utils.join(GITLET_DIR,
                "commits/" + currHeadCommitID), Commit.class);
        HashMap<String, String> newHeadCommitTrackedFiles = newHeadCommit.getTrackedFiles();
        HashMap<String, String> currHeadCommitTrackedFiles = currHeadCommit.getTrackedFiles();
        List<String> fileInCurrWorkDir = plainFilenamesIn(CWD);
        for (HashMap.Entry<String, String> entry : newHeadCommitTrackedFiles.entrySet()) {
            if (!currHeadCommitTrackedFiles.containsKey(entry.getKey())
                    && fileInCurrWorkDir.contains(entry.getKey())) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        for (HashMap.Entry<String, String> entry : newHeadCommitTrackedFiles.entrySet()) {
            restrictedDelete(entry.getKey());
            File newFile = Utils.join(CWD, entry.getKey());
            String newContent = readContentsAsString(Utils.join(blobFolder, entry.getValue()));
            writeContents(newFile, newContent);
        }
        for (HashMap.Entry<String, String> entry : currHeadCommitTrackedFiles.entrySet()) {
            if (!newHeadCommitTrackedFiles.keySet().contains(entry.getKey())) {
                restrictedDelete(entry.getKey());
            }
        }
        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = new StageInfo();
        writeObject(stageFile, stage);
        //File head = Utils.join(GITLET_DIR, "head");
        //writeContents(head, branch_name);
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        File currBranchFile = Utils.join(branchFolder, currBranch);
        writeContents(currBranchFile, givenCommitId);
    }

    public static void merge(String[] args) {
        File stageFile = Utils.join(GITLET_DIR, "staging");
        StageInfo stage = readObject(stageFile, StageInfo.class);
        if (!stage.getFilesToAdd().isEmpty() || !stage.getFilesToRemove().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        File blobFolder = Utils.join(GITLET_DIR, "blobs");
        File commitsFolder = Utils.join(GITLET_DIR, "commits");
        List<String> allCommitNames = plainFilenamesIn(commitsFolder);
        File branchFolder = Utils.join(GITLET_DIR, "branches");
        List<String> allBranchNames = plainFilenamesIn(branchFolder);
        String givenBranchName = args[1];
        String currentBranchName = readContentsAsString(Utils.join(GITLET_DIR, "head"));
        if (!allBranchNames.contains(givenBranchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String givenHeadCommitID = readContentsAsString(Utils.join(branchFolder, givenBranchName));
        String currentHeadCommitID = readContentsAsString(Utils.join(branchFolder,
                currentBranchName));
        if (givenBranchName.equals(currentBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        } else {
            Commit givenHeadCommit = getCommit(givenHeadCommitID);
            Commit currentHeadCommit = getCommit(currentHeadCommitID);
            List<String> filesInCWD = plainFilenamesIn(CWD);
            for (HashMap.Entry<String, String> entry : givenHeadCommit.getTrackedFiles()
                    .entrySet()) {
                if (!currentHeadCommit.getTrackedFiles().containsKey(entry.getKey())
                        && filesInCWD.contains(entry.getKey())) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return;
                }
            }
            ArrayList<Commit> givenBranchParentCommits = getBFSOrderedCommitList(givenHeadCommit);
            ArrayList<Commit> currBranchParentCommits = getBFSOrderedCommitList(currentHeadCommit);
            Commit ca = getCA(currBranchParentCommits, givenBranchParentCommits);
            if (ca.getCommitID().equals(currentHeadCommit.getCommitID())) {
                String[] argsTemp = new String[2];
                argsTemp[0] = "checkout";
                argsTemp[1] = givenBranchName;
                checkout(argsTemp);
                System.out.println("Current branch fast-forwarded.");
                return;
            } else if (ca.getCommitID().equals(givenHeadCommit.getCommitID())) {
                System.out.println("Given branch is an ancestor of the current branch.");
                return;
            } else {
                mergeHelp2(givenHeadCommit, currentHeadCommit, ca,
                        blobFolder, givenHeadCommitID);
            }
            String[] args1 = new String[3];
            args1[0] = "commit";
            args1[1] = "Merged " + givenBranchName + " into " + currentBranchName + ".";
            args1[2] = givenHeadCommitID;
            commit(args1);
        }
    }

    private static HashSet<String> mergeHelp1(Commit givenHeadCommit,
                                              Commit currentHeadCommit, Commit ca) {
        HashSet<String> allFiles = new HashSet<String>();
        for (String fileStr : givenHeadCommit.getTrackedFiles().keySet()) {
            allFiles.add(fileStr);
        }
        for (String fileStr : currentHeadCommit.getTrackedFiles().keySet()) {
            allFiles.add(fileStr);
        }
        for (String fileStr : ca.getTrackedFiles().keySet()) {
            allFiles.add(fileStr);
        }
        return allFiles;
    }

    private static void mergeHelp2(Commit givenHeadCommit,
                                   Commit currentHeadCommit, Commit ca,
                                   File blobFolder, String givenHeadCommitID) {
        HashSet<String> allFiles = mergeHelp1(givenHeadCommit,
                currentHeadCommit, ca);
        for (String fileStr : allFiles) {
            if (currentHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && ca.getTrackedFiles().containsKey(fileStr)
                    && currentHeadCommit.getTrackedFiles().get(fileStr)
                    .equals(givenHeadCommit.getTrackedFiles().get(fileStr))) {
                File fCurr = Utils.join(blobFolder,
                        currentHeadCommit.getTrackedFiles().get(fileStr));
                File fGiven = Utils.join(blobFolder,
                        givenHeadCommit.getTrackedFiles().get(fileStr));
                File fCA = Utils.join(blobFolder, ca.getTrackedFiles().get(fileStr));
                if (!filesAreSame(fGiven, fCA) && filesAreSame(fCurr, fCA)) {
                    mergeCase1Help(fileStr); //Case 1
                } //Case 3
            } else if (currentHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && currentHeadCommit.getTrackedFiles().get(fileStr).
                    equals(givenHeadCommit.getTrackedFiles().get(fileStr))) {
                File fCurr = Utils.join(blobFolder,
                        currentHeadCommit.getTrackedFiles().get(fileStr));
                File fGiven = Utils.join(blobFolder,
                        givenHeadCommit.getTrackedFiles().get(fileStr));
                if (filesAreSame(fGiven, fCurr)) {
                    continue;
                }
            } else if (!ca.getTrackedFiles().containsKey(fileStr) //Case 4
                    && !givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && currentHeadCommit.getTrackedFiles().containsKey(fileStr)) {
                continue;
            } else if (givenHeadCommit.getTrackedFiles().containsKey(fileStr) //Case 5
                    && !ca.getTrackedFiles().containsKey(fileStr)
                    && !currentHeadCommit.getTrackedFiles().containsKey(fileStr)) {
                mergeCase5Help(givenHeadCommitID, fileStr); //Case 5
            } else if (ca.getTrackedFiles().containsKey(fileStr) //Case 6
                    && currentHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && !givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && currentHeadCommit.getTrackedFiles().get(fileStr)
                    .equals(ca.getTrackedFiles().get(fileStr))) {
                mergeCase6Help(blobFolder, currentHeadCommit, ca, fileStr);
            } else if (ca.getTrackedFiles().containsKey(fileStr) //Case 7
                    && givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && !currentHeadCommit.getTrackedFiles().containsKey(fileStr)) {
                continue;
            } else { // Case 8 (conflicts)
                mergeCase8Help(fileStr, currentHeadCommit,
                        givenHeadCommit, blobFolder, ca);
            }
            if (currentHeadCommit.getTrackedFiles().containsKey(fileStr) //Case 2
                    && givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                    && ca.getTrackedFiles().containsKey(fileStr)) {
                File fCurr = Utils.join(blobFolder,
                        currentHeadCommit.getTrackedFiles().get(fileStr));
                File fGiven = Utils.join(blobFolder,
                        givenHeadCommit.getTrackedFiles().get(fileStr));
                File fCA = Utils.join(blobFolder, ca.getTrackedFiles().get(fileStr));
                if (!filesAreSame(fGiven, fCurr) && filesAreSame(fCurr, fCA)) {
                    File f = new File(fileStr);
                    writeContents(f, readContentsAsString(fGiven));
                    String[] args2 = new String[2];
                    args2[0] = "add";
                    args2[1] = fileStr;
                    add(args2);
                }
            }
        }
    }

    private static void mergeCase1Help(String fileStr) {
        //CASE 1
        String[] args1 = new String[4];
        args1[0] = "checkout";
        args1[1] = "given_head_commitID";
        args1[2] = "--";
        args1[3] = fileStr;
        checkout(args1);
        String[] args2 = new String[2];
        args2[0] = "add";
        args2[1] = fileStr;
        add(args2);
    }

    private static void mergeCase5Help(String givenHeadCommitID, String fileStr) {
        //Case 5
        String[] argsTemp = new String[4];
        argsTemp[0] = "checkout";
        argsTemp[1] = givenHeadCommitID;
        argsTemp[2] = "--";
        argsTemp[3] = fileStr;
        checkout(argsTemp);
        String[] argsTemp1 = new String[2];
        argsTemp1[0] = "add";
        argsTemp1[1] = fileStr;
        add(argsTemp1);
    }

    private static void mergeCase6Help(File blobFolder, Commit currentHeadCommit,
                                       Commit ca, String fileStr) {
        File fCurr = Utils.join(blobFolder,
                currentHeadCommit.getTrackedFiles().get(fileStr));
        File fCA = Utils.join(blobFolder, ca.getTrackedFiles().get(fileStr));
        if (filesAreSame(fCurr, fCA)) {
            String[] argsTemp = new String[2];
            argsTemp[0] = "rm";
            argsTemp[1] = fileStr;
            remove(argsTemp);
        }
    }

    private static void mergeCase8Help(String fileStr, Commit currentHeadCommit,
                                       Commit givenHeadCommit, File blobFolder,
                                       Commit ca) {
        File f = new File(fileStr);
        if (currentHeadCommit.getTrackedFiles().containsKey(fileStr)
                && givenHeadCommit.getTrackedFiles().containsKey(fileStr)) {
            File fCurr = Utils.join(blobFolder,
                    currentHeadCommit.getTrackedFiles().get(fileStr));
            File fGiven = Utils.join(blobFolder,
                    givenHeadCommit.getTrackedFiles().get(fileStr));
            File fCA = Utils.join(blobFolder, ca.getTrackedFiles().get(fileStr));
            if (!filesAreSame(fGiven, fCurr)
                    && !filesAreSame(fCurr, fCA) && !filesAreSame(fGiven, fCA)) {
                String header = "<<<<<<< HEAD\n";
                String cont1 = readContentsAsString(fCurr);
                String divider = "=======\n";
                String cont2 = readContentsAsString(fGiven);
                String footer = ">>>>>>>\n";
                String newCont = header + cont1 + divider + cont2 + footer;
                writeContents(f, newCont);
                System.out.println("Encountered a merge conflict.");
                String[] args2 = new String[2];
                args2[0] = "add";
                args2[1] = fileStr;
                add(args2);
            }
        } else if (!givenHeadCommit.getTrackedFiles().containsKey(fileStr)
                && currentHeadCommit.getTrackedFiles().containsKey(fileStr)) {
            String header = "<<<<<<< HEAD\n";
            String cont1 = readContentsAsString(Utils.join(blobFolder,
                    currentHeadCommit.getTrackedFiles().get(fileStr)));
            String divider = "=======\n";
            String cont2 = "";
            String footer = ">>>>>>>\n";
            String newCont = header + cont1 + divider + cont2 + footer;
            writeContents(f, newCont);
            System.out.println("Encountered a merge conflict.");
            String[] args2 = new String[2];
            args2[0] = "add";
            args2[1] = fileStr;
            add(args2);
        } else if (!currentHeadCommit.getTrackedFiles().containsKey(fileStr)
                && givenHeadCommit.getTrackedFiles().containsKey(fileStr)) {
            File fGiven = Utils.join(blobFolder,
                    givenHeadCommit.getTrackedFiles().get(fileStr));
            String header = "<<<<<<< HEAD\n";
            String cont1 = "";
            String divider = "=======\n";
            String cont2 = readContentsAsString(fGiven);
            String footer = ">>>>>>>\n";
            String newCont = header + cont1 + divider + cont2 + footer;
            writeContents(f, newCont);
            System.out.println("Encountered a merge conflict.");
            String[] args2 = new String[2];
            args2[0] = "add";
            args2[1] = fileStr;
            add(args2);
        }
    }

    private static boolean filesAreSame(File f1, File f2) {
        String f1Str = readContentsAsString(f1);
        String f2Str = readContentsAsString(f2);

        if (f1.equals(f2)) {
            return true;
        }
        return false;
    }

    private static ArrayList<Commit> getBFSOrderedCommitList(Commit commit) {
        ArrayList<Commit> commList = new ArrayList<Commit>();
        ArrayList<Commit> fringe = new ArrayList<Commit>();
        fringe.add(commit);

        while (!fringe.isEmpty()) {
            Commit nextCom = fringe.get(0);
            if (!commList.contains(nextCom)) {
                commList.add(nextCom);
            }
            if (nextCom.getPar1CommitID() != null) {
                fringe.add(getCommit(nextCom.getPar1CommitID()));
            }
            if (nextCom.getPar2CommitID() != null) {
                fringe.add(getCommit(nextCom.getPar2CommitID()));
            }
            fringe.remove(0);
        }

        return commList;
    }

    private static Commit getCA(ArrayList<Commit> currBranchCommits,
                                ArrayList<Commit> givenBranchCommits) {
        ArrayList<String> currBranchCommitID = new ArrayList<String>();
        ArrayList<String> givenBranchCommitID = new ArrayList<String>();
        for (Commit comm : currBranchCommits) {
            currBranchCommitID.add(comm.getCommitID());
        }
        for (Commit comm : givenBranchCommits) {
            givenBranchCommitID.add(comm.getCommitID());
        }

        for (String givenID : givenBranchCommitID) {
            if (currBranchCommitID.contains(givenID)) {
                return getCommit(givenID);
            }
        }
        System.out.println("Shouldn't be possible");
        return null;
    }

    public static void addRemote(String[] args) {
        String remoteName = args[1];
        String remoteLoc = args[2];
        String path = CWD.getParent();
        if (remoteLoc.startsWith("..")) {
            remoteLoc = remoteLoc.replace("..", path);
        }

        HashMap<String, String> remoteNameLocs = new HashMap<String, String>();
        File remoteFiles = Utils.join(CWD, ".gitlet", "remote");

        if (remoteFiles.exists()) {
            remoteNameLocs = Utils.readObject(remoteFiles, HashMap.class);
            if (remoteNameLocs.containsKey(remoteName)) {
                System.out.println("A remote with that name already exists.");
                return;
            } else {
                remoteNameLocs.put(remoteName, remoteLoc);
                Utils.writeObject(remoteFiles, remoteNameLocs);
            }
        } else {
            try {
                remoteFiles.createNewFile();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            remoteNameLocs.put(remoteName, remoteLoc);
            Utils.writeObject(remoteFiles, remoteNameLocs);
        }
    }

    public static void removeRemote(String[] args) {
        String remoteName = args[1];
        File remoteFiles = Utils.join(CWD, ".gitlet", "remote");
        HashMap<String, String> remoteNameLocs = Utils.readObject(remoteFiles, HashMap.class);
        if (!remoteNameLocs.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remoteNameLocs.remove(remoteName, remoteNameLocs.get(remoteName));
        Utils.writeObject(Utils.join(CWD, ".gitlet", "remote"), remoteNameLocs);
    }

    public static void fetch(String[] args) {
        String remoteName = args[1];
        String remoteBranchName = args[2];
        File remoteFiles = Utils.join(CWD, ".gitlet", "remote");
        HashMap<String, String> map = Utils.readObject(remoteFiles, HashMap.class);
        String remoteLoc = map.get(remoteName);
        File remoteFile = new File(remoteLoc);
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File remoteBranch = Utils.join(remoteLoc, "branches", remoteBranchName);
        if (!remoteBranch.exists()) {
            System.out.println("That remote does not have that branch.");
            return;
        }
        copyFromRemoteToCurr(remoteFile, remoteLoc);
        File branchFile = Utils.join
                (CWD, ".gitlet", "branches", remoteName + "/" + remoteBranchName);
        try {
            branchFile.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File remoteFile1 = Utils.join(remoteLoc, "branches", remoteBranchName);
        String branchCommit = Utils.readContentsAsString(remoteFile1);
        Utils.writeContents(branchFile, branchCommit);
    }

    public static void copyFromRemoteToCurr(File remote, String remoteLoc) {
        List<String> blobFilesInRemote = Utils.plainFilenamesIn
                (Utils.join(remote, "blobs"));
        List<String> branchFilesInRemote = Utils.plainFilenamesIn
                (Utils.join(remote, "branches"));
        List<String> commitFilesInRemote = Utils.plainFilenamesIn
                (Utils.join(remote, "commits"));
        for (String file : blobFilesInRemote) {
            File fTemp = Utils.join(CWD, ".gitlet", "blobs", file);
            if (!fTemp.exists()) {
                Path src = Paths.get(String.valueOf(Utils.join
                        (remoteLoc, "blobs", file)));
                Path dest = Paths.get(String.valueOf(Utils.join
                        (CWD, ".gitlet", "blobs", file)));
                try {
                    Files.copy(src, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        for (String file : branchFilesInRemote) {
            File fTemp = Utils.join(CWD, ".gitlet", "branches", file);
            if (!fTemp.exists()) {
                Path src = Paths.get(String.valueOf(Utils.join
                        (remoteLoc, "branches", file)));
                Path dest = Paths.get(String.valueOf(Utils.join
                        (CWD, ".gitlet", "branches", file)));
                try {
                    Files.copy(src, dest);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        for (String file : commitFilesInRemote) {
            File fTemp = Utils.join(CWD, ".gitlet", "branches", file);
            if (!fTemp.exists()) {
                Path src = Paths.get(String.valueOf(Utils.join
                        (remoteLoc, "commits", file)));
                Path dest = Paths.get(String.valueOf(Utils.join
                        (CWD, ".gitlet", "commits", file)));
                try {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void push(String[] args) {
        String remoteName = args[1];
        String branchName = args[2];
        File remoteFiles = Utils.join(CWD, ".gitlet", "remote");
        HashMap<String, String> remoteNameLocs = Utils.readObject(remoteFiles, HashMap.class);
        String remoteLoc = remoteNameLocs.get(remoteName);
        File remoteFile = new File(remoteLoc);
        if (!remoteFile.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }
        File current = Utils.join(CWD, ".gitlet");
        String remoteHead = Utils.readContentsAsString
                (Utils.join(remoteFile, "branches", branchName));
        String currentCommID = Utils.readContentsAsString
                (Utils.join(current, "branches", branchName));
        ArrayList<String> currentCommits = getHistoryForCommit
                (getCommit(currentCommID), currentCommID);
        if (!currentCommits.contains(remoteHead)) {
            System.out.println("Please pull down remote changes before pushing.");
            return;
        }

        copyFromRemoteToCurr(current, remoteLoc);
        Utils.writeContents(Utils.join(remoteLoc, "branches", branchName), currentCommID);
        return;
    }

    private static ArrayList<String> getHistoryForCommit(Commit commit, String commitID) {
        ArrayList<String> out = new ArrayList<String>();
        Commit temp = commit;

        while (temp.getPar1CommitID() != null) {
            out.add(temp.getCommitID());
            temp = getCommit(temp.getPar1CommitID());
        }
        return out;
    }
    
}
