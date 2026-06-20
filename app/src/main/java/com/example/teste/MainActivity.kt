package com.example.teste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teste.ui.theme.TesteTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.draw.clip

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

private const val SENHA_MEDICO = "esfaltodaboavista"

@Composable
fun AppNavegacao(vm: MedicamentosViewModel = viewModel()) {
    var telaAtual by remember { mutableStateOf("paciente") }
    var mostrarDialogSenha by remember { mutableStateOf(false) }
    var destinoAposSenha by remember { mutableStateOf("") }

    // NOVO
    var mostrarResumoDiario by remember { mutableStateOf(false) }

    val listaRemedios by vm.medicamentosPrescritos.collectAsState()
    val listaHorarios by vm.horariosPrescritos.collectAsState()

    LaunchedEffect(listaRemedios) {
        if (
            listaRemedios.isNotEmpty() &&
            vm.deveExibirResumoDiario()
        ) {
            mostrarResumoDiario = true
            vm.registrarAberturaHoje()
        }
    }

    fun navegarComSenha(destino: String) {
        destinoAposSenha = destino
        mostrarDialogSenha = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        when (telaAtual) {
            "paciente" -> MainScreen(
                vm = vm,
                onAbrirAreaMedico = { navegarComSenha("medico") },
                onAbrirConsultas = { telaAtual = "consultas" },
                onAgendarConsulta = { navegarComSenha("agendar") }
            )

            "medico" -> TelaDoMedico(
                vm = vm,
                onVoltar = { telaAtual = "paciente" }
            )

            "consultas" -> TelaConsultaPaciente(
                vm = vm,
                onVoltar = { telaAtual = "paciente" }
            )

            "agendar" -> TelaAgendarConsulta(
                vm = vm,
                onVoltar = { telaAtual = "paciente" }
            )
        }

        if (mostrarDialogSenha) {
            DialogSenha(
                onSenhaCorreta = {
                    mostrarDialogSenha = false
                    telaAtual = destinoAposSenha
                },
                onFechar = {
                    mostrarDialogSenha = false
                }
            )
        }

        // NOVO POPUP DE RESUMO
        if (mostrarResumoDiario) {
            DialogResumoMedicamentos(
                medicamentos = listaRemedios,
                horarios = listaHorarios,
                onFechar = {
                    mostrarResumoDiario = false
                }
            )
        }
    }
}

@Composable
fun DialogSenha(
    onSenhaCorreta: () -> Unit,
    onFechar: () -> Unit
) {
    var senha by remember { mutableStateOf("") }
    var senhaVisivel by remember { mutableStateOf(false) }
    var erroSenha by remember { mutableStateOf(false) }
    var tentativas by remember { mutableStateOf(0) }

    val corFundo    = Color(0xFF1E1E1E)
    val corPrimaria = Color(0xFFBB86FC)
    val corErro     = Color(0xFFCF6679)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = corFundo),
            elevation = CardDefaults.cardElevation(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onFechar,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color(0xFFB0B0B0)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = if (erroSenha) corErro.copy(alpha = 0.15f)
                            else corPrimaria.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = if (erroSenha) corErro else corPrimaria
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Área Restrita",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Digite a senha para continuar",
                    fontSize = 14.sp,
                    color = Color(0xFFB0B0B0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                OutlinedTextField(
                    value = senha,
                    onValueChange = {
                        senha = it
                        erroSenha = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("Senha", color = Color(0xFFB0B0B0))
                    },
                    placeholder = {
                        Text("Digite a senha", color = Color(0xFF666666))
                    },
                    singleLine = true,
                    isError = erroSenha,
                    visualTransformation = if (senhaVisivel)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    trailingIcon = {
                        TextButton(onClick = { senhaVisivel = !senhaVisivel }) {
                            Text(
                                text = if (senhaVisivel) "OCULTAR" else "MOSTRAR",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFBB86FC)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = corPrimaria,
                        unfocusedBorderColor = Color(0xFF444444),
                        errorBorderColor     = corErro,
                        focusedTextColor     = Color.White,
                        unfocusedTextColor   = Color.White,
                        cursorColor          = corPrimaria,
                        errorTextColor       = Color.White,
                        errorTrailingIconColor = corErro
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (erroSenha) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (tentativas >= 3)
                            "❌ Senha incorreta ($tentativas tentativas)"
                        else
                            "❌ Senha incorreta",
                        color = corErro,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (senha.trim().lowercase() == SENHA_MEDICO) {
                            onSenhaCorreta()
                        } else {
                            tentativas++
                            erroSenha = true
                            senha = ""
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = corPrimaria),
                    shape = RoundedCornerShape(14.dp),
                    enabled = senha.isNotEmpty()
                ) {
                    Text(
                        text = "ENTRAR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onFechar,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancelar",
                        fontSize = 15.sp,
                        color = Color(0xFFB0B0B0)
                    )
                }
            }
        }
    }
}

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
    val horarioAlerta by vm.horarioAlerta.collectAsState()
    val streak        by vm.streakDias.collectAsState()
    val tomadoHoje    by vm.tomadoHoje.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.verificarHorarioProximo()
    }

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
        Box(modifier = Modifier.fillMaxSize()) {

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
                    // ALTERAÇÃO 1 & 2: barra com telaAtual="inicio" e cores mais claras
                    BarraNavegacaoInferior(
                        telaAtual        = "inicio",
                        onAbrirInicio    = { /* já está na tela inicial */ },
                        onAbrirConsultas = onAbrirConsultas
                    )
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
                        item {
                            CardOfensiva(
                                streak     = streak,
                                tomadoHoje = tomadoHoje
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

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

            horarioAlerta?.let { horario ->
                val medicamento = listaRemedios.find { it.id == horario.medicamentoId }
                AlertaRemedio(
                    nomeRemedio = medicamento?.nome ?: "Remédio",
                    horario     = horario.horario,
                    onTomei     = {
                        vm.toggleDose(horario.id)
                        vm.fecharAlerta()
                    },
                    onFechar    = { vm.fecharAlerta() }
                )
            }
        }
    }
}

// =============================================================================
// ALTERAÇÃO 1 & 2: BarraNavegacaoInferior refatorada
//   - recebe telaAtual para saber qual aba destacar
//   - ícone ativo com alpha 1f, inativo com alpha 0.35f (mais claro)
//   - indicatorColor roxo claro em vez do escuro padrão
// =============================================================================
@Composable
fun BarraNavegacaoInferior(
    telaAtual: String,            // "inicio" ou "consultas"
    onAbrirInicio: () -> Unit,
    onAbrirConsultas: () -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFFF0F0F0),
        modifier = Modifier.height(120.dp)
    ) {
        NavigationBarItem(
            selected = telaAtual == "inicio",
            onClick  = onAbrirInicio,
            icon = {
                Image(
                    painter            = painterResource(R.drawable.icone_remedios),
                    contentDescription = "Início",
                    modifier           = Modifier.size(64.dp),
                    alpha              = if (telaAtual == "inicio") 1f else 0.35f
                )
            },
            label = {
                Text(
                    "Início",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = if (telaAtual == "inicio") Color(0xFF6200EE) else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor   = Color(0xFF6200EE),
                unselectedTextColor = Color.Gray,
                indicatorColor      = Color(0xFFE8DDFF)   // bolinha roxa clara sob ícone ativo
            )
        )
        NavigationBarItem(
            selected = telaAtual == "consultas",
            onClick  = onAbrirConsultas,
            icon = {
                Image(
                    painter            = painterResource(R.drawable.icone_consulta),
                    contentDescription = "Consultas",
                    modifier           = Modifier.size(64.dp),
                    alpha              = if (telaAtual == "consultas") 1f else 0.35f
                )
            },
            label = {
                Text(
                    "Consultas",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 16.sp,
                    color      = if (telaAtual == "consultas") Color(0xFF6200EE) else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedTextColor   = Color(0xFF6200EE),
                unselectedTextColor = Color.Gray,
                indicatorColor      = Color(0xFFE8DDFF)
            )
        )
    }
}

@Composable
fun AlertaRemedio(
    nomeRemedio: String,
    horario: String,
    onTomei: () -> Unit,
    onFechar: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            shape     = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            colors    = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier            = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick  = onFechar,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.Gray)
                    }
                }

                Text("💊", fontSize = 72.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Surface(color = Color(0xFFFFF3E0), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text       = "Hora do remédio!",
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color      = Color(0xFFBF360C)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(text = nomeRemedio, fontSize = 18.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "🕗 $horario", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF4A148C))

                Spacer(modifier = Modifier.height(28.dp))

                Button(
                    onClick  = onTomei,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "✅  Já tomei", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onFechar, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Fechar", fontSize = 16.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun CardMedicamentoPrescrito(
    remedio: MedicamentoPrescrito,
    horarioInfo: HorarioPrescrito,
    jaTomado: Boolean,
    onToggleTomado: () -> Unit
) {
    val corCard by animateColorAsState(targetValue = if (jaTomado) Color(0xFFE8F5E9) else Color(0xFFFFF3E0), label = "corCard")
    val escala by animateFloatAsState(targetValue = if (jaTomado) 1.02f else 1f, label = "escala")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = escala; scaleY = escala },
        colors = CardDefaults.cardColors(containerColor = corCard),
        elevation = CardDefaults.cardElevation(defaultElevation = if (jaTomado) 6.dp else 3.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(remedio.nome, fontSize = 24.sp, fontWeight = FontWeight.Black, color = if (jaTomado) Color(0xFF1B5E20) else Color(0xFFE65100))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(remedio.concentracao, fontSize = 18.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                    Text(remedio.forma, fontSize = 15.sp, color = Color.Gray)
                }
                Surface(color = if (jaTomado) Color(0xFF43A047) else Color(0xFFFF9800), shape = RoundedCornerShape(14.dp)) {
                    Text(horarioInfo.horario, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            DrawPills(horarioInfo.qtde)

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onToggleTomado,
                modifier = Modifier.fillMaxWidth().height(92.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (jaTomado) Color(0xFF2E7D32) else Color(0xFFFF9800)),
                shape = RoundedCornerShape(22.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Box(modifier = Modifier.size(60.dp).background(Color.White.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                        Text(if (jaTomado) "✓" else "💊", fontSize = 34.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Column {
                        Text(if (jaTomado) "REMÉDIO TOMADO" else "EU TOMEI", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(if (jaTomado) "Tudo certo por hoje" else "Toque para confirmar", fontSize = 14.sp, color = Color.White.copy(alpha = 0.92f))
                    }
                }
            }
            if (jaTomado) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), shape = RoundedCornerShape(14.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Text("✅", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Dose registrada com sucesso", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                    }
                }
            }
        }
    }
}

@Composable
fun TelaVazia(padding: PaddingValues) {
    Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "💊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Nenhum remédio prescrito", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Text("O médico ainda não selecionou\nos seus medicamentos", textAlign = TextAlign.Center, color = Color.LightGray)
        }
    }
}

@Composable
fun TurnoHeader(titulo: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 1.dp),
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
                modifier = Modifier.align(Alignment.Center).offset(x = 6.dp, y = 8.dp),
                textAlign = TextAlign.Center
            )

            Image(
                painter = painterResource(
                    when (titulo) {
                        "Manhã" -> R.drawable.acordar_flat
                        "Tarde" -> R.drawable.almoco_flat
                        else -> R.drawable.dormir_flat
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

@Composable
fun DrawerLateral(
    onAreaMedicoClick: () -> Unit,
    onAgendarConsultaClick: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = Color(0xFFF5F5F5),
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
        modifier = Modifier.width(300.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
                Column {
                    Text("MENU", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3949AB), letterSpacing = 2.sp)
                    Text("Navegação", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth().clickable { onAreaMedicoClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("👨‍⚕️", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Receitar Medicamento", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Prescrever e gerenciar", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.Lock, contentDescription = "Área restrita", tint = Color(0xFFBBBBBB), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Card(modifier = Modifier.fillMaxWidth().clickable { onAgendarConsultaClick() }, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("📅", fontSize = 28.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Agendar Consulta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        Text("Marcar novos horários", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.Lock, contentDescription = "Área restrita", tint = Color(0xFFBBBBBB), modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text("Versão 1.0.0", fontSize = 12.sp, color = Color.LightGray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun DrawPills(
    qtde: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (qtde <= 8) {
            repeat(qtde) {
                Image(
                    painter = painterResource(R.drawable.white_pill),
                    contentDescription = null,
                    modifier = Modifier
                        .size(30.dp)
                        .padding(end = 4.dp)
                )
            }
        } else {
            Image(
                painter = painterResource(R.drawable.white_pill),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "x$qtde",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 16.sp
            )
        }
    }
}

// =============================================================================
// ALTERAÇÃO 1: TelaConsultaPaciente agora tem barra de navegação inferior
// ALTERAÇÃO 3: Data exibida também no formato dd/mm/aaaa
// =============================================================================
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        // ALTERAÇÃO 1: barra inferior presente, aba "consultas" ativa,
        // clicar em "Início" navega de volta para MainScreen
        bottomBar = {
            BarraNavegacaoInferior(
                telaAtual        = "consultas",
                onAbrirInicio    = onVoltar,
                onAbrirConsultas = { /* já está nesta tela */ }
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
                val data    = consultaAgendada!!
                val hoje    = LocalDate.now()
                val diasAte = ChronoUnit.DAYS.between(hoje, data).toInt()
                val nomeDia = data.dayOfWeek
                    .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }
                val nomeMes = data.month
                    .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
                    .replaceFirstChar { it.uppercase() }

                // ALTERAÇÃO 3: formato dd/mm/aaaa
                val dataFormatada = "%02d/%02d/%04d"
                    .format(data.dayOfMonth, data.monthValue, data.year)

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

                    Card(
                        modifier  = Modifier.fillMaxWidth(),
                        colors    = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)),
                        shape     = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                nomeDia,
                                fontSize   = 30.sp,
                                fontWeight = FontWeight.Black,
                                color      = Color(0xFF3949AB)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${data.dayOfMonth}",
                                fontSize   = 88.sp,
                                fontWeight = FontWeight.Black,
                                color      = Color(0xFF1A237E),
                                lineHeight = 88.sp
                            )
                            Text(
                                "$nomeMes  ${data.year}",
                                fontSize   = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF3949AB)
                            )

                            // ALTERAÇÃO 3: badge com dd/mm/aaaa
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = Color(0xFFD1D5F0),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text          = dataFormatada,
                                    modifier      = Modifier.padding(horizontal = 18.dp, vertical = 7.dp),
                                    fontSize      = 20.sp,
                                    fontWeight    = FontWeight.Bold,
                                    color         = Color(0xFF1A237E),
                                    letterSpacing = 2.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors   = CardDefaults.cardColors(
                            containerColor = when {
                                diasAte < 0  -> Color(0xFFFFEBEE)
                                diasAte <= 3 -> Color(0xFFFFF3E0)
                                else         -> Color(0xFFE8F5E9)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier            = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment   = Alignment.CenterVertically
                        ) {
                            Text(
                                when {
                                    diasAte < 0  -> "⚠️"
                                    diasAte == 0 -> "🔔"
                                    diasAte <= 3 -> "⏰"
                                    else         -> "✅"
                                },
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                when {
                                    diasAte < 0  -> "Consulta já passou"
                                    diasAte == 0 -> "É HOJE!"
                                    diasAte == 1 -> "Falta 1 dia"
                                    else         -> "Faltam $diasAte dias"
                                },
                                fontSize   = 26.sp,
                                fontWeight = FontWeight.Black,
                                color      = when {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAgendarConsulta(
    vm: MedicamentosViewModel,
    onVoltar: () -> Unit
) {
    val CorFundoEscuro     = Color(0xFF121212)
    val CorSuperficie      = Color(0xFF1E1E1E)
    val CorPrimaria        = Color(0xFFBB86FC)
    val CorTextoPrimario   = Color.White
    val CorTextoSecundario = Color(0xFFB0B0B0)

    var mesAtual       by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var diaSelecionado by remember { mutableStateOf<LocalDate?>(vm.consultaAgendada.value) }
    var confirmado     by remember { mutableStateOf(false) }

    val nomeMes = mesAtual.month
        .getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        .replaceFirstChar { it.uppercase() }
    val ano               = mesAtual.year
    val diasNoMes         = mesAtual.lengthOfMonth()
    val primeiroDiaSemana = mesAtual.dayOfWeek.value % 7

    Scaffold(
        containerColor = CorFundoEscuro,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor             = CorFundoEscuro,
                    titleContentColor          = CorTextoPrimario,
                    navigationIconContentColor = CorTextoPrimario
                ),
                title = { Text("AGENDAR CONSULTA", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", modifier = Modifier.size(32.dp))
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
                    Text("Consulta agendada!", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF81C784), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    diaSelecionado?.let { data ->
                        val nomeDia    = data.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                        val nomeMesDia = data.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                        Text("$nomeDia\n${data.dayOfMonth} de $nomeMesDia de ${data.year}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CorTextoPrimario, textAlign = TextAlign.Center, lineHeight = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                    Button(onClick = onVoltar, modifier = Modifier.fillMaxWidth().height(64.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), shape = RoundedCornerShape(16.dp)) {
                        Text("VOLTAR", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                    }
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { mesAtual = mesAtual.minusMonths(1) }, modifier = Modifier.size(56.dp).background(CorSuperficie, CircleShape)) {
                    Text("◀", fontSize = 22.sp, color = CorPrimaria)
                }
                Text("$nomeMes $ano", fontSize = 24.sp, fontWeight = FontWeight.Black, color = CorTextoPrimario)
                IconButton(onClick = { mesAtual = mesAtual.plusMonths(1) }, modifier = Modifier.size(56.dp).background(CorSuperficie, CircleShape)) {
                    Text("▶", fontSize = 22.sp, color = CorPrimaria)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val semana = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
            Row(modifier = Modifier.fillMaxWidth()) {
                semana.forEach { dia ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(dia, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = CorPrimaria)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val totalCelulas = diasNoMes + primeiroDiaSemana
            val linhas = (totalCelulas + 6) / 7

            for (linha in 0 until linhas) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        val numeroCelula    = linha * 7 + col
                        val numeroDia       = numeroCelula - primeiroDiaSemana + 1
                        val dataDestaCelula = if (numeroDia in 1..diasNoMes) mesAtual.withDayOfMonth(numeroDia) else null
                        val estaSelecionado = dataDestaCelula != null && dataDestaCelula == diaSelecionado
                        val ehPassado       = dataDestaCelula != null && dataDestaCelula.isBefore(LocalDate.now())
                        val ehHoje          = dataDestaCelula == LocalDate.now()

                        Box(
                            modifier = Modifier
                                .weight(1f).aspectRatio(1f).padding(4.dp)
                                .background(
                                    color = when {
                                        estaSelecionado         -> CorPrimaria
                                        ehHoje                  -> CorPrimaria.copy(alpha = 0.2f)
                                        dataDestaCelula != null -> CorSuperficie
                                        else                    -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = dataDestaCelula != null && !ehPassado) { diaSelecionado = dataDestaCelula },
                            contentAlignment = Alignment.Center
                        ) {
                            if (numeroDia in 1..diasNoMes) {
                                Text(
                                    text = numeroDia.toString(),
                                    fontWeight = if (estaSelecionado) FontWeight.Black else FontWeight.Medium,
                                    fontSize = 18.sp,
                                    color = when {
                                        estaSelecionado -> Color.Black
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

            if (diaSelecionado != null) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CorSuperficie), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, CorPrimaria.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📅 Data selecionada:", fontSize = 14.sp, color = CorTextoSecundario)
                        diaSelecionado?.let { data ->
                            val nomeDia    = data.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                            val nomeMesDia = data.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                            Text("$nomeDia, ${data.dayOfMonth} de $nomeMesDia", fontSize = 18.sp, fontWeight = FontWeight.Black, color = CorPrimaria, textAlign = TextAlign.Center)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { vm.agendarConsulta(diaSelecionado!!); confirmado = true },
                    modifier = Modifier.fillMaxWidth().height(68.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CorPrimaria),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("CONFIRMAR CONSULTA", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF332B00)), shape = RoundedCornerShape(16.dp)) {
                    Text("👆 Selecione um dia no calendário", modifier = Modifier.padding(20.dp).fillMaxWidth(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFD54F), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun CardOfensiva(streak: Int, tomadoHoje: Boolean) {
    var animar by remember { mutableStateOf(false) }

    LaunchedEffect(tomadoHoje) {
        if (tomadoHoje) { animar = true; delay(1000); animar = false }
    }

    val escala by animateFloatAsState(targetValue = if (animar) 1.06f else 1f, animationSpec = tween(400), label = "escala")
    val corFundoInicio by animateColorAsState(targetValue = if (tomadoHoje) Color(0xFFFFF8F2) else Color(0xFFFAFAFA), animationSpec = tween(500), label = "corInicio")
    val corFundoFim    by animateColorAsState(targetValue = if (tomadoHoje) Color(0xFFFFE0B2) else Color(0xFFF5F5F5), animationSpec = tween(500), label = "corFim")

    val mensagemMotivacao = when {
        streak == 0 -> "💊 Comece sua jornada de saúde hoje!"
        streak < 3  -> "🔥 Bom começo! Mantenha o ritmo!"
        streak < 7  -> "💪 Você está construindo um hábito excelente!"
        streak < 14 -> "⭐ Incrível! Seu corpo agradece a constância."
        else        -> "🏆 Lendário! Você é imparável!"
    }

    val corTextoPrincipal = Color(0xFFE65100)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .graphicsLayer { scaleX = escala; scaleY = escala }
            .border(
                width = 1.5.dp,
                brush = Brush.horizontalGradient(colors = if (tomadoHoje) listOf(Color(0xFFFFB74D), Color(0xFFFF9800)) else listOf(Color.Transparent, Color.Transparent)),
                shape = RoundedCornerShape(24.dp)
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (tomadoHoje) 4.dp else 1.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.background(Brush.verticalGradient(listOf(corFundoInicio, corFundoFim))).padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("OFENSIVA ATUAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (tomadoHoje) Color(0xFFF57C00) else Color.Gray, letterSpacing = 1.5.sp)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) {
                        Text("$streak", fontSize = 42.sp, fontWeight = FontWeight.Black, color = corTextoPrincipal, lineHeight = 42.sp)
                        Text(if (streak == 1) " dia" else " dias", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = corTextoPrincipal.copy(alpha = 0.8f), modifier = Modifier.padding(bottom = 6.dp, start = 4.dp))
                    }
                }
                Box(modifier = Modifier.size(64.dp).background(color = if (tomadoHoje) Color(0xFFFFE0B2) else Color(0xFFE0E0E0), shape = RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Text(if (tomadoHoje) "🔥" else "💤", fontSize = 32.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = if (tomadoHoje) Color(0xFFFFCC80).copy(alpha = 0.5f) else Color(0xFFE0E0E0), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(mensagemMotivacao, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF424242), textAlign = TextAlign.Start)
            if (tomadoHoje) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                        Text("Tudo pronto por hoje! Meta batida.", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
fun DialogResumoMedicamentos(
    medicamentos: List<MedicamentoPrescrito>,
    horarios: List<HorarioPrescrito>,
    onFechar: () -> Unit
) {
    // Agrupa horários por turno para exibição organizada
    val turnos = listOf("Manhã", "Tarde", "Noite")
    val gruposPorTurno = turnos.mapNotNull { turno ->
        val horariosDoTurno = horarios.filter { it.turno == turno }
        if (horariosDoTurno.isEmpty()) null
        else turno to horariosDoTurno
    }

    // Total de doses no dia
    val totalDoses = horarios.size

    // Paleta do pop-up (tons frios/saúde, consistente com o tema do app)
    val corFundo       = Color(0xFF1A1A2E)   // azul-noite profundo
    val corSuperficie  = Color(0xFF16213E)   // azul-marinho escuro
    val corDestaque    = Color(0xFF7C83FD)   // lilás vibrante
    val corTexto       = Color(0xFFE8E8F0)
    val corTextoMuted  = Color(0xFF8888AA)

    val corTurno = mapOf(
        "Manhã" to Color(0xFFFFB347),        // âmbar solar
        "Tarde" to Color(0xFF56C596),         // verde-água
        "Noite" to Color(0xFF7C83FD)          // lilás
    )
    val emojTurno = mapOf(
        "Manhã" to "🌅",
        "Tarde" to "☀️",
        "Noite" to "🌙"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(tween(260)) + scaleIn(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                initialScale = 0.88f
            ),
            exit = fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.92f)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 560.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = corFundo),
                elevation = CardDefaults.cardElevation(24.dp)
            ) {
                Column {

                    // ── Cabeçalho ──────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(corSuperficie)
                            .padding(start = 24.dp, end = 8.dp, top = 20.dp, bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(end = 40.dp)) {
                            Text(
                                text = "Bom dia! 👋",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = corTextoMuted,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Seus remédios de hoje",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = corTexto
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            // Badge com total de doses
                            Surface(
                                color = corDestaque.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (totalDoses == 1) "1 dose programada" else "$totalDoses doses programadas",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = corDestaque
                                )
                            }
                        }

                        IconButton(
                            onClick = onFechar,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.07f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Fechar",
                                tint = corTextoMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // ── Lista de remédios por turno ─────────────────────────
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        gruposPorTurno.forEach { (turno, horariosDoTurno) ->
                            item(key = "header_$turno") {
                                // Cabeçalho do turno
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    Text(
                                        text = emojTurno[turno] ?: "",
                                        fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = turno.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = corTurno[turno] ?: corDestaque,
                                        letterSpacing = 1.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    HorizontalDivider(
                                        modifier = Modifier.weight(1f),
                                        color = (corTurno[turno] ?: corDestaque).copy(alpha = 0.25f),
                                        thickness = 1.dp
                                    )
                                }
                            }

                            itemsIndexed(
                                items = horariosDoTurno,
                                key = { _, h -> h.id }
                            ) { _, horario ->
                                val med = medicamentos.find { it.id == horario.medicamentoId }
                                if (med != null) {
                                    CardResumoRemedio(
                                        medicamento    = med,
                                        horario        = horario,
                                        corTurno       = corTurno[turno] ?: corDestaque,
                                        corSuperficie  = corSuperficie,
                                        corTexto       = corTexto,
                                        corTextoMuted  = corTextoMuted
                                    )
                                }
                            }
                        }
                    }

                    // ── Rodapé ─────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(corSuperficie)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Button(
                            onClick = onFechar,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = corDestaque),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(
                                text = "Entendido, vamos lá! 💪",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Este aviso aparece uma vez por dia",
                            fontSize = 12.sp,
                            color = corTextoMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}


@Composable
private fun CardResumoRemedio(
    medicamento:   MedicamentoPrescrito,
    horario:       HorarioPrescrito,
    corTurno:      Color,
    corSuperficie: Color,
    corTexto:      Color,
    corTextoMuted: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = corSuperficie),
        shape    = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Barra colorida lateral + horário
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(corTurno)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Nome e detalhes do medicamento
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = medicamento.nome,
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Black,
                    color      = corTexto,
                    maxLines   = 1
                )
                Text(
                    text     = medicamento.concentracao,
                    fontSize = 14.sp,
                    color    = corTextoMuted
                )
                Text(
                    text     = "${horario.qtde}x ${medicamento.forma}",
                    fontSize = 12.sp,
                    color    = corTextoMuted.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Chip do horário
            Surface(
                color = corTurno.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text       = horario.horario,
                    modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Black,
                    color      = corTurno
                )
            }
        }
    }
}


