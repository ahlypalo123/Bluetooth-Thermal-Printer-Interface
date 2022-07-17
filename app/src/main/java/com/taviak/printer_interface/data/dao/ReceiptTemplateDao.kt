package com.taviak.printer_interface.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.taviak.printer_interface.data.model.ListData
import com.taviak.printer_interface.data.model.ReceiptTemplate
import com.taviak.printer_interface.data.model.TemplateWithVariables
import com.taviak.printer_interface.data.model.VariableTemplateCrossRef

@Dao
interface ReceiptTemplateDao {

    @Query("SELECT * FROM receipttemplate")
    suspend fun getAll() : List<ReceiptTemplate>

    @Query("SELECT * FROM receipttemplate")
    fun getAllLiveData() : LiveData<List<ReceiptTemplate>>

    @Transaction
    @Query("SELECT * FROM receipttemplate WHERE active = 1")
    suspend fun getActiveWithVariables(): TemplateWithVariables?

    @Insert
    suspend fun insert(data: ReceiptTemplate)

    @Insert
    suspend fun insertRelation(data: VariableTemplateCrossRef)

    @Insert
    suspend fun insertRelationInBulk(data: List<VariableTemplateCrossRef>)

    @Update
    suspend fun save(data: ReceiptTemplate)

    @Query("SELECT * FROM receipttemplate WHERE active = 1")
    suspend fun getActive() : ReceiptTemplate?

    @Delete
    suspend fun delete(template: ReceiptTemplate)

    @Query("SELECT * FROM receipttemplate WHERE active = 1")
    fun getActiveLiveData() : LiveData<ReceiptTemplate>

}