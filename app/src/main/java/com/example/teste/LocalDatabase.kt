package com.example.teste

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// -------------------------------------------------------
// DAO — operações no banco local
// -------------------------------------------------------
@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos_prescritos ORDER BY turno, horario")
    fun observarTodos(): Flow<List<MedicamentoPrescrito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(medicamento: MedicamentoPrescrito)

    @Query("DELETE FROM medicamentos_prescritos")
    suspend fun limparTodos()

    @Transaction
    suspend fun substituirTodos(lista: List<MedicamentoPrescrito>) {
        limparTodos()
        lista.forEach { inserir(it) }
    }
}

// -------------------------------------------------------
// Database Room
// -------------------------------------------------------
@Database(entities = [MedicamentoPrescrito::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicamentoDao(): MedicamentoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remedios_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
