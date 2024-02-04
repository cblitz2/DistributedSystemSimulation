import java.io.*;
import java.net.*;
import java.util.*;

// Server class
public class Master
{
    private Socket clientSocket1;
    private Socket clientSocket2;
    private Socket slaveASocket;
    private Socket slaveBSocket;
    private static ServerSocket ssClient;
    private static ServerSocket ssSlaveA;
    private static ServerSocket ssSlaveB;
    static int aCounter = 0;
    static int bCounter = 0;
    static int numJobs = 0;
    static int numCompletedJob = 0;
    static boolean complete1 = false;
    static boolean complete2 = false;
    int id = 0;
    public Master()
    {

        try
        {
            //set up connection to Client
            ssClient = new ServerSocket(1234);
            System.out.println("Waiting for the client request");
            clientSocket1 = ssClient.accept();
            System.out.println("Client accepted");
            System.out.println("Waiting for the client request");
            clientSocket2 = ssClient.accept();
            System.out.println("Client accepted");


            //set up connection to Slaves
            ssSlaveA = new ServerSocket(5678);
            System.out.println("Waiting for the Slave A request");
            slaveASocket = ssSlaveA.accept();
            System.out.println("Slave A accepted");

            ssSlaveB = new ServerSocket(9000);
            System.out.println("Waiting for the Slave B request");
            slaveBSocket = ssSlaveB.accept();
            System.out.println("Slave B accepted");


            // obtaining input and out streams Master to Client
            PrintWriter clientWriter1 = new PrintWriter(clientSocket1.getOutputStream(), true);
            BufferedReader clientReader1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));

            PrintWriter clientWriter2 = new PrintWriter(clientSocket2.getOutputStream(), true);
            BufferedReader clientReader2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));

            //input/output streams for Master to SlaveA
            PrintWriter slaveAWriter = new PrintWriter(slaveASocket.getOutputStream(), true);
            BufferedReader slaveAReader = new BufferedReader(new InputStreamReader(slaveASocket.getInputStream()));

            //input/output streams for Master to SlaveB
            PrintWriter slaveBWriter = new PrintWriter(slaveBSocket.getOutputStream(), true);
            BufferedReader slaveBReader = new BufferedReader(new InputStreamReader(slaveBSocket.getInputStream()));

            //make arraylist that will hold jobs
            ArrayList<String> jobs = new ArrayList<>();
            List<String> jobsList = Collections.synchronizedList(jobs);
            ArrayList<String> client1Jobs = new ArrayList<>();
            List<String> client1JobsList = Collections.synchronizedList(client1Jobs);
            List<String> completedJobs = new ArrayList<>();
            List<String> completedJobsList = Collections.synchronizedList(completedJobs);


            //Write to Client
            Thread mastWriteToClient = new Thread(() ->
            {
                while(completedJobsList.isEmpty()); //spin if empty
                while(!(numCompletedJob == numJobs && complete1 && complete2))
                {
                    if (!completedJobsList.isEmpty()){
                        System.out.println("Telling Client Job " + completedJobsList.get(0) + " Completed");
                        if(client1JobsList.contains(completedJobsList.get(0)))
                        {
                            clientWriter1.println(completedJobsList.remove(0) + " Completed");
                        }
                        else
                        {
                            clientWriter2.println(completedJobsList.remove(0) + " Completed");
                        }
                    }
                }
                clientWriter1.println("All jobs complete");
                clientWriter2.println("All jobs complete");
                System.out.println("All jobs complete.");
            });


            //Reads all instructions from Client
            Thread mastReadFromClient1 = new Thread(() ->
            {
                try
                {
                    String message = clientReader1.readLine();
                    while (!message.equals("done"))
                    {
                        synchronized(this)
                        {
                            String job = id + ": " + message;
                            jobs.add(job);
                            System.out.println("Received job " + job + " from Client 1");
                            client1Jobs.add(job);
                            numJobs++;
                            id++;
                        }
                        message = clientReader1.readLine();
                    }
                    complete1 = true;
                }
                catch (IOException e)
                {
                    System.out.println("Error:" + e.getMessage());
                }
            });

            Thread mastReadFromClient2 = new Thread(() ->
            {
                try
                {
                    String message = clientReader2.readLine();
                    while (!message.equals("done"))
                    {
                        synchronized (this)
                        {
                            String job = id + ": " + message;
                            jobs.add(job);
                            System.out.println("Received job " + job + " from Client 2");
                            numJobs++;
                            id++;
                        }
                        message = clientReader2.readLine();
                    }
                    complete2 = true;
                }
                catch (IOException e)
                {
                    System.out.println("Error:" + e.getMessage());
                }
            });



            Thread mastWriteToSlave = new Thread(() ->
            {
                int done = 0;
                while(jobsList.isEmpty()); //spin
                while(done<2)
                {
                    if(!jobsList.isEmpty())
                    {
                        String job = jobsList.remove(0);
                        //write to Slave
                        if (job.endsWith("A"))
                        {
                            if (aCounter > (bCounter + 10))
                            {
                                synchronized (this)
                                {
                                    bCounter += 10;
                                }
                                slaveBWriter.println(job);
                                System.out.println("Writing job " + job + " to Slave B");
                            }
                            else
                            {
                                synchronized (this)
                                {
                                    aCounter += 2;
                                }
                                slaveAWriter.println(job);
                                System.out.println("Writing job " + job + " to Slave A");
                            }
                        }
                        else if (job.endsWith("B"))
                        {
                            if (bCounter > (aCounter + 10))
                            {
                                synchronized (this)
                                {
                                    aCounter += 10;
                                }
                                slaveAWriter.println(job);
                                System.out.println("Writing job " + job + " to Slave A");
                            }
                            else
                            {
                                synchronized (this)
                                {
                                    bCounter += 2;
                                }
                                slaveBWriter.println(job);
                                System.out.println("Writing job " + job + " to Slave B");
                            }
                        }
                        else if (job.endsWith("done"))
                        {
                            done++;
                        }
                    }
                }
                slaveAWriter.println("done");
                slaveBWriter.println("done");
                System.out.println("Writing done to A & B");
            });

            //Read from SlaveA
            Thread mastReadFromSlaveA = new Thread(() ->
            {
                try
                {
                    String message = slaveAReader.readLine();
                    while (!message.equals("Slave A Jobs Complete"))
                    {
                        System.out.println("Slave A Completed Job " + message);
                        if (message.endsWith("A"))
                        {
                            synchronized (this)
                            {
                                aCounter -= 2;
                            }
                        }
                        else if (message.endsWith("B"))
                        {
                            synchronized (this)
                            {
                                aCounter -= 10;
                            }
                        }
                        synchronized (this)
                        {
                            completedJobs.add(message);
                            numCompletedJob++;
                        }
                        message = slaveAReader.readLine();
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });

            //Read from SlaveB
            Thread mastReadFromSlaveB = new Thread(() ->
            {
                try
                {
                    String message = slaveBReader.readLine();
                    while (!message.equals("Slave B Jobs Complete"))
                    {
                        System.out.println("Slave B Completed Job " + message);
                        if (message.endsWith("B"))
                        {
                            synchronized (this)
                            {
                                bCounter -= 2;
                            }
                        }
                        else if (message.endsWith("A"))
                        {
                            synchronized (this)
                            {
                                bCounter -= 10;
                            }
                        }
                        synchronized (this)
                        {
                            completedJobs.add(message);
                            numCompletedJob++;
                        }
                        message = slaveBReader.readLine();
                    }
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            });

            mastReadFromClient1.start();
            mastReadFromClient2.start();
            mastWriteToSlave.start();
            mastReadFromSlaveA.start();
            mastReadFromSlaveB.start();
            mastWriteToClient.start();

            mastReadFromClient1.join();
            mastReadFromClient2.join();
            mastWriteToSlave.join();
            mastWriteToClient.join();
            mastReadFromSlaveA.join();
            mastReadFromSlaveB.join();

            //Close all resources
            clientReader1.close();
            clientWriter1.close();
            clientSocket1.close();
            clientReader2.close();
            clientWriter2.close();
            clientSocket2.close();
            ssClient.close();
            slaveASocket.close();
            ssSlaveA.close();
            slaveBSocket.close();
            ssSlaveB.close();

        }
        catch (Exception e)
        {
            System.out.println("Error: " + e.getMessage());
        }


    }

    public static void main(String[] args)
    {
        Master master = new Master();
    }

}