package com.craws.mrx.state;

import java.util.Vector;

/** The Timeline-class manages the timeline keeping track of the moves of Mr. X. The Tickets will be associated with the Place they were used to visit.
 *
 * The entries work as follows:
 * [Index]  | Ticket            | Place
 * [0]      | null              | StartPosition Mr. X
 * [1]      | 1st Ticket used   | Position after 1st Ticket used
 * [x]      | x-th Ticket used  | Position after x-th Ticket used
 */
public class Timeline {
    private Vector<Ticket> tickets;
    private Vector<Place> places;

    /** Constructor for creating a new empty Timeline
     */
    public Timeline() {
        tickets = new Vector<>();
        places = new Vector<>();
    }

    /** Constructor for creating a new Timeline with the given start position of Mr. X already inserted.
     * @param startPosition The position/Place Mr. X started on.
     */
    public Timeline(final Place startPosition) {
        tickets = new Vector<>();
        places = new Vector<>();

        addRound(null, startPosition);
    }

    public int size() {
        if(tickets.size() == places.size()) {
            return tickets.size();
        } else {
            return -1;
        }
    }

    public boolean isEmpty() {
        return tickets.isEmpty() && places.isEmpty();
    }

    public boolean containsPair(Ticket key, Place value) {
        return tickets.contains(key) && places.contains(value);
    }

    /** Adds a Ticket and the Place it was used to visit to the timeline
     *
     * @param key The Ticket used in the Round to be added
     * @param value The Place that was travelled to via the Ticket
     */
    public void addRound(Ticket key, Place value) {
        tickets.add(key);
        places.add(value);
    }

    /** Returns the Ticket used in the given round
     *
     * @param round The round for which the used Ticket shall be returned.
     *              If set to 0, null will be returned. If set as 1, the (first) Ticket used by Mr. X in the first round will be returned.
     * @return The Ticket that was used in the round given
     */
    public Ticket getTicketForRound(final int round) {
        if(round > places.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (ticket from a) round that has not been played yet.");
        }
        return tickets.get(round);
    }

    /** Returns the Place Mr. X ended on in the given round
     *
     * @param round The round for which the Place shall be returned.
     *              If set to 0, Mr. X's starting position will be returned. If set to 1, the (first) Place Mr. X travelled to will be returned.
     * @return The Ticket that was used in the round given
     */
    public Place getPlaceForRound(final int round) {
        if(round > places.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (place from a) round that has not been played yet.");
        }
        return places.get(round);
    }


}
