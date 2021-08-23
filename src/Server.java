import java.io.*;
import java.net.*;

public class Server
{
    private static boolean gameReady = false;

    public static void main(String[] args)
    {
        // Declare a server socket & a client socket for the server
        ServerSocket service = null;
        TCPClientHandler tcp = new TCPClientHandler();

        // Try to open a server socket on port 5000
        try
        {
            service = new ServerSocket(5000);
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

        // Create a socket object from the ServerSocket to listen & accept
        // connections. Open input and output streams
        try
        {
            int maxClients = 2;
            int activeClients = 0;
            Socket server = null;

            do  // while active clients less than 2. Server listens for incoming clients
            {
                server = service.accept();

                TCPClientHandler handler = new TCPClientHandler(server);

                Thread t = new Thread(handler);
                t.start();
                activeClients++;

                if(activeClients == 2)
                {
                    gameReady = true;
                }

            } while(activeClients < maxClients);

        } // end of try
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }

        // If a player disconnects, opponent will be notified & shut down. The server socket will also be closed down
      while (tcp.getPlayerDisconnected())
      {
          try
          {
              service.close();
          }
              catch (Exception e)
              {
                  System.out.println(e.getMessage());
              }
      }


    } // End of main

   // Used in TCPClientHandler to get status of a game
    public boolean getGameReady()
    {
        return gameReady;
    }

} // End of Server class