package com.craws.mrx.state;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
    private Vector<Item> timeline;

    /** Constructor for creating a new empty Timeline
     */
    public Timeline() {
        timeline = new Vector<>();
    }

    /** Constructor for creating a new Timeline with the given start position of Mr. X already inserted.
     * @param startPosition The position/Place Mr. X started on.
     */
    public Timeline(final Place startPosition) {
        timeline = new Vector<>();

        addRound(null, startPosition);
    }

    public int size() {
        return timeline.size();
    }

    public boolean isEmpty() {
        return timeline.isEmpty();
    }

    public boolean containsPair(@Nullable Ticket key, @NonNull Place value) {
        for(int i = 0; i < timeline.size(); i++) {
            Item currItem = timeline.get(i);
            if(currItem.getTicket() == key && currItem.getPlace() == value) {
                return true;
            }
        }

        return false;
    }

    /** Adds a Ticket and the Place it was used to visit to the timeline
     *
     * @param key The Ticket used in the Round to be added
     * @param value The Place that was travelled to via the Ticket
     */
    public void addRound(@Nullable Ticket key, @NonNull Place value) {
        timeline.add(new Item(key, value));
    }

    /** Returns the Ticket used in the given round
     *
     * @param round The round for which the used Ticket shall be returned.
     *              If set to 0, null will be returned. If set as 1, the (first) Ticket used by Mr. X in the first round will be returned.
     * @return The Ticket that was used in the round given
     */
    public @Nullable Ticket getTicketForRound(final int round) {
        if(round > timeline.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (ticket from a) round that has not been played yet.");
        }
        return timeline.get(round).getTicket();
    }

    /** Returns the Place Mr. X ended on in the given round
     *
     * @param round The round for which the Place shall be returned.
     *              If set to 0, Mr. X's starting position will be returned. If set to 1, the (first) Place Mr. X travelled to will be returned.
     * @return The Ticket that was used in the round given
     */
    public @NonNull Place getPlaceForRound(final int round) {
        if(round > timeline.size()) {
            throw new IndexOutOfBoundsException("Asked timeline for a (place from a) round that has not been played yet.");
        }
        return timeline.get(round).getPlace();
    }

    public void mark(final int round) {
        if(round < timeline.size()) {
            timeline.get(round).mark();
        } else {
            throw new IndexOutOfBoundsException("Tried to mark round that has not happened yet.");
        }
    }

    private static class Item {
        private Ticket ticket;
        private Place place;
        private boolean marked;

        public Item(final Ticket ticket, final Place place) {
            this.ticket = ticket;
            this.place = place;
            this.marked = false;
        }

        public void mark() {
            marked = true;
        }

        public Ticket getTicket() {
            return ticket;
        }

        public Place getPlace() {
            return place;
        }

        public boolean isMarked() {
            return marked;
        }
    }

}
