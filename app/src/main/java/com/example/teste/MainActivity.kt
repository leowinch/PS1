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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    vm: MedicamentosViewModel,
    onAbrirAreaMedico: () -> Unit
) {
    val listaRemedios by vm.medicamentosPrescritos.collectAsState()
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
                            modifier = Modifier.size(40.dp).padding(end = 8.dp),
                            tint = Color.Gray
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFFF0F0F0),
                    modifier = Modifier.height(100.dp)
                ) {
                    NavigationBarItem(
                        selected = true,
                        onClick = {},
                        icon = {
                            Image(
                                painterResource(R.drawable.icone_remedios),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                        },
                        label = { Text("Início", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Image(
                                painterResource(R.drawable.icone_consulta),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                        },
                        label = { Text("Consultas", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                        alwaysShowLabel = true
                    )
                    NavigationBarItem(
                        selected = false,
                        onClick = {},
                        icon = {
                            Image(
                                painterResource(R.drawable.icone_pressao),
                                contentDescription = null,
                                modifier = Modifier.size(80.dp)
                            )
                        },
                        label = {
                            Text(
                                "Pressão",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.offset(y = (-12).dp)
                            )
                        },
                        alwaysShowLabel = true
                    )
                }
            }
        ) { innerPadding ->
            if (listaRemedios.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "💊", fontSize = 64.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Nenhum remédio prescrito",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "O médico ainda não selecionou\nos seus medicamentos",
                            fontSize = 15.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.White)
                        .padding(horizontal = 16.dp)
                ) {
                    val turnos = listOf("Manhã", "Tarde", "Noite")
                    turnos.forEach { turno ->
                        val remediosDoTurno = listaRemedios.filter { it.turno == turno }
                        if (remediosDoTurno.isNotEmpty()) {
                            item { TurnoHeader(turno) }
                            items(remediosDoTurno) { remedio ->
                                CardMedicamentoPrescrito(remedio)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerLateral(onAreaMedicoClick: () -> Unit) {
    ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Menu",
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))
        NavigationDrawerItem(
            icon = { Text("🩺", fontSize = 24.sp) },
            label = {
                Column {
                    Text("Área do Médico", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Selecionar medicamentos", fontSize = 12.sp, color = Color.Gray)
                }
            },
            selected = false,
            onClick = onAreaMedicoClick,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun CardMedicamentoPrescrito(remedio: MedicamentoPrescrito) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5)),
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
                        color = Color(0xFF4A148C)
                    )
                    // Exibe concentração e forma vindos do banco
                    Text(
                        text = remedio.concentracao,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = remedio.forma,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                Surface(
                    color = Color(0xFFBDBDBD),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = remedio.horario,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1BEE7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Image(
                    painterResource(R.drawable.icone_registro),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "JÁ TOMEI",
                    color = Color(0xFF4A148C),
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
            }
        }
    }
}

@Composable
fun TurnoHeader(titulo: String) {
    val corFundo = Color(0xFF9E9E9E)
    val emoji = when (titulo) {
        "Manhã" -> "🌅"
        "Tarde" -> "☀️"
        else -> "🌙"
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(containerColor = corFundo),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 40.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = titulo.uppercase(),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
