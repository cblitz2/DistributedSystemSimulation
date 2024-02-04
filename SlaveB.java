import java.io.*;
import java.net.*;
import java.util.*;

public class SlaveB
{
    public static void main(String[] args) throws IOException, InterruptedException
    {
        // getting localhost ip
        InetAddress ip = InetAddress.getByName("localhost");

        // establish the connection with server port 9000
        Socket socket = new Socket(ip, 9000);

        // obtaining input and out streams
        PrintWriter clientWriter = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader responseReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        List<String> slaveBJobs = new ArrayList<>();
        List<String> slaveBJobsList = Collections.synchronizedList(slaveBJobs);

        //Read from Master
        Thread slaveBReadFromMast = new Thread(() ->
        {
            String message = "";
            try
            {
                do
                {
                    message = responseReader.readLine();
                    System.out.println("Job From Master: " + message);
                    slaveBJobs.add(message);
                }
                while (!message.equals("done"));
            }
            catch (IOException e)
            {
                System.out.println("Error:" + e.getMessage());
            }
        });


        //Write to Master
        Thread slaveBWriteToMast = new Thread(() ->
        {
            while(slaveBJobsList.isEmpty()); //spin if empty
            while(!slaveBJobsList.get(0).equals("done"))
            {
                if(slaveBJobsList.get(0).endsWith("B"))
                {
                    try
                    {
                        System.out.println("Working on " + slaveBJobsList.get(0) + " --> Sleep for 2 seconds");
                        Thread.sleep(2000);
                        System.out.println("Completed job " + slaveBJobsList.get(0));
                        clientWriter.println(slaveBJobsList.remove(0));
                        clientWriter.flush();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else if (slaveBJobsList.get(0).endsWith("A"))
                {
                    try
                    {
                        System.out.println("Working on " + slaveBJobsList.get(0) + " --> Sleep for 10 seconds");
                        Thread.sleep(10000);
                        System.out.println("Completed job " + slaveBJobsList.get(0));
                        clientWriter.println(slaveBJobsList.remove(0));
                        clientWriter.flush();
                    }
                    catch (InterruptedException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                while(slaveBJobsList.isEmpty()); //spin if empty
            }
            clientWriter.println("Slave B Jobs Complete");

        });
        slaveBReadFromMast.start();
        slaveBWriteToMast.start();

        slaveBWriteToMast.join();
        slaveBReadFromMast.join();

        //Close all resources
        clientWriter.close();
        responseReader.close();
        socket.close();
    }
}