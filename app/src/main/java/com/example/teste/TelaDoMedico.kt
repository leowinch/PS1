package com.example.teste

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

fun normalizarTexto(texto: String): String {
    val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return semAcento.lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaDoMedico(
    vm: MedicamentosViewModel,
    onVoltar: () -> Unit
) {
    val medicamentos by vm.medicamentosSupabase.collectAsState()
    val carregando by vm.carregando.collectAsState()
    val salvando by vm.salvando.collectAsState()
    val erro by vm.erro.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var textoBusca by remember { mutableStateOf("") }

    val medicamentosFiltrados = remember(medicamentos, textoBusca) {
        if (textoBusca.isBlank()) medicamentos
        else {
            val busca = normalizarTexto(textoBusca)
            medicamentos.filter { ui ->
                normalizarTexto(ui.medicamento.nome).contains(busca) ||
                        normalizarTexto(ui.medicamento.forma).contains(busca) ||
                        normalizarTexto(ui.medicamento.concentracao).contains(busca)
            }
        }
    }

    val totalSelecionados = medicamentos.count { it.selecionado }

    LaunchedEffect(Unit) { vm.carregarMedicamentosSupabase() }

    Scaffold(
        topBar = {
            Surface(
                color = Color(0xFFF3E5F5),
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = onVoltar) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color(0xFF1A1A1A))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🩺 Área do Médico", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1A1A1A))
                            if (totalSelecionados > 0)
                                Text("$totalSelecionados selecionado(s)", fontSize = 13.sp, color = Color(0xFF4A148C))
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                // ... dentro do Scaffold na bottomBar ...
                if (totalSelecionados > 0) {
                    Text(
                        "$totalSelecionados medicamento(s) serão prescritos",
                        fontSize = 13.sp, color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Botão gerar PDF ATUALIZADO
                    OutlinedButton(
                        onClick = {
                            // Filtramos apenas os que o médico selecionou na tela
                            val listaParaAgenda = medicamentos
                                .filter { it.selecionado }
                                .map { ui ->
                                    // Convertemos o MedicamentoUiState para MedicamentoComHorarios
                                    MedicamentoComHorarios(
                                        prescrito = MedicamentoPrescrito(
                                            id = ui.medicamento.id,
                                            nome = ui.medicamento.nome,
                                            concentracao = ui.medicamento.concentracao,
                                            forma = ui.medicamento.forma,
                                            diasTratamento = ui.diasTratamento,
                                            dataInicio = System.currentTimeMillis()
                                        ),
                                        horarios = ui.horarios.map { h ->
                                            HorarioPrescrito(
                                                medicamentoId = ui.medicamento.id,
                                                horario = h.horario,
                                                turno = h.turno
                                            )
                                        }
                                    )
                                }
                            if (listaParaAgenda.isNotEmpty()) {
                                // Chamamos a função do AgendaUtils
                                imprimirAgenda(context, listaParaAgenda)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A148C)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF4A148C))
                    ) {
                        Text("🖨️  GERAR AGENDA EM PDF", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Button(
                    onClick = { vm.salvarPrescricao(onConcluido = onVoltar) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !salvando,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (salvando) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Text("✅  CONFIRMAR PRESCRIÇÃO", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            carregando -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4A148C))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Buscando medicamentos...", color = Color.Gray)
                    }
                }
            }
            erro != null -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = erro ?: "", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { vm.carregarMedicamentosSupabase() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))) {
                            Text("Tentar novamente", color = Color.White)
                        }
                    }
                }
            }
            medicamentos.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Nenhum medicamento encontrado\nno banco de dados", color = Color.Gray, textAlign = TextAlign.Center, fontSize = 16.sp)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color(0xFFFAFAFA))
                        .padding(horizontal = 16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = textoBusca,
                            onValueChange = { textoBusca = it },
                            placeholder = { Text("Buscar medicamento...", color = Color(0xFF757575)) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF4A148C)) },
                            trailingIcon = {
                                if (textoBusca.isNotEmpty())
                                    TextButton(onClick = { textoBusca = "" }) { Text("✕", color = Color.Gray) }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A148C),
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A),
                                cursorColor = Color(0xFF4A148C)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Toque para selecionar. Após selecionar, adicione horários e a duração do tratamento.",
                                fontSize = 13.sp, color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(12.dp), textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (medicamentosFiltrados.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum resultado para \"$textoBusca\"", color = Color.Gray, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        items(medicamentosFiltrados, key = { it.medicamento.id }) { uiState ->
                            CardMedicamentoSelecao(
                                uiState = uiState,
                                onToggle = { vm.alternarSelecao(uiState.medicamento.id) },
                                onAdicionarHorario = { vm.adicionarHorario(uiState.medicamento.id) },
                                onRemoverHorario = { idx -> vm.removerHorario(uiState.medicamento.id, idx) },
                                onHorarioChange = { idx, h -> vm.atualizarHorario(uiState.medicamento.id, idx, h) },
                                onTurnoChange = { idx, t -> vm.atualizarTurno(uiState.medicamento.id, idx, t) },
                                onDiasChange = { vm.atualizarDias(uiState.medicamento.id, it) }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardMedicamentoSelecao(
    uiState: MedicamentoUiState,
    onToggle: () -> Unit,
    onAdicionarHorario: () -> Unit,
    onRemoverHorario: (Int) -> Unit,
    onHorarioChange: (Int, String) -> Unit,
    onTurnoChange: (Int, String) -> Unit,
    onDiasChange: (Int) -> Unit
) {
    val med = uiState.medicamento
    val selecionado = uiState.selecionado
    val turnos = listOf("Manhã", "Tarde", "Noite")
    var diasTexto by remember(uiState.diasTratamento) { mutableStateOf(uiState.diasTratamento.toString()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, if (selecionado) Color(0xFF4A148C) else Color(0xFFE0E0E0), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = if (selecionado) Color(0xFFF3E5F5) else Color.White),
        elevation = CardDefaults.cardElevation(if (selecionado) 4.dp else 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            // Cabeçalho clicável
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                        .background(if (selecionado) Color(0xFF4A148C) else Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selecionado) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(med.nome, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        color = if (selecionado) Color(0xFF4A148C) else Color(0xFF1A1A1A))
                    Text("${med.concentracao}  •  ${med.forma}", fontSize = 13.sp, color = Color(0xFF555555))
                }
            }

            // Conteúdo expandido quando selecionado
            if (selecionado) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE1BEE7))
                Spacer(modifier = Modifier.height(12.dp))

                // Campo de dias de tratamento
                OutlinedTextField(
                    value = diasTexto,
                    onValueChange = { novo ->
                        diasTexto = novo
                        novo.toIntOrNull()?.let { if (it > 0) onDiasChange(it) }
                    },
                    label = { Text("Duração do tratamento (dias)", fontSize = 13.sp, color = Color(0xFF1A1A1A)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A148C),
                        unfocusedBorderColor = Color(0xFFBBBBBB),
                        focusedLabelColor = Color(0xFF4A148C),
                        unfocusedLabelColor = Color(0xFF1A1A1A),
                        focusedTextColor = Color(0xFF1A1A1A),
                        unfocusedTextColor = Color(0xFF1A1A1A),
                        cursorColor = Color(0xFF4A148C)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    supportingText = {
                        val dias = diasTexto.toIntOrNull() ?: 0
                        if (dias > 0) {
                            val semanas = Math.ceil(dias / 7.0).toInt()
                            Text("$semanas semana(s) na agenda", color = Color(0xFF4A148C), fontSize = 12.sp)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Lista de horários
                Text("Horários", fontSize = 13.sp, color = Color(0xFF1A1A1A), fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))

                uiState.horarios.forEachIndexed { index, horarioUi ->
                    HorarioItem(
                        horario = horarioUi.horario,
                        turno = horarioUi.turno,
                        turnos = turnos,
                        podeDeletar = uiState.horarios.size > 1,
                        onHorarioChange = { onHorarioChange(index, it) },
                        onTurnoChange = { onTurnoChange(index, it) },
                        onDeletar = { onRemoverHorario(index) }
                    )
                    if (index < uiState.horarios.size - 1)
                        Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botão adicionar horário
                OutlinedButton(
                    onClick = onAdicionarHorario,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4A148C)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A148C))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Adicionar horário", fontSize = 14.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorarioItem(
    horario: String,
    turno: String,
    turnos: List<String>,
    podeDeletar: Boolean,
    onHorarioChange: (String) -> Unit,
    onTurnoChange: (String) -> Unit,
    onDeletar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8F0FF), RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = horario,
                onValueChange = onHorarioChange,
                label = { Text("Horário", fontSize = 12.sp, color = Color(0xFF1A1A1A)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4A148C),
                    unfocusedBorderColor = Color(0xFFBBBBBB),
                    focusedLabelColor = Color(0xFF4A148C),
                    unfocusedLabelColor = Color(0xFF1A1A1A),
                    focusedTextColor = Color(0xFF1A1A1A),
                    unfocusedTextColor = Color(0xFF1A1A1A),
                    cursorColor = Color(0xFF4A148C)
                ),
                shape = RoundedCornerShape(8.dp)
            )
            if (podeDeletar) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDeletar) {
                    Icon(Icons.Default.Delete, contentDescription = "Remover", tint = Color(0xFFB00020))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            turnos.forEach { t ->
                val ativo = turno == t
                Surface(
                    modifier = Modifier.clickable { onTurnoChange(t) },
                    color = if (ativo) Color(0xFF4A148C) else Color(0xFFEDE7F6),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when (t) { "Manhã" -> "🌅 Manhã"; "Tarde" -> "☀️ Tarde"; else -> "🌙 Noite" },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal,
                        color = if (ativo) Color.White else Color(0xFF4A148C)
                    )
                }
            }
        }
    }
}