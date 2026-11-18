package com.kamarajlaw.mactmedlegal.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface McopDao {
    @Transaction
    @Query("SELECT * FROM cases ORDER BY id DESC")
    fun observeAllCases(): Flow<List<FullCaseBundle>>

    @Transaction
    @Query("SELECT * FROM cases WHERE id = :caseId")
    fun observeCaseById(caseId: Long): Flow<FullCaseBundle?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCase(case: CaseEntity): Long

    @Delete
    suspend fun deleteCase(case: CaseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInjury(pin: InjuryPinEntity)

    @Delete
    suspend fun deleteInjury(pin: InjuryPinEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHeir(heir: LegalHeirEntity)

    @Delete
    suspend fun deleteHeir(heir: LegalHeirEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSceneItem(item: SceneItemEntity)

    @Query("DELETE FROM scene_items WHERE caseId = :caseId")
    suspend fun clearScene(caseId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CaseDocumentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: CaseExpenseEntity)
}
