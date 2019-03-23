package com.uninorte.edu.co.tracku.database.core;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

import com.uninorte.edu.co.tracku.database.daos.HistoricalDao;
import com.uninorte.edu.co.tracku.database.daos.UserDao;
import com.uninorte.edu.co.tracku.database.entities.Historical;
import com.uninorte.edu.co.tracku.database.entities.User;

@Database(entities = {User.class, Historical.class},version = 2, exportSchema = false)
public abstract class TrackUDatabaseManager extends RoomDatabase {
    public abstract UserDao userDao();
    public abstract HistoricalDao historicalDao();
}