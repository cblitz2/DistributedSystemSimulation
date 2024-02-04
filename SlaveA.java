import java.io.*;
import java.net.*;
import java.util.*;

public class SlaveA
{

    public static void main(String[] args) throws IOException, InterruptedException
    {
        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection with server port 5678
        Socket socket = new Socket(ip, 5678);

        // obtaining input and out streams
        PrintWriter clientWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        List<String> slaveAJobs = new ArrayList<>();
        List<String> slaveAJobsList = Collections.synchronizedList(slaveAJobs);


        //Read from Master
        Thread slaveAReadFromMast = new Thread(() ->
        {
            String message = "";
            try
            {
                do
                {
                    message = responseReader.readLine();
                    System.out.println("Job From Master: " + message);
                    slaveAJobs.add(message);
                }
                while (!message.equals("done"));
            }
            catch (IOException e)
            {
                System.out.println("Error:" + e.getMessage());
            }
        });


        //Write to Master
        Thread slaveAWriteToMast = new Thread(() ->
        {
            while(slaveAJobsList.isEmpty()); //spin if empty
            while(!slaveAJobsList.get(0).equals("done"))
            {
                if (slaveAJobsList.get(0).endsWith("A"))
                {
                    try
                    {
                        System.out.println("Working on " + slaveAJobsList.get(0) + " --> Sleep for 2 seconds");
                        Thread.sleep(2000);
                        System.out.println("Completed job " + slaveAJobsList.get(0));
                        clientWriter.println(slaveAJobsList.remove(0));
                        clientWriter.flush();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else if (slaveAJobsList.get(0).endsWith("B"))
                {
                    try
                    {
                        System.out.println("Working on " + slaveAJobsList.get(0) + " --> Sleep for 10 seconds");
                        Thread.sleep(10000);
                        System.out.println("Completed job " + slaveAJobsList.get(0));
                        clientWriter.println(slaveAJobsList.remove(0));
                        clientWriter.flush();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                while(slaveAJobsList.isEmpty());
            }
            clientWriter.println("Slave A Jobs Complete");
        });

        slaveAReadFromMast.start();
        slaveAWriteToMast.start();

        slaveAWriteToMast.join();
        slaveAReadFromMast.join();

        //Close all resources
        clientWriter.close();
        responseReader.close();
        socket.close();

    }
}