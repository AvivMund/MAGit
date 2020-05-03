package magit.ui;

import magit.EngineInterface;
import magit.engine.*;
import magit.engine.xml.XmlLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Ui {
    private EngineInterface engine;
    private String menu = "Magit Menu      Current User: %1$s       Current Repository Location: %2$s\n" +
            "0.  Create new repository\n" +
            "1.  Change user name\n" +
            "2.  Load from XML\n" +
            "3.  Switch repository\n" +
            "4.  Show current commit file system information\n" +
            "5.  Working copy status\n" +
            "6.  Commit\n" +
            "7.  List available branches\n" +
            "8.  Create a new branch\n" +
            "9.  Delete branch\n" +
            "10. Checkout branch\n" +
            "11. Show current branch history\n" +
            "12. Exit\n" +
            "==========================================\n" +
            "Please enter your choice: ";

    public Ui(EngineInterface newEngine) {
        engine= newEngine;
    }

    private void mainMenu() {
        System.out.print(String.format(menu, engine.getUsername(), engine.getCurrRepoName()));
    }

    public void start() {
        int choice = 0;
        Scanner in = new Scanner(System.in);
        boolean check;

        do {
            mainMenu();
            do{
                check = true;
                try{
                    choice = in.nextInt();
                    if(choice < 0 || choice > 12)
                    {
                        System.out.print("Error, you must enter a number in range 0-12\n" +
                                "Please enter your choice: ");
                        check = false;
                    }
                }
                catch (InputMismatchException e) {
                    System.out.println("Input error - Invalid value for an int.");
                    in.nextLine();
                    System.out.print("Please enter your choice: ");
                    check = false;
                }
            } while (!check);
            routeSelection(choice);
        }while(choice != 12);
    }

    private void routeSelection(int choice) {
        switch(choice)
        {
            case 0:
                creatNewRepository();
                break;
            case 1:
                updateUsername();
                break;
            case 2:
                try {
                    loadRepository();
                } catch (Exception e) {
                    System.out.println("Path is not legal or not found");
                }
                break;
            case 3:
                switchActiveRepo();
                break;
            case 4:
                showCurrentCommit();
                break;
            case 5:
                showWCStatus();
                break;
            case 6:
                makeACommit();
                break;
            case 7:
                showBranches();
                break;
            case 8:
                createNewBranch();
                break;
            case 9:
                deleteBranch();
                break;
            case 10:
                checkout();
                break;
            case 11:
                showActiveBranchHistory();
                break;
            case 12:
                System.out.println("BYE BYE!");
                break;

                default:
                    System.out.println("Error");


        }
    }

    private String getInput(String message) {
        Scanner in = new Scanner(System.in);
        System.out.print(message);
        String input = in.nextLine();
        return input;
    }

    private void creatNewRepository() {
        String repoName = getInput("Please enter name of new repository: ");
        String repoLocation = getInput("Please enter repository location(full path): ");
        System.out.println(engine.createRepo(repoName, repoLocation));
    }

    private void updateUsername() {
        engine.setUsername(getInput("Please enter new user name: "));
    }

    private void switchActiveRepo() {
        String fullPath = getInput("Please enter full path of the repository location: ");
        System.out.println(engine.switchCurrentRepo(fullPath));
    }

    private void showCurrentCommit() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }

        Folder headTree = engine.getHeadTree();
        if (headTree == null) {
            System.out.println("Unfortunately, There are no commits in the current repository");
            return;
        }

        System.out.println("Current commit's Items:");
        System.out.print(loadItemsData(headTree, engine.getCurrRepoLocation()));
    }

    private StringBuilder loadItemsData(Item currItem, String repoLocation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Name: " + currItem.getFullPath() + "\n");
        sb.append("Item type: " + currItem.getType() + "\n");
        sb.append("SHA-1: " + currItem.getSha1() + "\n");
        sb.append("Author: " + currItem.getLastUpdater() + "\n");
        sb.append("Update time: " + currItem.getLastUpdateDate() + "\n");
        sb.append("======================================================\n");
        if (currItem.getType() == ItemType.FOLDER) {
            List<Item> children = ((Folder)currItem).getItems();
            for (Item child : children) {
                sb.append(loadItemsData(child,repoLocation));
            }
        }

        return sb;
    }

    private void showWCStatus() {
        String currRepoName = engine.getCurrRepoName();
        if (currRepoName.equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        String currRepoLocation = engine.getCurrRepoLocation();
        String userName = engine.getUsername();
        Map<ChangeType, List<Item>> wcStatus = engine.getWCStatus();

        StringBuilder outputSb = new StringBuilder();
        outputSb.append("Current repository name: " + currRepoName + System.lineSeparator());
        outputSb.append("Current repository location: " + currRepoLocation + System.lineSeparator());
        outputSb.append("Current user name: " + userName + System.lineSeparator());
        outputSb.append("Deleted files compared to the current commit:" + System.lineSeparator());
        outputSb.append(getItems(wcStatus.get(ChangeType.DELETED)));
        outputSb.append("Updated files compared to the current commit:" + System.lineSeparator());
        outputSb.append(getItems(wcStatus.get(ChangeType.UPDATED)));
        outputSb.append("Created files compared to the current commit:" + System.lineSeparator());
        outputSb.append(getItems(wcStatus.get(ChangeType.CREATED)));

        System.out.print(outputSb);
    }

    private StringBuilder getItems(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append(item.getFullPath().toString() + System.lineSeparator());
        }
        return sb;
    }

    private void makeACommit() {
        String commitMessage = getInput("Please enter the description of the commit: ");
        System.out.println(engine.createCommit(commitMessage));
    }

    private void showBranches() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        StringBuilder outputSb = new StringBuilder("Branches:" + System.lineSeparator());
        Map<String, Branch> branches = engine.getBranches();
        Branch headBranch = engine.getHeadBranch();
        for (Map.Entry<String, Branch> entry : branches.entrySet()) {
            Branch currBranch = entry.getValue();
            if (currBranch == headBranch) {
               outputSb.append("**Head Branch**" + System.lineSeparator());
            }
            outputSb.append("Branch name: " + currBranch.getName() + System.lineSeparator());
            outputSb.append("Commit sha-1: " + currBranch.getCommitSha1() + System.lineSeparator());
            outputSb.append("Commit message: " + currBranch.getCommit().getMessage() + System.lineSeparator());
        }

        System.out.print(outputSb);
    }

    private void createNewBranch() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        String branchName = getInput("Please enter new branch name: ");
        System.out.println(engine.createBranch(branchName, null));
    }

    private void deleteBranch() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        String branchName = getInput("Please enter branch name to delete: ");
        System.out.println(engine.deleteBranch(branchName));
    }

    private void checkout() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        String branchName = getInput("Please enter branch name for checkout: ");
        String problemMessege = engine.beforeCheckout(branchName);
        String userAnswer = null;
        if (problemMessege != null) {
            System.out.println(problemMessege);
            if (problemMessege.endsWith("information!")) {
                userAnswer = getInput("Do you still want to continue ?\nPress-\n1- Yes\n2- NO\nYour choice(1 or 2): ");
                while(true) {
                    if(userAnswer.equals("1")){
                        break;
                    } else if (userAnswer.equals("2")) {
                        return;
                    } else {
                        userAnswer = getInput("Invalid input!\nPress-\n1- Yes\n2- NO\nYour choice(1 or 2): ");
                    }
                }
                engine.checkout(branchName);
            } else {
                return;
            }
        } else {
            engine.checkout(branchName);
        }
        System.out.println("Checkout performed successfully!");
    }

    private void showActiveBranchHistory() {
        if (engine.getCurrRepoName().equals("none")) {
            System.out.println("Unfortunately, There is no active repository in the system");
            return;
        }
        List<Commit> commits = engine.getActiveBranchCommitsHistory();
        if (commits.size() > 0) {
            StringBuilder outputSB = new StringBuilder("Active branch history:\n");
            for (Commit commit : commits) {
                outputSB.append("Commit SHA-1: " + commit.calculateSha1() + "\n");
                outputSB.append("Message: " + commit.getMessage() + "\n");
                outputSB.append("Author: " + commit.getAuthor() + "\n");
                outputSB.append("Date: " + commit.getDate() + "\n");
                outputSB.append("==================================================\n");
            }

            System.out.print(outputSB);
        } else {
            System.out.println("Unfortunately, There are no commits for the current repository");
        }
    }

    private void loadRepository() throws IOException {
        String fullPath = getInput("Please enter full path of the xml file: ");
        Path xmlPath = Paths.get(fullPath);
        if (!fullPath.endsWith(".xml")) {
        //if (!xmlPath.endsWith(".xml")) {
            System.out.println("ERROR: the path you entered does not end with '.xml'");
            return;
        } else if (!xmlPath.isAbsolute() || !Files.exists(xmlPath)) {
            System.out.println("ERROR: the path you entered is not absolute or not exist");
            return;
        }

        XmlLoader xmlLoader = new XmlLoader(fullPath);
        if (xmlLoader.isValid()) {
            if (!Files.exists(Paths.get(xmlLoader.getRepo().getLocation()))) {
                Files.createDirectory(Paths.get(xmlLoader.getRepo().getLocation()));
            }
            if (Files.exists(Paths.get(xmlLoader.getRepo().getLocation() + RepoManager.MAGIT_PATH))) {
                String userAnswer = getInput("There is already other repository in the xml repo location, do you want to continue and lose it?\nPress-\n1- Yes\n2- NO\nYour choice(1 or 2): ");
                while(true) {
                    if(userAnswer.equals("1")){
                        break;
                    } else if (userAnswer.equals("2")) {
                        return;
                    } else {
                        userAnswer = getInput("Invalid input!\nPress-\n1- Yes\n2- NO\nYour choice(1 or 2): ");
                    }
                }
                System.out.println(engine.loadRepoFromXml(xmlLoader, true));
            } else if (Files.list(Paths.get(xmlLoader.getRepo().getLocation())).count() != 0) {
                System.out.println("ERROR: the operation canceled because the repository location contains other data");
            } else {
                System.out.println(engine.loadRepoFromXml(xmlLoader, false));
            }
        } else {
            System.out.println(xmlLoader.getErrorMessage());
        }
    }
}
