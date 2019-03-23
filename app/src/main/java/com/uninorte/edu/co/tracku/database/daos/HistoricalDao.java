package com.uninorte.edu.co.tracku.database.daos;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import com.uninorte.edu.co.tracku.database.entities.Historical;


import java.util.List;
@Dao
public interface HistoricalDao {
    @Query("select * from historical")
    List<Historical> getAllHistoricals();


    @Query("select * from historical where userID=:id")
    List<Historical> getHistoricalById(int id);

    @Insert
    void insertHistorical(Historical historical);

    @Delete
    void deleteHistorical(Historical historical);

}
