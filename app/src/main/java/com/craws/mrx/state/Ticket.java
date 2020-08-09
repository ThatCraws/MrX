package com.craws.mrx.state;

/**
 * Tickets are collected by the players and can be spent for travelling between places or activating abilities.
 *
 * @author Julien
 *
 */
public class Ticket {
    /** The vehicle which can be used with this ticket */
    private Vehicle vehicle;
    /** The ability the ticket can be put towards to */
    private Ability ability;

    public Ticket(Vehicle vehicle, Ability ability) {
        this.vehicle = vehicle;
        this.ability = ability;
    }

    public Vehicle getVehicle() {
        return  vehicle;
    }

    public  Ability getAbility() {
        return ability;
    }
}
