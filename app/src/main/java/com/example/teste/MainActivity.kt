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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teste.ui.theme.TesteTheme


data class Medicamento(
    val nome: String,
    val dose: String,
    val horario: String,
    val turno: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TesteTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    // Dados fictícios
    val listaRemedios = listOf(
        Medicamento("Escitalopram", "1 comprimido (5mg)", "08:00", "Manhã"),
        Medicamento("Ibuprofeno", "20 gotas", "09:30", "Manhã"),
        Medicamento("Dipirona", "1 comprimido", "13:00", "Tarde"),
        Medicamento("Melatonina", "1 comprimido", "21:00", "Noite")
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MEUS REMÉDIOS", fontWeight = FontWeight.Black) },
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
                // 1. Aumentamos a altura total da barra aqui
                modifier = Modifier.height(100.dp)
            ) {

                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = {
                        Image(painterResource(R.drawable.icone_remedios),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                    },
                    label = {
                        Text("Início", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    },
                    alwaysShowLabel = true
                )


                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Image(painterResource(R.drawable.icone_consulta), contentDescription = null, modifier = Modifier.size(80.dp)) },
                    label = { Text("Consultas", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                    alwaysShowLabel = true
                )

                NavigationBarItem(
                    selected = false,
                    onClick = {},
                    icon = { Image(painterResource(R.drawable.icone_pressao), contentDescription = null, modifier = Modifier.size(80.dp)) },
                    label = { Text("Pressão", fontSize = 16.sp, fontWeight = FontWeight.Bold,modifier = Modifier.offset(y = (-12).dp) ) },
                    alwaysShowLabel = true
                )
            }
        }
    ) { innerPadding ->
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
                    item {
                        TurnoHeader(turno)
                    }

                    items(remediosDoTurno) { remedio ->
                        CardMedicamento(remedio)
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
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

@Composable
fun CardMedicamento(remedio: Medicamento) {
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
                    Text(
                        text = remedio.dose,
                        fontSize = 16.sp,
                        color = Color.DarkGray
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
                onClick = {  },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE1BEE7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Image(painterResource(R.drawable.icone_registro), contentDescription = null, modifier = Modifier.size(80.dp))
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