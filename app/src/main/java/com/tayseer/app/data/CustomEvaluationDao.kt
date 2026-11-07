package com.tayseer.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomEvaluationDao {
    @Query("SELECT * FROM custom_evaluations")
    fun getAllCustomEvaluations(): Flow<List<CustomEvaluation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomEvaluation(evaluation: CustomEvaluation)

    @Delete
    suspend fun deleteCustomEvaluation(evaluation: CustomEvaluation)
}
