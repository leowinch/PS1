package com.example.teste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teste.ui.theme.TesteTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TesteTheme {
                AppNavegacao()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NAVEGAÇÃO CENTRAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppNavegacao(vm: MedicamentosViewModel = viewModel()) {
    var telaAtual by remember { mutableStateOf("paciente") }

    when (telaAtual) {
        "paciente"  -> MainScreen(
            vm                = vm,
            onAbrirAreaMedico = { telaAtual = "medico" },
            onAbrirConsultas  = { telaAtual = "consultas" },
            onAgendarConsulta = { telaAtual = "agendar" }
        )
        "medico"    -> TelaDoMedico(
            vm       = vm,
            onVoltar = { telaAtual = "paciente" }
        )
        "consultas" -> TelaConsultaPaciente(
            vm       = vm,
            onVoltar = { telaAtual = "paciente" }
        )
        "agendar"   -> TelaAgendarConsulta(
            vm       = vm,
            onVoltar = { telaAtual = "paciente" }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TELA PRINCIPAL (PACIENTE)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MedicamentosViewModel,
    onAbrirAreaMedico: () -> Unit,
    onAbrirConsultas: () -> Unit,
    onAgendarConsulta: () -> Unit
) {
    val listaRemedios by vm.medicamentosPrescritos.collectAsState()
    val listaHorarios by vm.horariosPrescritos.collectAsState()
    val dosesTomadas  by vm.dosesTomadas.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerLateral(
                onAreaMedicoClick = {
                    scope.launch { drawerState.close() }
                    onAbrirAreaMedico()
                },
                onAgendarConsultaClick = {
                    scope.launch { drawerState.close() }
                    onAgendarConsulta()
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("MEUS REMÉDIOS", fontWeight = FontWeight.Black) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Menu",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    },
                    actions = {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 8.dp),
                            tint = Color.Gray
                        )
                    }
                )
            },
            bottomBar = {
                BarraNavegacaoInferior(onAbrirConsultas = onAbrirConsultas)
            }
        ) { innerPadding ->
            if (listaRemedios.isEmpty()) {
                TelaVazia(innerPadding)
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.White)
                        .padding(horizontal = 16.dp)
                ) {
                    val turnos = listOf("Manhã", "Tarde", "Noite")

                    turnos.forEach { nomeTurno ->
                        val horariosDesteTurno = listaHorarios.filter { it.turno == nomeTurno }

                        if (horariosDesteTurno.isNotEmpty()) {
                            item { TurnoHeader(nomeTurno) }

                            items(horariosDesteTurno) { horario ->
                                val medicamento = listaRemedios.find { it.id == horario.medicamentoId }

                                if (medicamento != null) {
                                    val jaTomado = dosesTomadas.contains(horario.id)
                                    CardMedicamentoPrescrito(
                                        remedio        = medicamento,
                                        horarioInfo    = horario,
                                        jaTomado       = jaTomado,
                                        onToggleTomado = { vm.toggleDose(horario.id) }
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TELA: AGENDAR CONSULTA (uso do médico)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAgendarConsulta(
    vm: MedicamentosViewModel,
    onVoltar: () -> Unit
) {
    // Cores do Tema Dark Customizado
    val CorFundoEscuro = Color(0xFF121212)
    val CorSuperficie = Color(0xFF1E1E1E)
    val CorPrimaria = Color(0xFFBB86FC) // Roxo suave (padrão Material Dark) ou use Color(0xFF64B5F6) para Azul
    val CorTextoPrimario = Color.White
    val CorTextoSecundario = Color(0xFFB0B0B0)

    var mesAtual       by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var diaSelecionado by remember { mutableStateOf<LocalDate?>(vm.consultaAgendada.value) }
    var confirmado     by remember { mutableStateOf(false) }

    val nomeMes = mesAtual.month
        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }
    val ano          = mesAtual.year
    val diasNoMes    = mesAtual.lengthOfMonth()
    val primeiroDiaSemana = mesAtual.dayOfWeek.value % 7

    Scaffold(
        containerColor = CorFundoEscuro, // Fundo principal preto
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = CorFundoEscuro,
                    titleContentColor = CorTextoPrimario,
                    navigationIconContentColor = CorTextoPrimario
                ),
                title = { Text("AGENDAR CONSULTA", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->

        if (confirmado) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(CorFundoEscuro),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("✅", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Consulta agendada!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF81C784), // Verde mais claro para fundo preto
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    diaSelecionado?.let { data ->
                        val nomeDia = data.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                        val nomeMesDia = data.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                        Text(
                            "$nomeDia\n${data.dayOfMonth} de $nomeMesDia de ${data.year}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = CorTextoPrimario,
                            textAlign = TextAlign.Center,
                            lineHeight = 32.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(
                        onClick = onVoltar,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("VOLTAR", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Navegação de mês
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { mesAtual = mesAtual.minusMonths(1) },
                    modifier = Modifier.size(56.dp).background(CorSuperficie, CircleShape)
                ) {
                    Text("◀", fontSize = 22.sp, color = CorPrimaria)
                }

                Text(
                    "$nomeMes $ano",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = CorTextoPrimario
                )

                IconButton(
                    onClick = { mesAtual = mesAtual.plusMonths(1) },
                    modifier = Modifier.size(56.dp).background(CorSuperficie, CircleShape)
                ) {
                    Text("▶", fontSize = 22.sp, color = CorPrimaria)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dias da semana
            val semana = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
            Row(modifier = Modifier.fillMaxWidth()) {
                semana.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            dia,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = CorPrimaria
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grade de dias
            val totalCelulas = diasNoMes + primeiroDiaSemana
            val linhas = (totalCelulas + 6) / 7

            for (linha in 0 until linhas) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val numeroCelula = linha * 7 + col
                        val numeroDia = numeroCelula - primeiroDiaSemana + 1
                        val dataDestaCelula = if (numeroDia in 1..diasNoMes) mesAtual.withDayOfMonth(numeroDia) else null

                        val estaSelecionado = dataDestaCelula != null && dataDestaCelula == diaSelecionado
                        val ehPassado = dataDestaCelula != null && dataDestaCelula.isBefore(LocalDate.now())
                        val ehHoje = dataDestaCelula == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(4.dp)
                                .background(
                                    color = when {
                                        estaSelecionado -> CorPrimaria
                                        ehHoje          -> CorPrimaria.copy(alpha = 0.2f)
                                        dataDestaCelula != null -> CorSuperficie
                                        else            -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = dataDestaCelula != null && !ehPassado) {
                                    diaSelecionado = dataDestaCelula
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (numeroDia in 1..diasNoMes) {
                                Text(
                                    text = numeroDia.toString(),
                                    fontWeight = if (estaSelecionado) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 18.sp,
                                    color = when {
                                        estaSelecionado -> Color.Black // Texto preto no fundo roxo/azul para contraste
                                        ehPassado       -> Color(0xFF444444)
                                        else            -> CorTextoPrimario
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // Resumo e Botão Final
            if (diaSelecionado != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CorSuperficie),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, CorPrimaria.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📅 Data selecionada:", fontSize = 14.sp, color = CorTextoSecundario)
                        diaSelecionado?.let { data ->
                            val nomeDia = data.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                            val nomeMesDia = data.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                            Text(
                                "$nomeDia, ${data.dayOfMonth} de $nomeMesDia",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = CorPrimaria,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        vm.agendarConsulta(diaSelecionado!!)
                        confirmado = true
                    },
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorPrimaria),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONFIRMAR CONSULTA", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF332B00)), // Amarelo bem escuro
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "👆 Selecione um dia no calendário",
                        modifier = Modifier.padding(20.dp).fillMaxWidth(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
// ─────────────────────────────────────────────────────────────────────────────
// TELA: PRÓXIMA CONSULTA (uso do paciente)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaConsultaPaciente(
    vm: MedicamentosViewModel,
    onVoltar: () -> Unit
) {
    val consultaAgendada by vm.consultaAgendada.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MINHA CONSULTA", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Voltar",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (consultaAgendada == null) {
                // Sem consulta agendada
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text("📭", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        "Nenhuma consulta\nagendada ainda",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Fale com o seu médico\npara marcar uma data.",
                        fontSize = 18.sp,
                        color = Color.LightGray,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp
                    )
                }
            } else {
                val data = consultaAgendada!!
                val hoje = LocalDate.now()
                val diasAte = ChronoUnit.DAYS.between(hoje, data).toInt()

                val nomeDia = data.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }
                val nomeMes = data.month
                    .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp)
                ) {
                    Text("🏥", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Sua próxima consulta",
                        fontSize = 20.sp,
                        color = Color(0xFF5C6BC0),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Card com a data — número do dia enorme para fácil leitura
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                nomeDia,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF3949AB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${data.dayOfMonth}",
                                fontSize = 88.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1A237E),
                                lineHeight = 88.sp
                            )
                            Text(
                                "$nomeMes  ${data.year}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3949AB)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Contagem regressiva com cor dinâmica
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                diasAte <  0 -> Color(0xFFFFEBEE)
                                diasAte <= 3 -> Color(0xFFFFF3E0)
                                else         -> Color(0xFFE8F5E9)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when {
                                    diasAte <  0 -> "⚠️"
                                    diasAte == 0 -> "🔔"
                                    diasAte <= 3 -> "⏰"
                                    else         -> "✅"
                                },
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                when {
                                    diasAte <  0 -> "Consulta já passou"
                                    diasAte == 0 -> "É HOJE!"
                                    diasAte == 1 -> "Falta 1 dia"
                                    else         -> "Faltam $diasAte dias"
                                },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = when {
                                    diasAte <= 0 -> Color(0xFFC62828)
                                    diasAte <= 3 -> Color(0xFFE65100)
                                    else         -> Color(0xFF2E7D32)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BARRA DE NAVEGAÇÃO INFERIOR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BarraNavegacaoInferior(onAbrirConsultas: () -> Unit) {
    NavigationBar(
        containerColor = Color(0xFFF0F0F0),
        modifier = Modifier.height(120.dp)
    ) {
        NavigationBarItem(
            selected = true,
            onClick  = {},
            icon = {
                Image(
                    painterResource(R.drawable.icone_remedios),
                    contentDescription = "Início",
                    modifier = Modifier.size(64.dp)
                )
            },
            label = { Text("Início", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        )
        NavigationBarItem(
            selected = false,
            onClick  = onAbrirConsultas,
            icon = {
                Image(
                    painterResource(R.drawable.icone_consulta),
                    contentDescription = "Consultas",
                    modifier = Modifier.size(64.dp)
                )
            },
            label = { Text("Consultas", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CARD DE MEDICAMENTO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CardMedicamentoPrescrito(
    remedio: MedicamentoPrescrito,
    horarioInfo: HorarioPrescrito,
    jaTomado: Boolean,
    onToggleTomado: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (jaTomado) Color(0xFFE8F5E9) else Color(0xFFF3E5F5)
        ),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = remedio.nome,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (jaTomado) Color(0xFF2E7D32) else Color(0xFF4A148C)
                    )
                    Text(text = remedio.concentracao, fontSize = 16.sp, color = Color.DarkGray)
                    Text(text = remedio.forma, fontSize = 14.sp, color = Color.Gray)
                }
                Surface(
                    color = if (jaTomado) Color(0xFF81C784) else Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = horarioInfo.horario,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onToggleTomado,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (jaTomado) Color(0xFFC8E6C9) else Color(0xFFE1BEE7)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (!jaTomado) {
                    Image(
                        painterResource(R.drawable.icone_registro),
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (jaTomado) "TOMADO ✓" else "JÁ TOMEI",
                    color = if (jaTomado) Color(0xFF1B5E20) else Color(0xFF4A148C),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TELA VAZIA
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TelaVazia(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "💊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Nenhum remédio prescrito",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Text(
                "O médico ainda não selecionou\nos seus medicamentos",
                textAlign = TextAlign.Center,
                color = Color.LightGray
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CABEÇALHO DE TURNO
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun TurnoHeader(titulo: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp, horizontal = 1.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF9E9E9E)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(2.dp)
        ) {
            Text(
                titulo.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = 6.dp, y = 8.dp),
                textAlign = TextAlign.Center
            )
            Image(
                painter = painterResource(
                    when (titulo) {
                        "Manhã" -> R.drawable.acordar_flat
                        "Tarde" -> R.drawable.almoco_flat
                        else    -> R.drawable.dormir_flat
                    }
                ),
                contentDescription = null,
                modifier = Modifier
                    .size(92.dp)
                    .align(Alignment.CenterStart)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DRAWER LATERAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DrawerLateral(
    onAreaMedicoClick: () -> Unit,
    onAgendarConsultaClick: () -> Unit
) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Menu",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        NavigationDrawerItem(
            icon     = { Text("🩺", fontSize = 24.sp) },
            label    = { Text("Área do Médico", fontWeight = FontWeight.Bold) },
            selected = false,
            onClick  = onAreaMedicoClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        NavigationDrawerItem(
            icon     = { Text("📅", fontSize = 24.sp) },
            label    = { Text("Agendar Consulta", fontWeight = FontWeight.Bold) },
            selected = false,
            onClick  = onAgendarConsultaClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}