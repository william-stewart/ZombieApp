package com.google.android.gms.location.sample.locationupdates;

import android.location.Location;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Created by admin on 5/3/2016.
 */
public class LocationHistoryManager {
    private Queue<Location> locations;
    private int maxSize;

    public LocationHistoryManager(int maxNumOfRecords) {
        this.maxSize = maxNumOfRecords;
        locations = new LinkedList<Location>();
    }

    private boolean maxSizeReached() {
        return locations.size() == maxSize;
    }

    private void dequeue() {
        locations.poll();
    }

    public void add(Location location) {
        if(maxSizeReached()){
            dequeue();
        }
        locations.add(location);
    }

    public Collection<Location> getAll() {
        return locations;
    }

    public int getSize() {
        return locations.size();
    }

}
