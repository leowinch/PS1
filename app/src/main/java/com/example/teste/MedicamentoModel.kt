package com.example.teste

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// -------------------------------------------------------
// Modelo do Supabase — espelha exatamente sua tabela
// Colunas reais: nome, concentração, forma
// -------------------------------------------------------
@Serializable
data class MedicamentoSupabase(
    val id: Int = 0,
    val nome: String = "",

    @SerialName("concentração")   // ajuste para o nome exato da coluna no Supabase
    val concentracao: String = "",

    val forma: String = ""        // "Comprimido", "Cápsula", "Solução oral", etc.
)

// -------------------------------------------------------
// Entidade Room — salva localmente a prescrição do médico
// Horário e turno são definidos pelo médico no app,
// não vêm do banco de dados
// -------------------------------------------------------
@Entity(tableName = "medicamentos_prescritos")
data class MedicamentoPrescrito(
    @PrimaryKey val id: Int,
    val nome: String,
    val concentracao: String,
    val forma: String,
    val horario: String,   // definido pelo médico: ex: "08:00"
    val turno: String      // definido pelo médico: "Manhã", "Tarde" ou "Noite"
)

// -------------------------------------------------------
// Estado de UI — usado na tela do médico
// Combina o dado do Supabase + horário/turno escolhidos
// -------------------------------------------------------
data class MedicamentoUiState(
    val medicamento: MedicamentoSupabase,
    val selecionado: Boolean = false,
    val horario: String = "08:00",   // valor padrão editável pelo médico
    val turno: String = "Manhã"      // valor padrão editável pelo médico
)