import java.lang.*;
import java.io.*;
import java.net.*;

class TCPClientHandler implements Runnable
{
    private Socket server = null;
    private Server serv = new Server();
    // incremented after a player opts to play again or sends car choice. Game starts if setupPlayers = 2
    private static int setupPlayers = 0;

    private String line; // The message from the Client
    private ObjectInput input = null; // used to read objects (a car) and messages (a string)
    private ObjectOutputStream output = null; // to send objects (a car) and messages (a string)
    // Important to have the following as static, as otherwise can, unintentionally, return to null
    private static Car RacingCar1 = null; // Player 1's car
    private static Car RacingCar2 = null; // Player 2's car
    private static String racingCar1Color = ""; // Color of player 1 car
    private static String racingCar2Color = ""; // Color of player 2 car
    private boolean carsCollided = false;  // set to true when the cars collide into each other
    private boolean racingCar1Crashed = false; // true when player 1 crashes into inner/outer
    private boolean racingCar2Crashed = false; // true when player 2 crashes into inner/outer
    // checkpoints are set to true when player crosses a given point, facing in forward direction
    private boolean racingCar1CheckPoint1 = false;
    private boolean racingCar1CheckPoint2 = false;
    private boolean racingCar1CheckPoint3 = false;
    private boolean racingCar1CheckPoint4 = false;
    private boolean racingCar2CheckPoint1 = false;
    private boolean racingCar2CheckPoint2 = false;
    private boolean racingCar2CheckPoint3 = false;
    private boolean racingCar2CheckPoint4 = false;
    private static boolean hasWinner = false; //  true when a player completes required laps
    private static boolean playerDisconnected = false; // true when a player quits (and sends server a null message)
    private static boolean gameReset = false; // set to true once game has been completed and false once game started
    private static int racingCar1Laps = 0;
    private static int racingCar2Laps = 0;
    private final int LAPS = 3;   // number of laps a player needs to complete in order to win.
    private static int notifiedOfWinner = 0; // A counter that ensures that a winning message only sent once
    private static int notifiedOfLoser = 0; // A counter that ensures that a losing message only sent once
    private static String winner = "";
    private static String loser = "";

    private String carType = ""; // color of the car received, also the user's car color choice at start of game
    private boolean gameStarted = false; // set to true when both clients are active and set-up

    public TCPClientHandler(Socket server) // Constructor, initializing the server.
    {
        this.server = server;
        try {
            server.setKeepAlive(true);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    } // End of constructor

    public TCPClientHandler()
    {

    }


    public void run()
    {
        try
        {   // Initializing the input and output streams
            output = new ObjectOutputStream(
                    server.getOutputStream()
            );

            input = new ObjectInputStream(
                    server.getInputStream()
            );

            do
            {
                // If there are two active clients and they both have chosen their cars
                if(serv.getGameReady() == true && setupPlayers == 2)
                {
                    gameStarted = true;
                }

                line = receiveMessage(); // receiving message from client

                if(line != null)  // if client message contains something
                {
                    handleClientResponse(line);
                }
                else // the message contains nothing, which means a client has quit
                {
                    playerDisconnected = true;
                }

                if ( line.equals("CLOSE") )
                {
                    break;
                }

                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException e)
                {
                    System.out.println("Interupted message: " + e.getMessage());
                }

            } while(server.getKeepAlive() == true);
            // while server is active

        }
        catch (Throwable e)
        {
            System.out.println("TCPClientHandler Error: " + e.getMessage());
        }

    } // end of run()

    // Will merge a series of bytes into a string and then send the output to the Client
    private void sendMessage(String message)
    {
        try
        {
            output.writeBytes(message + "\n" );
            output.flush();
            output.reset();
        } catch (Exception e)
        {
          System.out.println(e.getMessage());
        }
    }
    // This will return a string, the message from the Client, and store in 'line'
    private String receiveMessage()
    {
        try
        {
            return input.readLine();
        } catch (Exception e)
        {
            return null;
        }
    }


 // This will send the opposing car to the Client which requested a car_update
    private void sendForeignCarUpdate()
    {
        sendMessage("foreign_car_update");

        if(carType.equals(racingCar1Color)) // if the player requesting the update is player 1
        {
            try {
                output.writeObject(RacingCar2); // send player 2 car
                output.flush();
                output.reset();
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
            }
        }

        if(carType.equals(racingCar2Color)) // if the player requesting the update is player 2
        {
            try {
                output.writeObject(RacingCar1); // send player 1 car
                output.flush();
                output.reset();
            }
            catch (Exception e)
            {
                System.out.println(e.getMessage());
            }

        }

    } // end of sendForeignCarUpdate method

// Is called when receives "car_update" from Client
    private void receiveCar()
    {
        Car inputCar = null;

        try
        {
            inputCar = (Car) input.readObject(); // Reading the player's car
        } catch (Exception e)
        {
            System.out.println(e.getMessage());
        }

        if (inputCar == null)
        {
            System.out.println("inputCar is null");
            return;
        }

        // Finding out if the received car is from player 1 or 2
        if(inputCar.getColor().equals(racingCar1Color) )
        {
            RacingCar1 = inputCar; // updating Player 1 car with its new position
        }

        if(inputCar.getColor().equals(racingCar2Color))
        {
            RacingCar2 = inputCar; // updating Player 2 car with it's new position
        }

        checkGameEvents(inputCar);

    } // end of receiveCar method

    // Called before 1st game begins, this will store player 1 & 2 color choices and send car in starting position
   private void setStartingPositions()
    {

        if(setupPlayers == 0) // Setting up player 1
        {
            RacingCar1 = new Car(370, 505, carType);
            racingCar1Color = carType;
            setupPlayers ++;
            try
            {  // sending player 1
                sendMessage("setup_game");
                output.writeObject( RacingCar1 );
                output.flush();
                output.reset();

            } catch (Exception e)
            {
              System.out.println(e.getMessage());
            }
        }

       else // Player 1 has already been set up, so set up player 2
       {
           RacingCar2 = new Car(370, 555, carType);
           racingCar2Color = carType;
          // if player 2 chose same color as player 1
           if(racingCar1Color.equals(racingCar2Color) && gameReset == false)
           {
               sendMessage("choose_again"); // prompt client again
           }
           else  // The players have not chosen same car color
               {
               setupPlayers++;
               try
               {
                   // sending player 2
                   sendMessage("setup_game");
                   output.writeObject(RacingCar2);
                   output.flush();
                   output.reset();

               } catch (Exception e) {
                   System.out.println(e.getMessage());
               }

           } // END OF ELSE ( players not choosing same color)

       } // End of else ( for player 2)


    } // End of setStartingPositons method


    public boolean checkCarsCollide()
    {
        // The cars have intersected
        carsCollided = RacingCar1.getXPosition() <= RacingCar2.getXPosition() + 25 && RacingCar1.getXPosition() + 25 >= RacingCar2.getXPosition() && RacingCar1.getYPosition() <= RacingCar2.getYPosition() + 25 && RacingCar1.getYPosition() + 25 >= RacingCar2.getYPosition();
        return carsCollided;
    }

    // this will check if a player has crossed a checkpoint
    public void checkPoint1(Car c)
    {
        // if either player has crossed a checkpoint and has been facing in forwards direction
        if((c.getXPosition() >= 685 && c.getXPosition() <= 750) && (c.getYPosition() <= 490 && c.getYPosition() >= 470) && c.getCurrentCar() > 8  )
        {
            if(c.getColor().equals(racingCar1Color)) // if that player is player 1
            {
                racingCar1CheckPoint1 = true;
            }
            else    // the player is player 2
                {
                 racingCar2CheckPoint1 = true;
                }
        }

    }
    public void checkPoint2(Car c)
    {
        // if either player has crossed a checkpoint and has been facing in forwards direction
        if((c.getXPosition() >= 685 && c.getXPosition() <= 750) && (c.getYPosition() <= 210 && c.getYPosition() >= 190) && c.getCurrentCar() > 8  )
        {
            if(c.getColor().equals(racingCar1Color)) // if that player is player 1
            {
                racingCar1CheckPoint2 = true;
            }
            else  // the player is player 2
            {
                racingCar2CheckPoint2 = true;
            }
        }

    }
    public void checkPoint3(Car c)
    {
        // if either player has crossed a checkpoint and has been facing in forwards direction
        if((c.getXPosition() >= 50 && c.getXPosition() <= 112) && (c.getYPosition() <= 175 && c.getYPosition() >= 155) && c.getCurrentCar() < 8  )
        {
            if(c.getColor().equals(racingCar1Color))  // if that player is player 1
            {
                racingCar1CheckPoint3 = true;
            }
            else  // the player is player 2
            {
                racingCar2CheckPoint3 = true;
            }
        }

    }
    public void checkPoint4(Car c)
    {
        // if either player has crossed a checkpoint and has been facing in forwards direction
        if((c.getXPosition() >= 50 && c.getXPosition() <= 112) && (c.getYPosition() <= 460 && c.getYPosition() >= 440) && c.getCurrentCar() < 8  )
        {
            if(c.getColor().equals(racingCar1Color)) // if that player is player 1
            {
                racingCar1CheckPoint4 = true;
            }
            else   // the player is player 2
            {
                racingCar2CheckPoint4 = true;
            }
        }

    }

    // Checking to see if a player has completed a lap- if they have crossed all checkpoints and finish line
    public void checkLaps()
    {
        if(racingCar1CheckPoint1 == true && racingCar1CheckPoint2 == true && racingCar1CheckPoint3 == true && racingCar1CheckPoint4 == true && RacingCar1.getXPosition() >= 384 && RacingCar1.getXPosition() <= 424 && RacingCar1.getYPosition() >= 480 && RacingCar1.getYPosition() <= 585 )
        {
            racingCar1Laps ++;  // player 1 has completed a lap
            racingCar1CheckPoint1 = false; // reset the checkpoints
            racingCar1CheckPoint2 = false;
            racingCar1CheckPoint3 = false;
            racingCar1CheckPoint4 = false;
        }
        if(racingCar2CheckPoint1 == true && racingCar2CheckPoint2 == true && racingCar2CheckPoint3 == true && racingCar2CheckPoint4 == true && RacingCar2.getXPosition() >= 384 && RacingCar2.getXPosition() <= 424 && RacingCar2.getYPosition() >= 480 && RacingCar2.getYPosition() <= 585)
        {
            racingCar2Laps ++;   // player 2 has completed a lap
            racingCar2CheckPoint1 = false;  // reset the checkpoints
            racingCar2CheckPoint2 = false;
            racingCar2CheckPoint3 = false;
            racingCar2CheckPoint4 = false;
        }

    }
   // This will check to see if either player has completed required number of laps
    public void checkWinner()
    {
        if(racingCar1Laps == LAPS)
        {
            hasWinner = true;
            winner = racingCar1Color;
            loser = racingCar2Color;
        }
        if(racingCar2Laps == LAPS)
        {
            hasWinner = true;
            winner = racingCar2Color;
            loser = racingCar1Color;
        }

    }

    // Will check for a disconnected player, collision or a winner
    private void checkGameEvents(Car c)
    {
        // Checking whether the players have passed checkpoints, completed a lap or won the game
        checkPoint1(c);
        checkPoint2(c);
        checkPoint3(c);
        checkPoint4(c);
        checkLaps();
        checkWinner();


        if(playerDisconnected == true)
        {
            sendMessage("player_disconnected"); // This will inform the other player
        }

        if(hasWinner == true && c.getColor().equals(winner) && notifiedOfWinner == 0)
        {
            // A player has won
            notifiedOfWinner ++; // ensures that if does not get executed more than once per game
            sendMessage("winning_message"); // informing the player that he/she has won

            winner = "";
            setupPlayers = 0;
            resetGame();
        }

        if(hasWinner == true && c.getColor().equals(loser) && notifiedOfLoser == 0)
        {
            // A player has lost
            notifiedOfLoser ++;
            loser = "";
            sendMessage("losing_message"); // Informing that player
            resetGame();
        }

        // this will check if the player has crashed with an edge
        c.CheckCollideInnerEdge();
        c.CheckCollideOuterEdge();
        racingCar1Crashed = RacingCar1.getHasCrashed();
        racingCar2Crashed = RacingCar2.getHasCrashed();

        // checking if the two cars have collided into each other
        if(carsCollided == false && gameStarted == true) {
            carsCollided = checkCarsCollide();
        }
        if(carsCollided == true) // if they have collided, inform both the players
        {
            sendMessage("cars_collide");
            resetGame();
        }

        if(c.getHasCrashed() == true && gameStarted == true) // Player has crashed with an edge
        {
            sendMessage("edge_crash"); // informing the player, after which will no longer be able to move
        }
        // Both players have collided with the inner/outer edges and thus neither player can move
        if(racingCar1Crashed == true && racingCar2Crashed == true && gameStarted == true)
        {
            sendMessage("both_crashed"); // informing both the players and ending the game
            resetGame();
        }

    }

    // Called when the players opt to play again, this will reset car properties and send car in it's start position
private void playAgain()
{
    RacingCar1.setSpeed(0);
    RacingCar1.setCurrentCar(0);
    RacingCar1.setYPosition(505);
    RacingCar1.setXPosition(370);
    RacingCar2.setSpeed(0);
    RacingCar2.setCurrentCar(0);
    RacingCar2.setYPosition(555);
    RacingCar2.setXPosition(370);
    RacingCar1.setHasCrashed(false);
    RacingCar2.setHasCrashed(false);

    Car inputCar = null;
    try
    {
        inputCar = (Car) input.readObject(); // the car of the player requesting to play again
    } catch (Exception e)
    {
        System.out.println(e.getMessage());
    }
if(inputCar.getColor().equals(racingCar1Color)) // it was player 1 who sent the request
{

    try
    {   // Sending player 1 car, in it's starting position
        sendMessage("setup_game");
        output.writeObject( RacingCar1 );
        output.flush();
        output.reset();
    }
    catch (Exception e)
    {
     System.out.println(e.getMessage());
    }

}
else   // It was player 2 that sent the play again request
{
    try
    {
        // sending player 2 car, in it's starting position
        sendMessage("setup_game");
        output.writeObject( RacingCar2 );
        output.flush();
        output.reset();
    }
    catch (Exception e)
    {
     System.out.println(e.getMessage());
    }

}

} // end of playAgain method

    // this will reset flags once a game has ended
   private void resetGame()
   {

       racingCar1CheckPoint1 = false;
       racingCar1CheckPoint2 = false;
       racingCar1CheckPoint3 = false;
       racingCar1CheckPoint4 = false;
       racingCar2CheckPoint1 = false;
       racingCar2CheckPoint2 = false;
       racingCar2CheckPoint3 = false;
       racingCar2CheckPoint4 = false;
       carsCollided = false;
       gameStarted = false;
       setupPlayers = 0;
       gameReset = true;

   }
// Used in Server to find out when a player has left the session
   public boolean getPlayerDisconnected()
   {
       return playerDisconnected;
   }

   // This will handle messages from the Client
    private void handleClientResponse(String response)
    {
        String[] responseParts = response.split(" ");

        switch (responseParts[0])
        {
            case "identify": // Client will send "identify" at start when choosing a car color
                carType = responseParts[1]; // the player's choice of car color
                setStartingPositions();
                break;

            case "car_update": // will be received from Client during a game
                receiveCar();
                sendForeignCarUpdate();
                break;

            case "play_again":  // Client requests to play again

                setupPlayers++;
                if(playerDisconnected == true)
                { // triggered if the winner said no to rematch but losing player said yes
                    sendMessage("player_disconnected");
                }
                else{
                    playAgain();
                }
                break;

            case "no_play_again":
                playerDisconnected = true;
                sendMessage("player_disconnected");
                break;

            // When a game not in progress, Client will repeatedly send this message, asking when can start
            case "check_game_start":

                // may occur if 1st received response says yes but 2nd says no to a rematch
                if(playerDisconnected == true)
                {
                    sendMessage("player_disconnected"); // informing the other client
                }

                if(setupPlayers == 2) // the game can start, both players are set-up
                {
                    winner = "";  // ensuring all flags are re-set before game begins
                    loser = "";
                    racingCar1Laps = 0;
                    racingCar2Laps = 0;
                    notifiedOfWinner = 0;
                    notifiedOfLoser = 0;
                    hasWinner = false;
                    gameReset = false;
                    sendMessage("start_game"); // informing the client he/she can start
                }
                else  // Both players are not set-up yet
                {
                    sendMessage("wait_to_start"); // inform the client to wait for other one to set-up
                }
                break;

        } // end of switch
    } // end of handleClientResponse method

} // End of TCPClientHandler class