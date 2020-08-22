package com.craws.mrx.engine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import com.craws.mrx.graphics.City;
import com.craws.mrx.graphics.Figure;
import com.craws.mrx.graphics.Render;
import com.craws.mrx.state.Ability;
import com.craws.mrx.state.GameState;
import com.craws.mrx.state.Place;
import com.craws.mrx.state.Ticket;
import com.craws.mrx.state.Vehicle;

import java.util.Queue;
import java.util.Stack;

public class GameView extends SurfaceView implements Runnable {

    // game-engine
    volatile boolean running;
    private Thread gameThread;

    // render
    private SurfaceHolder surfaceHolder;
    private Canvas canvas;
    private Paint paint;
    private Stack<Render> renderStack;

    // Drawable Objects
    private Figure playerOne;
    private City homeCity;
    private City secondCity;

    // Temp
    private Ticket medTicket;
    private Ticket medTicketTwo;

    public GameView(Context context) {
        super(context);

        startGame(context);
    }

    public GameView(Context context, AttributeSet attrs) {
        super(context, attrs);

        startGame(context);
    }

    private void startGame(Context context) {
        renderStack = new Stack<>();

        GameState state = new GameState();

        /*homeCity = new City(context, "Brem", 200, 250);
        secondCity = new City(context, "Kaffhausen", 1370, 500);

        homeCity.getPlace().connectTo(secondCity.getPlace(), Vehicle.MEDIUM);

        playerOne = new Figure(context, 0, "Earl", homeCity.getPlace());

        medTicket = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);
        medTicketTwo = new Ticket(Vehicle.MEDIUM, Ability.SPECIAL);

        playerOne.getPlayer().giveTicket(medTicket);
        playerOne.getPlayer().giveTicket(medTicketTwo);

        renderStack.add(homeCity);
        renderStack.add(secondCity);
        renderStack.add(playerOne);
        */
        surfaceHolder = getHolder();
        paint = new Paint();
    }

    @Override
    public void run() {
        while(running) {
            // update the game state
            update();

            // draw the frame with updated state
            draw();

            // control the frame pacing
            control();
        }
    }

    private void update() {
        for(Render toUpdate: renderStack) {
            toUpdate.update();
        }
    }

    private void draw() {
        if(surfaceHolder.getSurface().isValid()) {
            canvas = surfaceHolder.lockCanvas();

            canvas.drawColor(Color.WHITE);

            for(Render toRender: renderStack) {
                canvas.drawBitmap(toRender.getBitmap(), toRender.getX(), toRender.getY(), paint);
                //if(toRender.getClass().getName().equals("com.craws.mrx.graphics.City")) {

                //}
            }

            // TODO: Implement line drawing between cities

            paint.setTextSize(72);
            canvas.drawText(canvas.getWidth() + "x" + canvas.getHeight(), 20, 650, paint);

            surfaceHolder.unlockCanvasAndPost(canvas);
        }

    }

    private void control() {
        try {
            Thread.sleep(16, 666666); // game is supposed to run at 60fps TODO: Achieve proper frame pacing!
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        running = false;

        try {
            gameThread.join();
        } catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        running = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    /*public void switcheroo() {
        Ticket medTicketLeft = null;

        for(Ticket currTicket: playerOne.getPlayer().getInventory()) {
            if(currTicket.getVehicle() == Vehicle.MEDIUM) {
                medTicketLeft = currTicket;
                break;
            }
        }

        if(playerOne.getPlayer().getCurrPlace().equals(homeCity.getPlace())) {
            playerOne.getPlayer().doTurn(secondCity.getPlace(), medTicketLeft);
        } else {
            playerOne.getPlayer().doTurn(homeCity.getPlace(), medTicketLeft);
        }
    } */
}
