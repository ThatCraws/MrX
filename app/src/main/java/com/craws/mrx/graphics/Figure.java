package com.craws.mrx.graphics;

import android.content.Context;
import android.graphics.BitmapFactory;

import com.craws.mrx.engine.GameView;
import com.craws.mrx.state.Player;

public class Figure extends Render {
    private final Player player;

    // For travelling between cities (and especially ease-in and out of movement)
    private static final float maxSpeed = 70;
    private float totalTravelDistance;
    private boolean travelling = false;

    public Figure(final Context context, final Player player) {
        super(BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.player_sprites), 1, 6, 0f, 0f);
        this.player = player;

        resize((int) (getWidth() * GameView.FIGURE_SCALE_FACTOR), (int) (getHeight() * GameView.FIGURE_SCALE_FACTOR));
        snapToCurrentCity();

        currFrame = player.getPort();
    }

    public Figure(final Context context, final Player player, final int width, final int height) {
        super(BitmapFactory.decodeResource(context.getResources(), com.craws.mrx.R.drawable.player_sprites), 1, 6, 0f, 0f, width, height);
        this.player = player;

        snapToCurrentCity();

        currFrame = player.getPort();
    }

    @Override
    public void update() {
        updateViewport();

        if(player.getPlace() != null) {
            // get necessary information about the city to travel to
            float placeX = player.getPlace().getCity().getX();
            float placeY = player.getPlace().getCity().getY();
            int placeWidth = player.getPlace().getCity().getWidth();
            int placeHeight = player.getPlace().getCity().getHeight();

            // calculate the x-position (figure is horizontally centered to city and vertically its bottom is placed on the city-center
            float targetX = placeX + placeWidth / 2f - getWidth() / 2f;
            float targetY = placeY + placeHeight / 2f - getHeight();

            // If we're not standing  correctly yet, move the figure
            if(getX() != targetX || getY() != targetY) {

                // The vector (in the mathematical sense) which represents the way to be travelled is: (x_target - x_player, y_target - y-player)
                // Calculating the x- and y-values of our travel-vector
                float deltaX = targetX - getX();
                float deltaY = targetY - getY();

                // The vector's magnitude/its length/the length of the way to travel (negative when moving to the left)
                float currDistance = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

                if (!travelling) {
                    travelling = true;
                    totalTravelDistance = currDistance;
                }

                float step;
                float ratioTravelled = 1 - currDistance / totalTravelDistance;

                /* To create an ease-in and out effect, we use the curve of the quadratic function x^2.
                    To let its value rise to 1 (at x = 0) and then drop again, we just use the factor -1 (turning it upside down).
                    We then raise it so the maximum turning point is 1, so we end on the function:
                        f(x) = 2x^2 * (-1) + 1
                        x | f(x)
                        -1| 0
                        0 | 1
                        1 | 0

                    The range we want to use is x = [-1, 1].
                    So with the travel ratio ranging from 0 to 1, we can just multiply by two and then subtract 1.
                 */
                float x = ratioTravelled * 2 - 1;
                float speedFactor = (x * x) * (-1) + 1;
                // + 10 so the figure does not completely stop. Subtracted those 10 from maxSpeed so we don't exceed it.
                // also, we have to make sure that step is negative when moving to the left(travelDistance is negative).
                // To do that we multiply by currDistance / |currDistance|.
                step = (speedFactor * (maxSpeed - 10) + 10) * (currDistance / Math.abs(currDistance));

                // Get the ratio of the total distance left and the distance we want to travel this frame
                float ratio = step / currDistance;

                if (Math.abs(currDistance) < Math.abs(step)) {
                    moveTo(targetX, targetY);
                    travelling = false;
                    totalTravelDistance = 0;
                } else {
                    // stretch the vector axes by the ratio
                    moveTo(getX() + deltaX * ratio,getY() + deltaY * ratio);
                }
            }
        }
    }

    public void snapToCurrentCity() {
        City city = (player.getPlace() != null) ? player.getPlace().getCity() : null;
        if(city != null) {
            moveTo(city.getX() + city.getWidth() / 2f - getWidth() / 2f, city.getY() + city.getHeight() / 2f - getHeight());
        }
    }

    public Player getPlayer() {
        return player;
    }
}
