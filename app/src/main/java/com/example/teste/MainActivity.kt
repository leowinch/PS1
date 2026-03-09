package com.example.teste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teste.ui.theme.TesteTheme
import kotlinx.coroutines.launch
import androidx.compose.runtime.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TesteTheme {
                var telaAtual by remember { mutableStateOf("home") }
                if (telaAtual == "home") {
                    HomeScreen(onNavigateToRemedios = { telaAtual = "remedios" })
                } else {
                   RemediosScreen(onBack = { telaAtual = "home" })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToRemedios: () -> Unit) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet (
                drawerContainerColor = Color(0xFFE3F2FD),
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Menu", modifier = Modifier.padding(16.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                NavigationDrawerItem(
                    label = {Text("Área do médico", color = Color.Black)},
                    selected = false,
                    onClick = { scope.launch { drawerState.close() } }
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Cuidado Diário", color = Color.White, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Progresso da Semana", fontSize = 20.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val dias = listOf("D", "S", "T", "Q", "Q", "S", "S")
                    dias.forEach { dia ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = dia, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.LightGray, shape = CircleShape),
                                contentAlignment = Alignment.Center
                            ) {}
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { /* Registrar medicação */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp), // Aumentei levemente para 65dp para dar mais respiro
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF56E1DE))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.icone_registro), // Use a sua imagem aqui
                            contentDescription = null,
                            modifier = Modifier.size(128.dp) // Tamanho menor para caber na linha
                        )

                        Spacer(modifier = Modifier.width(12.dp)) // Espaço entre a imagem e o texto

                        Text(
                            text = "REGISTRAR REMÉDIO",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(32.dp))

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    BigButton("Meus Remédios", Color(0xFFEFC845), idImagem = R.drawable.icone_remedios, click = onNavigateToRemedios)
                    BigButton("consultas", Color(0xFF3FE16A), idImagem = R.drawable.icone_consulta,  click = {/* tela que manda pra consulta*/}) // Ajustei para um verde mais suave
                    BigButton("Registrar pressão", Color(0xFFEF4736), idImagem = R.drawable.icone_pressao, click = {/* tela que manda pra registrar pressão*/}) // Ajustei para um vermelho mais suave
                }
            }
        }
    }
}

@Composable
fun BigButton(label: String, color: Color, idImagem: Int, click: () -> Unit) {
    Button(
        onClick = click,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp), // Aumentado para comportar o ícone de 140dp
        shape = MaterialTheme.shapes.medium,
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.Black),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
        contentPadding = PaddingValues(start = 0.dp, end = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically, // Centraliza verticalmente ícone e texto
            horizontalArrangement = Arrangement.Start // Começa da esquerda
        ) {
            Image(
                painter = painterResource(id = idImagem),
                contentDescription = label,
                modifier = Modifier
                    .size(180.dp) // Ícone bem grande como solicitado
                    .offset(x = (-10).dp)
            )

            Text(
                text = label.uppercase(),
                modifier = Modifier.weight(1f), // Faz o texto ocupar o restante do espaço à direita
                fontSize = 24.sp, // Aumentei um pouco mais para equilibrar com o ícone gigante
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center // Centraliza o texto dentro do espaço que sobrou
            )
        }
    }
}