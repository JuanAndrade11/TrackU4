package com.uninorte.edu.co.tracku.com.uninorte.edu.co.tracku.gps;

public interface GPSManagerInterface {
    void LocationReceived(double latitude, double longitude);
    void GPSManagerException(Exception error);
}
