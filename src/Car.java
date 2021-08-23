
import javax.swing.*;
import java.io.Serializable;

public class Car implements Serializable
{

    private static final long serialVersionUID = 752746024; // This needs to match that of the Client UID
    private int XPosition;
    private int YPosition;
    private int speed = 0;
    private final int TOTAL_IMAGES = 16;
    ImageIcon[] Cars;
    private int currentCar = 0;  // current image number of car
    private  boolean hasCrashed = false; // set true when crash with inner or outer areas
    private String carColor;

    public Car(int Xpos, int Ypos, String color)  // constructor
    {
        carColor = color;
        XPosition = Xpos;
        YPosition = Ypos;
        Cars = new ImageIcon[TOTAL_IMAGES]; // Initializing the Cars array

        // Loading all images of a color into Cars
        for (int i = 0; i < Cars.length; i++)
        {
            Cars[i] = new ImageIcon(getClass().getResource("res/" + carColor + i + ".png"));
        }

    }  // end of constructor

    // Will be used to compare position between car objects, if they have collided or won the race
    public int getXPosition()
    {
        return XPosition;
    }
    public int getYPosition()
    {
        return YPosition;
    }

    public int setXPosition(int x)
    {
        XPosition = x;
        return XPosition;
    }
    public int setYPosition(int y)
    {
        YPosition = y;
        return YPosition;
    }

    // Checks if received car is player 1 or 2, also if player 1 & 2 choose same car at start
    public String getColor()
    {
        return carColor;
    }

    // Used to check that the user was facing in the right direction when crossing a checkpoint
    public int getCurrentCar()
    {
        return currentCar;
    }


    // The following methods are used to reset car position & speed before starting rematch
    public int setCurrentCar(int car)
    {
        currentCar = car;
        return currentCar;
    }

    public int setSpeed(int s)
    {
        speed = s;
        return s;

    }

    public boolean getHasCrashed()
    {
        return hasCrashed;
    }

    public boolean setHasCrashed(boolean b) // Used to reset hasCrashed to true for new game
    {
        hasCrashed = b;
        return hasCrashed;
    }

    // Car collides with grass
    public void CheckCollideInnerEdge()
    {
        if( (XPosition >= 140 && XPosition <= 685) && (YPosition <= 495 && YPosition >= 165) )
        {
            hasCrashed = true;
            speed = 0;
        }
    }

    public void CheckCollideOuterEdge()
    {
        if( YPosition >= 565) // bottom outer area
        {
            hasCrashed = true;
            speed = 0;
        }
        if((XPosition >= 50 && XPosition <= 800) && YPosition <= 100 ) // top outer area
        {
            hasCrashed = true;
            speed = 0;
        }
        if(XPosition >= 750) // right outer area
        {
            hasCrashed = true;
            speed = 0;
        }
        if(XPosition <= 50) // left outer area
        {
            hasCrashed = true;
            speed = 0;
        }

    } // End of CollideOuterEdge method

} // End of Car Class