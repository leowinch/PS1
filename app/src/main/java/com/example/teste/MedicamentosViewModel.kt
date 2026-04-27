package com.example.teste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MedicamentosViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = AppDatabase.getInstance(app).medicamentoDao()

    // Remédios prescritos salvos localmente (tela do paciente)
    val medicamentosPrescritos: StateFlow<List<MedicamentoPrescrito>> =
        dao.observarTodos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Estado da tela do médico
    private val _medicamentosSupabase = MutableStateFlow<List<MedicamentoUiState>>(emptyList())
    val medicamentosSupabase: StateFlow<List<MedicamentoUiState>> = _medicamentosSupabase

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    private val _salvando = MutableStateFlow(false)
    val salvando: StateFlow<Boolean> = _salvando

    // -------------------------------------------------------
    // Busca medicamentos do Supabase
    // Ajuste "medicamentos" para o nome real da sua tabela
    // -------------------------------------------------------
    fun carregarMedicamentosSupabase() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            try {
                val resultado = SupabaseClient.client
                    .postgrest["Medicamentos"]
                    .select()
                    .decodeList<MedicamentoSupabase>()

                // Marca como selecionados os que já estão prescritos,
                // e restaura o horário/turno que o médico tinha definido antes
                val prescritosMap = dao.observarTodos().first().associateBy { it.id }

                _medicamentosSupabase.value = resultado.map { med ->
                    val prescrito = prescritosMap[med.id]
                    MedicamentoUiState(
                        medicamento = med,
                        selecionado = prescrito != null,
                        horario = prescrito?.horario ?: "08:00",
                        turno = prescrito?.turno ?: "Manhã"
                    )
                }
            } catch (e: Exception) {
                _erro.value = "Erro ao carregar: ${e.message}"
            } finally {
                _carregando.value = false
            }
        }
    }

    // Alterna seleção de um medicamento
    fun alternarSelecao(id: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map { if (it.medicamento.id == id) it.copy(selecionado = !it.selecionado) else it }
        }
    }

    // Atualiza o horário de um medicamento específico
    fun atualizarHorario(id: Int, horario: String) {
        _medicamentosSupabase.update { lista ->
            lista.map { if (it.medicamento.id == id) it.copy(horario = horario) else it }
        }
    }

    // Atualiza o turno de um medicamento específico
    fun atualizarTurno(id: Int, turno: String) {
        _medicamentosSupabase.update { lista ->
            lista.map { if (it.medicamento.id == id) it.copy(turno = turno) else it }
        }
    }

    // Salva a prescrição localmente e volta para a tela do paciente
    fun salvarPrescricao(onConcluido: () -> Unit) {
        viewModelScope.launch {
            _salvando.value = true
            try {
                val selecionados = _medicamentosSupabase.value
                    .filter { it.selecionado }
                    .map { ui ->
                        MedicamentoPrescrito(
                            id = ui.medicamento.id,
                            nome = ui.medicamento.nome,
                            concentracao = ui.medicamento.concentracao,
                            forma = ui.medicamento.forma,
                            horario = ui.horario,
                            turno = ui.turno
                        )
                    }
                dao.substituirTodos(selecionados)
                onConcluido()
            } finally {
                _salvando.value = false
            }
        }
    }
}