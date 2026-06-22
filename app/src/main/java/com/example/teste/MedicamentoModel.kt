package com.example.teste

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -------------------------------------------------------
// Modelo do Supabase — espelha exatamente sua tabela
// -------------------------------------------------------
@Serializable
data class MedicamentoSupabase(
    val id: Int = 0,
    val nome: String = "",
    @SerialName("concentracao")
    val concentracao: String = "",
    val forma: String = ""
)

// -------------------------------------------------------
// Um horário individual de um medicamento prescrito
// Ex: Amoxicilina 08:00 Manhã, Amoxicilina 20:00 Noite
// -------------------------------------------------------
@Entity(tableName = "horarios_prescritos")
data class HorarioPrescrito(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicamentoId: Int,     // referência ao MedicamentoPrescrito
    val horario: String,        // "08:00"
    val turno: String,           // "Manhã", "Tarde", "Noite"
    val qtde: Int               // qtde que deve ser administrada naquele horário
)

// -------------------------------------------------------
// Medicamento prescrito — agora com dias de tratamento
// Os horários ficam em HorarioPrescrito (relação 1-N)
// -------------------------------------------------------
@Entity(tableName = "medicamentos_prescritos")
data class MedicamentoPrescrito(
    @PrimaryKey val id: Int,
    val nome: String,
    val concentracao: String,
    val forma: String,
    val diasTratamento: Int,        // quantos dias o paciente vai tomar
    val dataInicio: Long,            // timestamp do dia seguinte à prescrição (meia-noite)
    val imgRemedio: String
)

// -------------------------------------------------------
// Registro de dose tomada — persiste o "Já tomei"
// -------------------------------------------------------
@Entity(tableName = "doses_tomadas", primaryKeys = ["horarioId", "dataTimestamp"])
data class DoseTomada(
    val horarioId: Int,         // qual HorarioPrescrito foi tomado
    val dataTimestamp: Long     // meia-noite do dia em que foi tomado
)

// -------------------------------------------------------
// Estado de UI de um horário — usado na tela do médico
// -------------------------------------------------------
data class HorarioUiState(
    val horario: String = "08:00",
    val turno: String = "Manhã",
    val qtde: Int = 4
)

// -------------------------------------------------------
// Estado de UI de um medicamento na tela do médico
// -------------------------------------------------------
data class MedicamentoUiState(
    val medicamento: MedicamentoSupabase,
    val selecionado: Boolean = false,
    val horarios: List<HorarioUiState> = listOf(HorarioUiState()),
    val diasTratamento: Int = 7,
    val qtde: Int = 4,
    val imgRemedio: String = TipoRemedio.BRANCO_REDONDO.id
)

// -------------------------------------------------------
// Dados completos para exibir na tela do paciente
// -------------------------------------------------------
data class MedicamentoComHorarios(
    val prescrito: MedicamentoPrescrito,
    val horarios: List<HorarioPrescrito>
)

// -------------------------------------------------------
// Estado de um horário na tela do paciente
// Indica qual horário mostrar agora (o próximo pendente)
// -------------------------------------------------------
data class HorarioAtivo(
    val horarioPrescrito: HorarioPrescrito,
    val jaToмado: Boolean,
    val dentroDoTratamento: Boolean  // false se já passou os dias
)