package com.uninorte.edu.co.tracku.database.entities;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
@Entity
public class Historical {
    @PrimaryKey(autoGenerate = true)
    public int locationId;

    @ColumnInfo(name="userID")
    public String userID;

    @ColumnInfo(name="latitude")
    public String latitude;

    @ColumnInfo(name="longitude")
    public String longitude;

    @ColumnInfo(name="date")
    public String date;
}
