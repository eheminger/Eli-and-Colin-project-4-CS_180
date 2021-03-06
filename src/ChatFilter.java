import java.io.*;
import java.util.ArrayList;

public class ChatFilter {
    private BufferedReader br;
    private String badWordsFile;
    private ArrayList<String> badWords = new ArrayList<>();
    /**
     * The ChatFilter constructor.
     * The constructor takes in a filepath that leads to the badWordsFileName
     * and attempts to create a buffered reader that reads from that file.
     * If the filepath does not exist, a FileNotFoundException will be thrown.
     * @param badWordsFilePath the filepath that contains the words to be censored.
     * @throws FileNotFoundException
     */
    public ChatFilter(String badWordsFilePath) throws FileNotFoundException {
        //Check if the filepath exists
        badWordsFile = badWordsFilePath.substring(badWordsFilePath.lastIndexOf("/") + 1);
        if (!new File(badWordsFilePath).exists()) {
            throw new FileNotFoundException("Error: file specified in the the creation of the server does not exist!");
        } else {
            br = new BufferedReader(new FileReader(badWordsFilePath));
        }

        //The array that will store all the bad words
        String line;
        try {
            //Create an ArrayList containing all the bad words in the badWordsFile.
            while ((line = br.readLine()) != null) {
                badWords.add(line);
            }
        } catch (IOException e) {
            //If there ends up being some error in reading the file
            System.out.println("Error in reading process! A null value will be returned");
        }

    }

    /**
     * Lists all the bad words in the file.
     */
    public synchronized void listBadWords() {
        System.out.println("Banned Words File: " + badWordsFile);
        System.out.println("Banned Words:");
        for (String temp: badWords) {
            System.out.println(temp);
        }
    }

    /**
     * The filter method.
     * This method takes in a message as a parameter, and then censors all
     * instances of words from the badWords
     * file.
     * If no words were censored, then the normal string is returned
     * @param msg the message to be censored.
     * @return the censored message.
     */
    public synchronized String filter(String msg) {

        //Begin checking for bad words
        for (String temp: badWords) {
            if (msg.toLowerCase().contains(temp.toLowerCase()) || msg.toUpperCase().contains(temp.toUpperCase())) {
                String replaceString = "";
                for (int i = 0; i < temp.length(); i++) {
                    replaceString += "*";
                }
                ArrayList<Integer> indexes = new ArrayList<>();
                int index = 0;
                while (msg.toUpperCase().indexOf(temp.toUpperCase(), index) > 0) {
                    indexes.add(msg.toUpperCase().indexOf(temp.toUpperCase(), index));
                    index = msg.toUpperCase().indexOf(temp.toUpperCase(), index) + 1;
                }
                for (int i: indexes) {
                    msg = msg.replace(msg.substring(i, i + temp.length()), replaceString);
                }
            }
        }
        return msg;
    }
}
