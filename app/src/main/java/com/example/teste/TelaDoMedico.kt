package com.example.teste

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.Normalizer

// -------------------------------------------------------
// Normaliza texto: remove acentos e converte para minúsculo
// Permite busca por aproximação sem acento e case-insensitive
// -------------------------------------------------------
fun normalizarTexto(texto: String): String {
    val semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    return semAcento.lowercase()
}

// -------------------------------------------------------
// Tela do Médico
// -------------------------------------------------------
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

    var textoBusca by remember { mutableStateOf("") }

    // Filtra por aproximação: ignora acentos e maiúsculas
    val medicamentosFiltrados = remember(medicamentos, textoBusca) {
        if (textoBusca.isBlank()) {
            medicamentos
        } else {
            val busca = normalizarTexto(textoBusca)
            medicamentos.filter { ui ->
                normalizarTexto(ui.medicamento.nome).contains(busca) ||
                        normalizarTexto(ui.medicamento.forma).contains(busca) ||
                        normalizarTexto(ui.medicamento.concentracao).contains(busca)
            }
        }
    }

    val totalSelecionados = medicamentos.count { it.selecionado }

    LaunchedEffect(Unit) {
        vm.carregarMedicamentosSupabase()
    }

    Scaffold(
        topBar = {
            // Usamos Surface + Column manualmente para ter controle total da cor do texto
            Surface(
                color = Color(0xFFF3E5F5),
                tonalElevation = 0.dp,
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
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color(0xFF1A1A1A)   // ← ícone escuro
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "🩺 Área do Médico",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color(0xFF1A1A1A)   // ← título escuro
                            )
                            if (totalSelecionados > 0) {
                                Text(
                                    "$totalSelecionados selecionado(s)",
                                    fontSize = 13.sp,
                                    color = Color(0xFF4A148C)
                                )
                            }
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
                if (totalSelecionados > 0) {
                    Text(
                        "$totalSelecionados medicamento(s) serão prescritos",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "✅  CONFIRMAR PRESCRIÇÃO",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { innerPadding ->

        when {
            carregando -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF4A148C))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Buscando medicamentos...", color = Color.Gray)
                    }
                }
            }

            erro != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = erro ?: "Erro desconhecido",
                            textAlign = TextAlign.Center,
                            color = Color.Gray,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { vm.carregarMedicamentosSupabase() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A148C))
                        ) {
                            Text("Tentar novamente", color = Color.White)
                        }
                    }
                }
            }

            medicamentos.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nenhum medicamento encontrado\nno banco de dados",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
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
                    // Campo de busca
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = textoBusca,
                            onValueChange = { textoBusca = it },
                            placeholder = {
                                Text(
                                    "Buscar medicamento...",
                                    color = Color(0xFF757575)
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF4A148C)
                                )
                            },
                            trailingIcon = {
                                if (textoBusca.isNotEmpty()) {
                                    TextButton(onClick = { textoBusca = "" }) {
                                        Text("✕", color = Color.Gray)
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF4A148C),
                                unfocusedBorderColor = Color(0xFFCCCCCC),
                                focusedLabelColor = Color(0xFF4A148C),
                                cursorColor = Color(0xFF4A148C),
                                focusedTextColor = Color(0xFF1A1A1A),
                                unfocusedTextColor = Color(0xFF1A1A1A)
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    // Instrução
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Toque em um medicamento para selecioná-lo e definir horário e turno.",
                                fontSize = 13.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(12.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Resultado vazio da busca
                    if (medicamentosFiltrados.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Nenhum resultado para \"$textoBusca\"",
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        items(medicamentosFiltrados, key = { it.medicamento.id }) { uiState ->
                            CardMedicamentoSelecao(
                                uiState = uiState,
                                onToggle = { vm.alternarSelecao(uiState.medicamento.id) },
                                onHorarioChange = { vm.atualizarHorario(uiState.medicamento.id, it) },
                                onTurnoChange = { vm.atualizarTurno(uiState.medicamento.id, it) }
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

// -------------------------------------------------------
// Card de medicamento com seleção + campos de horário e turno
// -------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardMedicamentoSelecao(
    uiState: MedicamentoUiState,
    onToggle: () -> Unit,
    onHorarioChange: (String) -> Unit,
    onTurnoChange: (String) -> Unit
) {
    val med = uiState.medicamento
    val selecionado = uiState.selecionado
    val turnos = listOf("Manhã", "Tarde", "Noite")

    val corBorda = if (selecionado) Color(0xFF4A148C) else Color(0xFFE0E0E0)
    val corFundo = if (selecionado) Color(0xFFF3E5F5) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, corBorda, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        elevation = CardDefaults.cardElevation(if (selecionado) 4.dp else 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (selecionado) Color(0xFF4A148C) else Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (selecionado) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = med.nome,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selecionado) Color(0xFF4A148C) else Color(0xFF1A1A1A)
                    )
                    Text(
                        text = "${med.concentracao}  •  ${med.forma}",
                        fontSize = 13.sp,
                        color = Color(0xFF555555)
                    )
                }
            }

            if (selecionado) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFE1BEE7))
                Spacer(modifier = Modifier.height(12.dp))

                // Campo de horário com fontes pretas
                OutlinedTextField(
                    value = uiState.horario,
                    onValueChange = { onHorarioChange(it) },
                    label = {
                        Text(
                            "Horário (ex: 08:00)",
                            fontSize = 13.sp,
                            color = Color(0xFF1A1A1A)   // ← label preta
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A148C),
                        unfocusedBorderColor = Color(0xFFBBBBBB),
                        focusedLabelColor = Color(0xFF4A148C),
                        unfocusedLabelColor = Color(0xFF1A1A1A),  // ← label sem foco preta
                        focusedTextColor = Color(0xFF1A1A1A),     // ← texto digitado preto
                        unfocusedTextColor = Color(0xFF1A1A1A),   // ← texto sem foco preto
                        cursorColor = Color(0xFF4A148C)
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Turno",
                    fontSize = 13.sp,
                    color = Color(0xFF1A1A1A),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    turnos.forEach { turno ->
                        val ativo = uiState.turno == turno
                        Surface(
                            modifier = Modifier.clickable { onTurnoChange(turno) },
                            color = if (ativo) Color(0xFF4A148C) else Color(0xFFEDE7F6),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = when (turno) {
                                    "Manhã" -> "🌅 Manhã"
                                    "Tarde" -> "☀️ Tarde"
                                    else -> "🌙 Noite"
                                },
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                fontWeight = if (ativo) FontWeight.Bold else FontWeight.Normal,
                                color = if (ativo) Color.White else Color(0xFF4A148C)
                            )
                        }
                    }
                }
            }
        }
    }
}