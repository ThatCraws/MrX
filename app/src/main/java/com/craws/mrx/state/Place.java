package com.craws.mrx.state;

import com.craws.mrx.graphics.City;

/**
 * Representing a place players can visit, meaning they end and start their turn on a place and can move from one to another.
 * Places have names that will be displayed to the user and are connected by routes.
 *
 * @author Julien
 *
 */
public class Place {
    /** The graphical representation of this Place. */
    private City graphic;
    /** The name of the place to be shown to the player */
    private String name;

    public Place(City graphic, String name) {
        this.graphic = graphic;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public City getGraphic() {
        return graphic;
    }
}
