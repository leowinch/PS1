package com.example.teste

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalTime
import java.time.LocalDate
import java.util.Calendar
import java.time.temporal.ChronoUnit

// ─────────────────────────────────────────────────────────────────────────────
// UTILITÁRIOS DE DATA
// ─────────────────────────────────────────────────────────────────────────────

/** Retorna o timestamp de meia-noite (00:00:00.000) do Calendar informado. */
fun meianoite(cal: Calendar): Long {
    return Calendar.getInstance().apply {
        set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

/** Timestamp de meia-noite de hoje. Usado como chave das DoseTomada. */
fun hoje(): Long = meianoite(Calendar.getInstance())

/** Timestamp de meia-noite de amanhã. Usado como dataInicio da prescrição. */
fun amanha(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, 1)
    return meianoite(cal)
}

/** Quantos ms faltam até a próxima meia-noite. Usado para agendar o reset diário. */
fun msAteMeianoite(): Long {
    val agora = System.currentTimeMillis()
    val amanha = amanha()
    return amanha - agora
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

class MedicamentosViewModel(app: Application) : AndroidViewModel(app) {

    // DAOs do Room
    private val medDao     = AppDatabase.getInstance(app).medicamentoDao()
    private val horarioDao = AppDatabase.getInstance(app).horarioDao()
    private val doseDao    = AppDatabase.getInstance(app).doseDao()

    // DataStore (persistência leve — streak, record, consulta)
    private val dataStore  = app.dataStore

    // ── OFENSIVA — lidos do DataStore ──────────────────────────────────────

    val streakDias: StateFlow<Int> = dataStore.data
        .map { it[AppPrefsKeys.STREAK] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recordStreak: StateFlow<Int> = dataStore.data
        .map { it[AppPrefsKeys.RECORD] ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val tomadoHoje: StateFlow<Boolean> = dataStore.data
        .map { it[AppPrefsKeys.TOMADO_HOJE] ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // ── CONSULTA AGENDADA — lida do DataStore ──────────────────────────────

    val consultaAgendada: StateFlow<LocalDate?> = dataStore.data
        .map { prefs ->
            prefs[AppPrefsKeys.DATA_CONSULTA]
                ?.takeIf { it.isNotEmpty() }
                ?.let { LocalDate.parse(it) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun agendarConsulta(data: LocalDate) {
        viewModelScope.launch {
            dataStore.edit { prefs ->
                prefs[AppPrefsKeys.DATA_CONSULTA] = data.toString()
            }
        }
    }

    // ── MEDICAMENTOS E HORÁRIOS (Room) ─────────────────────────────────────

    val medicamentosPrescritos: StateFlow<List<MedicamentoPrescrito>> =
        medDao.observarTodos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _horariosPrescritos = MutableStateFlow<List<HorarioPrescrito>>(emptyList())
    val horariosPrescritos: StateFlow<List<HorarioPrescrito>> = _horariosPrescritos

    // ── DOSES TOMADAS HOJE (Room) ──────────────────────────────────────────

    // Observa as doses do dia atual reativamente.
    // Quando a meia-noite chega e carregarDosesHoje() é chamada, hoje() retorna
    // o novo timestamp → o Room devolve lista vazia → UI mostra tudo desmarcado.
    val dosesTomadas: StateFlow<Set<Int>> =
        doseDao.observarPorDia(hoje())
            .map { lista -> lista.map { it.horarioId }.toSet() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ─────────────────────────────────────────────────────────────────────
    // INIT — carrega horários e agenda reset diário
    // ─────────────────────────────────────────────────────────────────────

    val icon: String = "white_pill.png"

    init {
        // Carrega horários sempre que a lista de medicamentos mudar
        viewModelScope.launch {
            medicamentosPrescritos.collect { lista ->
                _horariosPrescritos.value = if (lista.isNotEmpty()) {
                    horarioDao.buscarPorMedicamentos(lista.map { it.id })
                } else {
                    emptyList()
                }
            }
        }

        // Verifica na abertura se o streak precisa ser zerado
        // (ex: app ficou fechado por 2 dias)
        viewModelScope.launch {
            verificarResetDiario()
        }

        // Agenda o reset automático toda meia-noite enquanto o app estiver aberto
        agendarResetMeianoite()
    }

    // ─────────────────────────────────────────────────────────────────────
    // RESET DIÁRIO
    // Chamado na abertura do app e todo dia à meia-noite.
    // Lógica:
    //   - Se ultimaData == hoje → nada muda (reiniciou hoje mesmo)
    //   - Se ultimaData == ontem → tomadoHoje volta para false, streak mantém
    //   - Se ultimaData < ontem  → tomadoHoje = false, streak = 0 (quebrou)
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun verificarResetDiario() {
        val hoje  = LocalDate.now().toString()
        val ontem = LocalDate.now().minusDays(1).toString()

        dataStore.edit { prefs ->
            val ultimaData = prefs[AppPrefsKeys.ULTIMA_DATA]

            when {
                // Primeira vez que abre o app: não há ultimaData, não faz nada
                ultimaData == null -> { /* sem histórico ainda */ }

                // Já processou hoje, não mexe
                ultimaData == hoje -> { /* já ok */ }

                // Completou ontem — mantém o streak, só reseta o status de hoje
                ultimaData == ontem -> {
                    prefs[AppPrefsKeys.TOMADO_HOJE] = false
                }

                // Passou mais de um dia sem completar — zera o streak
                else -> {
                    prefs[AppPrefsKeys.STREAK]      = 0
                    prefs[AppPrefsKeys.TOMADO_HOJE] = false
                }
            }
        }
    }

    /** Aguarda até a próxima meia-noite e dispara o reset; depois se reagenda. */
    private fun agendarResetMeianoite() {
        viewModelScope.launch {
            while (true) {
                delay(msAteMeianoite() + 1000L) // +1s de margem
                verificarResetDiario()
                // Após o reset, o Flow dosesTomadas precisa ser recriado com o
                // novo timestamp de hoje. Como o StateFlow foi inicializado com
                // hoje() no init, aqui forçamos um reload das doses do novo dia.
                carregarDosesHoje()
            }
        }
    }

    /**
     * Recarrega as doses do dia atual.
     * Necessário após a virada da meia-noite para garantir que a UI
     * reflita o novo dia mesmo com o app aberto.
     */
    private fun carregarDosesHoje() {
        viewModelScope.launch {
            // O Flow observarPorDia foi criado com o hoje() do init.
            // Para o caso do app ficar aberto durante a virada do dia,
            // buscamos manualmente e emitimos no StateFlow auxiliar.
            val doses = doseDao.buscarPorDia(hoje())
            _dosesTomadasManual.value = doses.map { it.horarioId }.toSet()
        }
    }

    // StateFlow auxiliar para quando o app vira a meia-noite aberto
    private val _dosesTomadasManual = MutableStateFlow<Set<Int>>(emptySet())

    // StateFlow final que a UI consome — mescla o Flow reativo do Room
    // com o manual (para o caso de virada de dia com app aberto)
    val dosesTomadasUI: StateFlow<Set<Int>> =
        combine(dosesTomadas, _dosesTomadasManual) { reativo, manual ->
            // Após meia-noite o manual será emptySet e o reativo também
            // (novo dia = sem doses). Antes da meia-noite o manual reflete
            // o que foi carregado no init.
            reativo + manual
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ─────────────────────────────────────────────────────────────────────
    // TOGGLE DOSE — marca ou desmarca um horário como tomado
    // ─────────────────────────────────────────────────────────────────────

    fun toggleDose(horarioId: Int) {
        viewModelScope.launch {
            val hojeTs    = hoje()
            val existente = doseDao.buscar(horarioId, hojeTs)

            if (existente != null) {
                // Desmarca
                doseDao.remover(existente)
                // Se desmarcou, já não completou o dia
                dataStore.edit { prefs ->
                    prefs[AppPrefsKeys.TOMADO_HOJE] = false
                }
            } else {
                // Marca
                doseDao.inserir(DoseTomada(horarioId, hojeTs))
                // Verifica se agora completou todos os horários do dia
                verificarConclusaoDoDia()
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // CONCLUSÃO DO DIA — incrementa streak se tomou tudo
    // ─────────────────────────────────────────────────────────────────────

    private suspend fun verificarConclusaoDoDia() {
        val hojeTexto        = LocalDate.now().toString()
        val totalHorariosHoje = _horariosPrescritos.value.size
        // Busca do banco para ter o número mais atualizado possível
        val totalTomados     = doseDao.buscarPorDia(hoje()).size

        // Só incrementa se tomou tudo E ainda não ganhou o ponto hoje
        if (totalHorariosHoje > 0 && totalTomados >= totalHorariosHoje) {
            dataStore.edit { prefs ->
                val ultimaData  = prefs[AppPrefsKeys.ULTIMA_DATA]

                // Já ganhou hoje — não duplica
                if (ultimaData == hojeTexto) return@edit

                val streakAtual = prefs[AppPrefsKeys.STREAK] ?: 0
                val recordAtual = prefs[AppPrefsKeys.RECORD] ?: 0
                val ontem       = LocalDate.now().minusDays(1).toString()

                // Se ontem também completou, continua a sequência; senão começa em 1
                val novoStreak  = if (ultimaData == ontem) streakAtual + 1 else 1

                prefs[AppPrefsKeys.STREAK]      = novoStreak
                prefs[AppPrefsKeys.TOMADO_HOJE] = true
                prefs[AppPrefsKeys.ULTIMA_DATA] = hojeTexto

                // Atualiza o recorde se bateu
                if (novoStreak > recordAtual) {
                    prefs[AppPrefsKeys.RECORD] = novoStreak
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // HELPER — próximo horário ativo (usado na tela do paciente)
    // ─────────────────────────────────────────────────────────────────────

    fun proximoHorarioAtivo(
        medicamento: MedicamentoPrescrito,
        horarios: List<HorarioPrescrito>,
        dosesTomadas: Set<Int>
    ): HorarioPrescrito? {
        val hojeTs      = hoje()
        val diasPassados = ((hojeTs - medicamento.dataInicio) / (1000L * 60 * 60 * 24)).toInt()

        if (diasPassados < 0 || diasPassados >= medicamento.diasTratamento) return null

        return horarios
            .filter { it.medicamentoId == medicamento.id }
            .sortedBy { it.horario }
            .firstOrNull { it.id !in dosesTomadas }
    }

    // ─────────────────────────────────────────────────────────────────────
    // ÁREA DO MÉDICO — Supabase + prescrição
    // ─────────────────────────────────────────────────────────────────────

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
                    .postgrest["Medicamentos"]
                    .select()
                    .decodeList<MedicamentoSupabase>()

                val prescritosMap  = medDao.observarTodos().first().associateBy { it.id }
                val todosHorarios  = horarioDao.buscarTodos()

                _medicamentosSupabase.value = resultado.map { med ->
                    val prescrito      = prescritosMap[med.id]
                    val horariosAtuais = todosHorarios
                        .filter { it.medicamentoId == med.id }
                        .map { HorarioUiState(it.horario, it.turno, it.qtde) }

                    MedicamentoUiState(
                        medicamento    = med,
                        selecionado    = prescrito != null,
                        horarios       = if (horariosAtuais.isNotEmpty()) horariosAtuais else listOf(HorarioUiState()),
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
            lista.map { if (it.medicamento.id == id) it.copy(horarios = it.horarios + HorarioUiState()) else it }
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
            lista.map { if (it.medicamento.id == id) it.copy(diasTratamento = dias) else it }
        }
    }

    fun salvarPrescricao(onConcluido: () -> Unit) {
        viewModelScope.launch {
            _salvando.value = true
            try {
                val selecionados    = _medicamentosSupabase.value.filter { it.selecionado }
                val inicioTratamento = amanha()

                // Limpa tudo e repersiste do zero
                medDao.limparTodos()
                horarioDao.limparTodos()
                doseDao.limparTodos()

                selecionados.forEach { ui ->
                    medDao.inserir(
                        MedicamentoPrescrito(
                            id             = ui.medicamento.id,
                            nome           = ui.medicamento.nome,
                            concentracao   = ui.medicamento.concentracao,
                            forma          = ui.medicamento.forma,
                            diasTratamento = ui.diasTratamento,
                            dataInicio     = inicioTratamento
                        )
                    )
                    ui.horarios.forEach { h ->
                        horarioDao.inserir(
                            HorarioPrescrito(
                                medicamentoId = ui.medicamento.id,
                                horario       = h.horario,
                                turno         = h.turno,
                                qtde          = h.qtde
                            )
                        )
                    }
                }

                val ids = selecionados.map { it.medicamento.id }
                _horariosPrescritos.value = horarioDao.buscarPorMedicamentos(ids)

                // Nova prescrição = doses limpas
                _dosesTomadasManual.value = emptySet()

                // Reset do status de hoje (nova prescrição = começa do zero)
                dataStore.edit { prefs ->
                    prefs[AppPrefsKeys.TOMADO_HOJE] = false
                }

                onConcluido()
            } finally {
                _salvando.value = false
            }
        }
    }

    // Horário próximo para exibir o alerta (null = nenhum)
    private val _horarioAlerta = MutableStateFlow<HorarioPrescrito?>(null)
    val horarioAlerta: StateFlow<HorarioPrescrito?> = _horarioAlerta

    fun verificarHorarioProximo() {
        viewModelScope.launch {
            val cal   = Calendar.getInstance()
            val agora = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val janela = 30

            val proximo = _horariosPrescritos.value.firstOrNull { horario ->
                val partes       = horario.horario.split(":")
                val horarioMin   = partes[0].toInt() * 60 + partes[1].toInt()
                val diff         = Math.abs(agora - horarioMin)
                diff <= janela && !dosesTomadas.value.contains(horario.id)
            }

            _horarioAlerta.value = proximo
        }
    }
    fun fecharAlerta() {
        _horarioAlerta.value = null
    }
}