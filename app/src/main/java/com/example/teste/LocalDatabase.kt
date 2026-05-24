package com.example.teste

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// -------------------------------------------------------
// DAO — Medicamentos Prescritos
// -------------------------------------------------------
@Dao
interface MedicamentoDao {

    @Query("SELECT * FROM medicamentos_prescritos")
    fun observarTodos(): Flow<List<MedicamentoPrescrito>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(medicamento: MedicamentoPrescrito)

    @Query("DELETE FROM medicamentos_prescritos")
    suspend fun limparTodos()
}

// -------------------------------------------------------
// DAO — Horários Prescritos
// -------------------------------------------------------
@Dao
interface HorarioDao {

    @Query("SELECT * FROM horarios_prescritos WHERE medicamentoId = :medId")
    fun observarPorMedicamento(medId: Int): Flow<List<HorarioPrescrito>>

    @Query("SELECT * FROM horarios_prescritos")
    suspend fun buscarTodos(): List<HorarioPrescrito>

    @Query("SELECT * FROM horarios_prescritos WHERE medicamentoId IN (:ids)")
    suspend fun buscarPorMedicamentos(ids: List<Int>): List<HorarioPrescrito>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(horario: HorarioPrescrito)

    @Query("DELETE FROM horarios_prescritos")
    suspend fun limparTodos()

    @Query("DELETE FROM horarios_prescritos WHERE medicamentoId = :medId")
    suspend fun limparPorMedicamento(medId: Int)
}

// -------------------------------------------------------
// DAO — Doses Tomadas
// -------------------------------------------------------
@Dao
interface DoseDao {

    /** Flow reativo — Room re-emite sempre que a tabela muda no dia informado. */
    @Query("SELECT * FROM doses_tomadas WHERE dataTimestamp = :meianoite")
    fun observarPorDia(meianoite: Long): Flow<List<DoseTomada>>

    @Query("""
        SELECT * FROM doses_tomadas 
        WHERE horarioId = :horarioId AND dataTimestamp = :dataTimestamp
    """)
    suspend fun buscar(horarioId: Int, dataTimestamp: Long): DoseTomada?

    @Query("SELECT * FROM doses_tomadas WHERE dataTimestamp = :dataTimestamp")
    suspend fun buscarPorDia(dataTimestamp: Long): List<DoseTomada>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserir(dose: DoseTomada)

    @Delete
    suspend fun remover(dose: DoseTomada)

    @Query("DELETE FROM doses_tomadas")
    suspend fun limparTodos()
}

// -------------------------------------------------------
// Database
// -------------------------------------------------------
@Database(
    entities = [MedicamentoPrescrito::class, HorarioPrescrito::class, DoseTomada::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicamentoDao(): MedicamentoDao
    abstract fun horarioDao(): HorarioDao
    abstract fun doseDao(): DoseDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "remedios_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

// -------------------------------------------------------
// DataStore — preferências persistidas entre sessões
// -------------------------------------------------------
val Context.dataStore by preferencesDataStore(name = "app_prefs")

object AppPrefsKeys {
    val STREAK        = intPreferencesKey("streak_dias")
    val RECORD        = intPreferencesKey("record_streak")
    val TOMADO_HOJE   = booleanPreferencesKey("tomado_hoje")
    val ULTIMA_DATA   = stringPreferencesKey("ultima_data")
    val DATA_CONSULTA = stringPreferencesKey("consulta_agendada")
}