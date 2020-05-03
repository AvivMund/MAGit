package magit.engine;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

public class Utils {
    public static void writeToTextFile (String fullPath, String text) {
        try (PrintWriter writer = new PrintWriter(fullPath, "UTF-8")){
            Files.createDirectories(Paths.get(fullPath).getParent());
            writer.println(text);
        } catch (FileNotFoundException e) {
            System.out.println("Problem when saving file " + fullPath);
        } catch (UnsupportedEncodingException e) {
            System.out.println("Problem when saving file " + fullPath);
        } catch (IOException e) {
            System.out.println("Problem when creating directory for file " + fullPath);
            e.printStackTrace();
        }
    }

    public static String readFromTextFile(String fullPath) {
        String data = null;
        File file = new File(fullPath);
        try (Scanner scanner = new Scanner(file)) {
            data = scanner.nextLine();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return data;
    }
}
