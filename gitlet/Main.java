package gitlet;

import java.io.File;

import static gitlet.Utils.join;

/** Driver class for Gitlet, a subset of the Git version-control system.
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args == null || args.length == 0){
            System.out.println("Please enter a command.");
            return;
        } if (!Utils.join(new File(System.getProperty("user.dir")), ".gitlet").exists() && !args[0].equals("init")){
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        String firstArg = args[0];
        Main self = new Main();

        switch(firstArg) {
            case "init":
                // handle the `init` command
                Repository repo = new Repository(args);
                break;
            case "add":
                // handle the `add [filename]` command
                Repository.add(args);
                break;
            case "commit":
                Repository.commit(args);
                break;
            case "checkout":
                Repository.checkout(args);
                break;
            case "log":
                Repository.log();
                break;
            case "rm":
                Repository.remove(args);
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                Repository.find(args);
                break;
            case "status":
                Repository.status();
                break;
            case "branch":
                Repository.createBranch(args);
                break;
            case "rm-branch":
                Repository.removeBranch(args);
                break;
            case "reset":
                Repository.reset(args);
                break;
            case "merge":
                Repository.merge(args);
                break;
            case "add-remote":
                Repository.addRemote(args);
                break;
            case "rm-remote":
                Repository.removeRemote(args);
                break;
            case "fetch":
                Repository.fetch(args);
                break;
            case "push":
                Repository.push(args);
                break;
            default:
                System.out.println("No command with that name exists.");
                break;
        }

    }
}
