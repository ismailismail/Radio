package de.winterrettich.ninaradio.event;

import de.winterrettich.ninaradio.model.Station;

public class SelectStationEvent {
    public final Station station;

    public SelectStationEvent(Station station) {
        this.station = station;
    }
}
