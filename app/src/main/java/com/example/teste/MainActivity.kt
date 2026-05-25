package com.example.teste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

    fun navegarComSenha(destino: String) {
        destinoAposSenha = destino
        mostrarDialogSenha = true
    }

    // Box raiz — o dialog flutua aqui, acima de tudo
    Box(modifier = Modifier.fillMaxSize()) {

        when (telaAtual) {
            "paciente" -> MainScreen(
                vm                = vm,
                onAbrirAreaMedico = { navegarComSenha("medico") },
                onAbrirConsultas  = { telaAtual = "consultas" },
                onAgendarConsulta = { navegarComSenha("agendar") }
            )
            "medico"   -> TelaDoMedico(
                vm       = vm,
                onVoltar = { telaAtual = "paciente" }
            )
            "consultas" -> TelaConsultaPaciente(
                vm       = vm,
                onVoltar = { telaAtual = "paciente" }
            )
            "agendar"  -> TelaAgendarConsulta(
                vm       = vm,
                onVoltar = { telaAtual = "paciente" }
            )
        }

        // Dialog sempre visível aqui, independente do drawer
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

    // Cores da tela do médico (dark)
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

                // Botão fechar no topo
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

                // Ícone de cadeado
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

                // Campo de senha
                OutlinedTextField(
                    value = senha,
                    onValueChange = {
                        senha = it
                        erroSenha = false // limpa erro ao digitar
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

                // Mensagem de erro
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

                // Botão confirmar
                Button(
                    onClick = {
                        // toLowerCase garante que qualquer combinação de maiúsculas/minúsculas funciona
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
    onAgendarConsulta: () -> Unit   // ← add this
) {
    val listaRemedios by vm.medicamentosPrescritos.collectAsState()
    val listaHorarios by vm.horariosPrescritos.collectAsState()
    val dosesTomadas by vm.dosesTomadas.collectAsState()

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
                            Icon(Icons.Default.Menu, contentDescription = "Menu", modifier = Modifier.size(32.dp))
                        }
                    },
                    actions = {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).padding(end = 8.dp),
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
                                        remedio = medicamento,
                                        horarioInfo = horario,
                                        jaTomado = jaTomado,
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
fun BarraNavegacaoInferior(onAbrirConsultas: () -> Unit) {
    NavigationBar(containerColor = Color(0xFFF0F0F0), modifier = Modifier.height(120.dp)) {
        NavigationBarItem(
            selected = true, onClick = {},
            icon = { Image(painterResource(R.drawable.icone_remedios), contentDescription = "Início", modifier = Modifier.size(64.dp)) },
            label = { Text("Início", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        )
        NavigationBarItem(
            selected = false, onClick = onAbrirConsultas,
            icon = { Image(painterResource(R.drawable.icone_consulta), contentDescription = "Consultas", modifier = Modifier.size(64.dp)) },
            label = { Text("Consultas", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        )
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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding).background(Color.White), contentAlignment = Alignment.Center) {
            if (consultaAgendada == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text("📭", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Nenhuma consulta\nagendada ainda", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 36.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Fale com o seu médico\npara marcar uma data.", fontSize = 18.sp, color = Color.LightGray, textAlign = TextAlign.Center, lineHeight = 28.sp)
                }
            } else {
                val data    = consultaAgendada!!
                val hoje    = LocalDate.now()
                val diasAte = ChronoUnit.DAYS.between(hoje, data).toInt()
                val nomeDia = data.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }
                val nomeMes = data.month.getDisplayName(TextStyle.FULL, Locale("pt", "BR")).replaceFirstChar { it.uppercase() }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
                    Text("🏥", fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("Sua próxima consulta", fontSize = 20.sp, color = Color(0xFF5C6BC0), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE8EAF6)), shape = RoundedCornerShape(24.dp), elevation = CardDefaults.cardElevation(6.dp)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(nomeDia, fontSize = 30.sp, fontWeight = FontWeight.Black, color = Color(0xFF3949AB))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${data.dayOfMonth}", fontSize = 88.sp, fontWeight = FontWeight.Black, color = Color(0xFF1A237E), lineHeight = 88.sp)
                            Text("$nomeMes  ${data.year}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3949AB))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = when { diasAte < 0 -> Color(0xFFFFEBEE); diasAte <= 3 -> Color(0xFFFFF3E0); else -> Color(0xFFE8F5E9) }),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text(when { diasAte < 0 -> "⚠️"; diasAte == 0 -> "🔔"; diasAte <= 3 -> "⏰"; else -> "✅" }, fontSize = 36.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                when { diasAte < 0 -> "Consulta já passou"; diasAte == 0 -> "É HOJE!"; diasAte == 1 -> "Falta 1 dia"; else -> "Faltam $diasAte dias" },
                                fontSize = 26.sp, fontWeight = FontWeight.Black,
                                color = when { diasAte <= 0 -> Color(0xFFC62828); diasAte <= 3 -> Color(0xFFE65100); else -> Color(0xFF2E7D32) }
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
                    containerColor         = CorFundoEscuro,
                    titleContentColor      = CorTextoPrimario,
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
                modifier = Modifier.fillMaxSize().padding(innerPadding).background(CorFundoEscuro),
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
