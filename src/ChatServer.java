import java.io.FileNotFoundException;
import java.util.Scanner;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;
import java.text.SimpleDateFormat;

final class ChatServer {
    private static int uniqueId = 0;
    private final List<ClientThread> clients = new ArrayList<>();
    private final int port;
    private final ChatFilter filter;
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    Date date = new Date();

    private ChatServer() throws FileNotFoundException {
        try {
            filter = new ChatFilter("src/badwords.txt");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Error: Filepath does not exist!");
        }

        filter.listBadWords();
        port = 1503;

    }

    private ChatServer(int port) throws FileNotFoundException {
        try {
            filter = new ChatFilter("src/badwords.txt");
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Error: Filepath does not exist!");
        }

        filter.listBadWords();
        this.port = port;
    }

    private ChatServer(int port, String filename) throws FileNotFoundException {
        try {
            filter = new ChatFilter(filename);
        } catch (FileNotFoundException e) {
            throw new FileNotFoundException("Error: Filepath does not exist!");
        }

        filter.listBadWords();
        this.port = port;
    }

    /*
     * This is what starts the ChatServer.
     * Right now it just creates the socketServer and adds a new ClientThread to a list to be handled
     */
    private void start() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println(formatter.format(date) + " Server waiting for Clients on port "
                    + port + ".");
            while (true) {
                Socket socket = serverSocket.accept();
                Runnable r = new ClientThread(socket, uniqueId++);
                Thread t = new Thread(r);
                clients.add((ClientThread) r);
                t.start();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    /**sends a message to all the chat cliets
     * This goes through a loop of all the clients and tries to
     * send a message to each one.
     *
     */
    private synchronized void broadcast(String message) {
        for(ClientThread temp : clients){
            temp.writeMessage(message);
        }
    }


    /**
     * Sends a direct message to the specified recipient.
     * Will print the message to both the receiver and sender.
     * @param message the message to be sent.
     * @param sender the user sending the message.
     * @param recipient the user receiving the message.
     */
    private synchronized void directMessage(String message, String sender, String recipient) {
        for (ClientThread temp: clients) {
            if (temp.username.equalsIgnoreCase(recipient) || temp.username.equalsIgnoreCase(sender)) {
                temp.writeMessage(message);
            }
        }
    }

    /**
     * Returns a list of the current users to whoever typed in the command.
     * It takes a username in as a parameter and will fill a return string
     * with all the usernames not including who typed the command.
     * @param username the username of whoever typed the command.
     */
    private synchronized void listUsers(String username) {
        String listString = "Active Users: " + "\n";
        ClientThread receiver = null;
        if (clients.size() == 1) {
            clients.get(0).writeMessage("You are the only user in the chat server!");
        } else {
            for (ClientThread temp : clients) {
                if (!temp.username.equals(username)) {
                    listString += temp.username + "\n";
                } else {
                    receiver = temp;
                }
            }
            if (receiver != null) {
                receiver.writeMessage(listString);
            }
        }
    }

    /*
     *  > java ChatServer
     *  > java ChatServer portNumber
     *  If the port number is not specified 1500 is used
     */
    public static void main(String[] args) {
        ChatServer server;
        switch (args.length) {
            case 0:
                try {
                    server = new ChatServer();
                } catch (FileNotFoundException e) {
                    System.out.println("Error file not found for censoring! Make sure file is specified correctly!");
                    return;
                }
                break;
            case 1:
                try {
                    server = new ChatServer(Integer.parseInt(args[0]));
                } catch (FileNotFoundException e) {
                    System.out.println("Error file not found for censoring! Make sure file is specified correctly!");
                    return;
                }
                break;
            case 2:
                try {
                    server = new ChatServer(Integer.parseInt(args[0]), args[1]);
                } catch (FileNotFoundException e) {
                    System.out.println("Error file not found for censoring! Make sure file is specified correctly!");
                    return;
                }
                break;
            default:
                System.out.println("Error occurred when creating the server! Make sure you are not using too" +
                        " many arguments!");
                return;
        }
        server.start();
    }


    /*
     * This is a private class inside of the ChatServer
     * A new thread will be created to run this every time a new client connects.
     */
    private final class ClientThread implements Runnable {
        Socket socket;
        ObjectInputStream sInput;
        ObjectOutputStream sOutput;
        int id;
        String username;
        ChatMessage cm;

        private ClientThread(Socket socket, int id) throws IllegalArgumentException {
            this.id = id;
            this.socket = socket;
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                username = (String) sInput.readObject();
                if (clients.size() > 0) {
                    for (ClientThread temp: clients) {
                        if (temp.getUsername().equalsIgnoreCase(username)) {
                            sOutput.writeObject("Username has already been taken!");
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        /** This sends the message to the chat client connected,
         * and does it weather or not the person sent it
         *
         *
         * NEEEDs to check if socket is connected?
         *
         */

        private boolean writeMessage(String msg) {

            try {
                sOutput.writeObject(msg);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            return true;

        }

        private String getUsername() {
            return username;
        }

        /**
         * The remove method.
         * This method takes a ClientThread ID as a parameter and
         * removes the ClientThread from the clients ArrayList that shares
         * the ID.
         *
         * The method is synchronized to ensure that there is no concurrency issues
         * when removing the client from the clients ArrayList.
         *
         * Use this method when a user disconnects to remove their ClientThread from the list of active threads.
         *
         * @param theID the ID of the ClientThread object to be removed.
         */
        public synchronized void remove(int theID) {
            //Iterate across the clients ArrayList.
            for (ClientThread temp: clients) {
                //If the ID of the current ClientThread object matches the given ID.
                if (temp.id == theID) {
                    try {
                        //Remove the object from the clients ArrayList.
                        temp.socket.close();
                        temp.sInput.close();
                        temp.sOutput.close();
                        clients.remove(temp);
                        break;
                    } catch (IOException e){
                        return;
                    }

                }
            }
        }

             /*
          Iterator<ClientThread> iter = clients.iterator();
            while(iter.hasNext()){
                ClientThread thread = iter.next();
                if(theID == thread.id){
                    iter.remove();
                }
            }
         */

        /*
         * This is what the client thread actually runs.
         */
        @Override
        public void run() {
            System.out.println(formatter.format(date) + " " + username + " just connected");
            System.out.println(formatter.format(date) + " Server waiting for Clients on port "
                    + port + ".");
            while (true) {
                String message = null;
                try {
                    cm = (ChatMessage) sInput.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    remove(id);
                    System.out.println(username + " just forced logged out.");
                    break;
                }

                //If the message is a logout message
                if (cm.getNum() == 1) {
                    message = cm.getStr();
                    remove(id);
                    System.out.println(cm.getStr());
                    break;
                    //If a recipient was specified when creating the message, send a direct message.
                } else if (cm.getRecipient() != null) {

                    //The substring method is used to get the message excluding the "/msg user"
                    String lul = formatter.format(date) + " " + username + " -> "
                            + cm.getRecipient() + ": " + cm.getStr().substring(cm.getStr()
                            .indexOf(cm.getRecipient()) + cm.getRecipient().length()) + "\n";
                    message = lul;
                    //Censor the string

                    directMessage(filter.filter(lul), cm.getSender(), cm.getRecipient());
                    //Send a broadcast message to all members of the server
                } else if (cm.getStr().contains("/list")) {
                    //Gets the username and passes it to the method. The username is always preceded by a space.
                    listUsers(cm.getStr().substring(cm.getStr().indexOf(" ") + 1));
                    message = cm.getStr();
                } else {
                    //Censor the string
                    String lul = formatter.format(date) + " " + username + ": "
                            + cm.getStr() + "\n";
                    broadcast(filter.filter(lul));
                    message = lul;
                }
                //Print the message out server side.
                System.out.println(message);


                /*try {
                    sOutput.writeObject(username + ": " + cm.getStr() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                */
            }
        }
    }
}
