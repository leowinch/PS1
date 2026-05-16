package com.example.teste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
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

@Composable
fun AppNavegacao(vm: MedicamentosViewModel = viewModel()) {
    // Estado de navegação centralizado
    var telaAtual by remember { mutableStateOf("paciente") }

    when (telaAtual) {
        "paciente" -> MainScreen(
            vm = vm,
            onAbrirAreaMedico = { telaAtual = "medico" }
        )
        "medico" -> TelaDoMedico(
            vm = vm,
            onVoltar = { telaAtual = "paciente" }
        )
        // Aqui você pode adicionar "consultas" ou "pressao" futuramente
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MedicamentosViewModel,
    onAbrirAreaMedico: () -> Unit
) {
    // Observando os estados do ViewModel (União das tabelas de Medicamento e Horário)
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
                BarraNavegacaoInferior()
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
                        // FILTRO CORRETO: Buscamos horários que pertencem a este turno
                        val horariosDesteTurno = listaHorarios.filter { it.turno == nomeTurno }

                        if (horariosDesteTurno.isNotEmpty()) {
                            item { TurnoHeader(nomeTurno) }

                            items(horariosDesteTurno) { horario ->
                                // BUSCA DO MEDICAMENTO: Relacionamos o Horário ao Medicamento pelo ID
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
            Column(modifier = Modifier.fillMaxSize(1f)) {
                DrawPills(horarioInfo.qtde)
                Text("QTDE = ${horarioInfo.qtde}")
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

@Composable
fun BarraNavegacaoInferior() {
    NavigationBar(containerColor = Color(0xFFF0F0F0), modifier = Modifier.height(100.dp)) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Image(painterResource(R.drawable.icone_remedios), null, Modifier.size(50.dp)) },
            label = { Text("Início", fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Image(painterResource(R.drawable.icone_consulta), null, Modifier.size(50.dp)) },
            label = { Text("Consultas", fontWeight = FontWeight.Bold) }
        )
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Image(painterResource(R.drawable.icone_pressao), null, Modifier.size(50.dp)) },
            label = { Text("Pressão", fontWeight = FontWeight.Bold) }
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
fun DrawerLateral(onAreaMedicoClick: () -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Menu", fontWeight = FontWeight.Black, fontSize = 22.sp, modifier = Modifier.padding(horizontal = 24.dp))
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        NavigationDrawerItem(
            icon = { Text("🩺", fontSize = 24.sp) },
            label = { Text("Área do Médico", fontWeight = FontWeight.Bold) },
            selected = false,
            onClick = onAreaMedicoClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
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

        // Até 5 comprimidos → desenha todos
        if (qtde <= 5) {

            repeat(qtde) {
                Image(
                    painter = painterResource(R.drawable.white_pill),
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 2.dp)
                )
            }

        } else {

            // Mais de 5 → 1 comprimido + número
            Image(
                painter = painterResource(R.drawable.white_pill),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(4.dp))

            Text(
                text = "x$qtde",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}