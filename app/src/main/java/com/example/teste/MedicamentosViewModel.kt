package com.example.teste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar
import java.time.LocalDate

// -------------------------------------------------------
// Helpers de data
// -------------------------------------------------------

// Retorna meia-noite do dia informado (zera hora/min/seg/ms)
fun meianoite(cal: Calendar): Long {
    return Calendar.getInstance().apply {
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

// Meia-noite de hoje
fun hoje(): Long {
    return meianoite(Calendar.getInstance())
}

// Meia-noite de amanhã (primeiro dia do tratamento)
fun amanha(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return meianoite(cal)
}

// Retorna lista de timestamps (meia-noite) para cada dia do tratamento
fun diasDeTratamento(dataInicio: Long, diasTratamento: Int): List<Long> {
    val lista = mutableListOf<Long>()
    val cal = Calendar.getInstance()
    cal.timeInMillis = dataInicio
    repeat(diasTratamento) {
        lista.add(meianoite(cal))
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }
    return lista
}

// -------------------------------------------------------
// ViewModel
// -------------------------------------------------------
class MedicamentosViewModel(app: Application) : AndroidViewModel(app) {

    private val medDao = AppDatabase.getInstance(app).medicamentoDao()
    private val horarioDao = AppDatabase.getInstance(app).horarioDao()
    private val doseDao = AppDatabase.getInstance(app).doseDao()



    private val _consultaAgendada = MutableStateFlow<LocalDate?>(null)
    val consultaAgendada: StateFlow<LocalDate?> = _consultaAgendada.asStateFlow()

    fun agendarConsulta(data: LocalDate) {
        _consultaAgendada.value = data
    }
    // ---------------------------------------------------
    // Estado da tela do paciente
    // ---------------------------------------------------
    val medicamentosPrescritos: StateFlow<List<MedicamentoPrescrito>> =
        medDao.observarTodos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Horários de todos os medicamentos prescritos
    private val _horariosPrescritos = MutableStateFlow<List<HorarioPrescrito>>(emptyList())
    val horariosPrescritos: StateFlow<List<HorarioPrescrito>> = _horariosPrescritos

    // Doses já tomadas hoje
    private val _dosesTomadas = MutableStateFlow<Set<Int>>(emptySet()) // ids de HorarioPrescrito tomados hoje
    val dosesTomadas: StateFlow<Set<Int>> = _dosesTomadas

    init {
        // Carrega horários sempre que a lista de medicamentos mudar
        viewModelScope.launch {
            medicamentosPrescritos.collect { lista ->
                if (lista.isNotEmpty()) {
                    val ids = lista.map { it.id }
                    _horariosPrescritos.value = horarioDao.buscarPorMedicamentos(ids)
                } else {
                    _horariosPrescritos.value = emptyList()
                }
            }
        }
        // Carrega doses tomadas hoje
        carregarDosesHoje()
    }

    fun carregarDosesHoje() {
        viewModelScope.launch {
            val doses = doseDao.buscarPorDia(hoje())
            _dosesTomadas.value = doses.map { it.horarioId }.toSet()
        }
    }

    // Marca ou desmarca uma dose como tomada
    fun toggleDose(horarioId: Int) {
        viewModelScope.launch {
            val hoje = hoje()
            val existente = doseDao.buscar(horarioId, hoje)
            if (existente != null) {
                doseDao.remover(existente)
                _dosesTomadas.update { it - horarioId }
            } else {
                doseDao.inserir(DoseTomada(horarioId, hoje))
                _dosesTomadas.update { it + horarioId }
            }
        }
    }

    // Retorna o próximo horário ativo para um medicamento
    // (o primeiro não tomado hoje, em ordem de horário)
    fun proximoHorarioAtivo(
        medicamento: MedicamentoPrescrito,
        horarios: List<HorarioPrescrito>,
        dosesTomadas: Set<Int>
    ): HorarioPrescrito? {
        val agora = Calendar.getInstance()
        val hojeTs = hoje()

        // Verifica se ainda está dentro do período de tratamento
        val diasPassados = ((hojeTs - medicamento.dataInicio) / (1000 * 60 * 60 * 24)).toInt()
        if (diasPassados < 0 || diasPassados >= medicamento.diasTratamento) return null

        // Ordena horários pelo horário do dia
        val ordenados = horarios
            .filter { it.medicamentoId == medicamento.id }
            .sortedBy { it.horario }

        // Retorna o primeiro não tomado hoje
        return ordenados.firstOrNull { it.id !in dosesTomadas }
    }

    // ---------------------------------------------------
    // Estado da tela do médico
    // ---------------------------------------------------
    private val _medicamentosSupabase = MutableStateFlow<List<MedicamentoUiState>>(emptyList())
    val medicamentosSupabase: StateFlow<List<MedicamentoUiState>> = _medicamentosSupabase

    private val _carregando = MutableStateFlow(false)
    val carregando: StateFlow<Boolean> = _carregando

    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro

    private val _salvando = MutableStateFlow(false)
    val salvando: StateFlow<Boolean> = _salvando

    fun carregarMedicamentosSupabase() {
        viewModelScope.launch {
            _carregando.value = true
            _erro.value = null
            try {
                val resultado = SupabaseClient.client
                    .postgrest["Medicamentos"]   // <- nome exato da sua tabela
                    .select()
                    .decodeList<MedicamentoSupabase>()

                // Restaura seleção prévia se existir
                val prescritosMap = medDao.observarTodos().first().associateBy { it.id }
                val todosHorarios = horarioDao.buscarTodos()

                _medicamentosSupabase.value = resultado.map { med ->
                    val prescrito = prescritosMap[med.id]
                    val horariosAtuais = todosHorarios
                        .filter { it.medicamentoId == med.id }
                        .map { HorarioUiState(it.horario, it.turno, it.qtde) }

                    MedicamentoUiState(
                        medicamento = med,
                        selecionado = prescrito != null,
                        horarios = if (horariosAtuais.isNotEmpty()) horariosAtuais
                        else listOf(HorarioUiState()),
                        diasTratamento = prescrito?.diasTratamento ?: 7
                    )
                }
            } catch (e: Exception) {
                _erro.value = "Erro ao carregar: ${e.message}"
            } finally {
                _carregando.value = false
            }
        }
    }

    fun alternarSelecao(id: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map { if (it.medicamento.id == id) it.copy(selecionado = !it.selecionado) else it }
        }
    }

    fun adicionarHorario(id: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id)
                    it.copy(horarios = it.horarios + HorarioUiState())
                else it
            }
        }
    }

    fun removerHorario(id: Int, index: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id && it.horarios.size > 1)
                    it.copy(horarios = it.horarios.toMutableList().also { h -> h.removeAt(index) })
                else it
            }
        }
    }

    fun atualizarHorario(id: Int, index: Int, horario: String) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id) {
                    val novos = it.horarios.toMutableList()
                    novos[index] = novos[index].copy(horario = horario)
                    it.copy(horarios = novos)
                } else it
            }
        }
    }

    fun atualizarQtdeHorario(id: Int, index: Int, qtde: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id) {
                    val novos = it.horarios.toMutableList()
                    novos[index] = novos[index].copy(qtde = qtde)
                    it.copy(horarios = novos)
                } else it
            }
        }
    }

    fun atualizarTurno(id: Int, index: Int, turno: String) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id) {
                    val novos = it.horarios.toMutableList()
                    novos[index] = novos[index].copy(turno = turno)
                    it.copy(horarios = novos)
                } else it
            }
        }
    }

    fun atualizarDias(id: Int, dias: Int) {
        _medicamentosSupabase.update { lista ->
            lista.map {
                if (it.medicamento.id == id) it.copy(diasTratamento = dias) else it
            }
        }
    }

    // Salva prescrição — primeiro dia é amanhã
    fun salvarPrescricao(onConcluido: () -> Unit) {
        viewModelScope.launch {
            _salvando.value = true
            try {
                val selecionados = _medicamentosSupabase.value.filter { it.selecionado }

                // Limpa tudo e reconstrói
                medDao.limparTodos()
                horarioDao.limparTodos()
                doseDao.limparTodos()

                val inicioTratamento = amanha()

                selecionados.forEach { ui ->
                    // Salva o medicamento
                    medDao.inserir(
                        MedicamentoPrescrito(
                            id = ui.medicamento.id,
                            nome = ui.medicamento.nome,
                            concentracao = ui.medicamento.concentracao,
                            forma = ui.medicamento.forma,
                            diasTratamento = ui.diasTratamento,
                            dataInicio = inicioTratamento
                        )
                    )
                    // Salva cada horário
                    ui.horarios.forEach { h ->
                        horarioDao.inserir(
                            HorarioPrescrito(
                                medicamentoId = ui.medicamento.id,
                                horario = h.horario,
                                turno = h.turno,
                                qtde = h.qtde
                            )
                        )
                    }
                }

                // Recarrega horários na tela do paciente
                val ids = selecionados.map { it.medicamento.id }
                _horariosPrescritos.value = horarioDao.buscarPorMedicamentos(ids)
                _dosesTomadas.value = emptySet()

                onConcluido()
            } finally {
                _salvando.value = false
            }
        }
    }
}