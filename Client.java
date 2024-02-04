import java.io.*;
import java.net.*;
import java.util.*;

// Client class 
public class Client
{
    public Client(int port)
    {
        try
        {

            // establish the connection with server port
            Socket socket = new Socket("127.0.0.1", port);
            System.out.println("Connected to Master");


            // obtaining input and out streams
            PrintWriter clientWriter = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            Scanner s = new Scanner(System.in);

            //Writes all jobs to Master
            Thread clientWriteToMast = new Thread(() ->
            {
                //Take user input
                boolean done = false;
                while (!done)
                {
                    System.out.println("Which Job? A, B, or any key to terminate");
                    String key = s.nextLine();
                    synchronized (this)
                    {
                        if (key.toUpperCase().equals("A") || key.toUpperCase().equals("B"))
                        {
                            System.out.println("Received job " + key.toUpperCase() + " from user.");
                            clientWriter.println(key.toUpperCase());
                            System.out.println("Sending job " + key.toUpperCase() + " to Master.");
                            clientWriter.flush();
                        }
                        else
                        {
                            done = true;
                        }
                    }
                }
                clientWriter.println("done");
            });


            //Read from Master
            Thread clientReadFromMast = new Thread (() ->
            {
                try
                {
                    String message = responseReader.readLine();
                    while (!message.equals("All jobs complete"))
                    {
                        System.out.println("Job " + message);
                        message = responseReader.readLine();
                    }
                    System.out.println("All jobs completed.");
                }
                catch (IOException ex)
                {
                    throw new RuntimeException(ex);
                }
            });

            clientReadFromMast.start();
            clientWriteToMast.start();

            clientReadFromMast.join();
            clientWriteToMast.join();

            responseReader.close();
            clientWriter.close();
            socket.close();

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }
    public static void main(String[] args)
    {
        Client client = new Client(1234);
    }
}