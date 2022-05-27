package com.taviak.printer_interface.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.taviak.printer_interface.data.model.Variable

@Dao
interface VariableDao {

    @Query("SELECT * FROM variable")
    suspend fun getAll() : List<Variable>

    @Query("SELECT * FROM variable")
    fun getAllLiveData() : LiveData<List<Variable>>

    @Query("SELECT * FROM variable WHERE shortName = :name")
    suspend fun findByShortName(name: String) : Variable?

    @Insert
    suspend fun insert(data: Variable)

    @Update
    suspend fun save(data: Variable)

    @Delete
    suspend fun delete(data: Variable)

}