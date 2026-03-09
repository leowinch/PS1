package com.example.teste

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


data class Remedio(val nome: String, val horario: String, val tomado: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemediosScreen(onBack: () -> Unit) {
    // 2. Lista de exemplo para visualizarmos a tela
    val listaRemedios = listOf(
        Remedio("Paracetamol", "08:00", true),
        Remedio("Vitamina C", "12:00", false),
        Remedio("Remédio Pressão", "20:00", false)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Meus Remédios", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1976D2))
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Lista de Hoje",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 3. O LazyColumn cria a lista com scroll
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listaRemedios) { remedio ->
                    CardRemedio(remedio)
                }
            }
        }
    }
}

@Composable
fun CardRemedio(remedio: Remedio) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (remedio.tomado) Color(0xFFE8F5E9) else Color.White
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = remedio.horario, fontSize = 16.sp, color = Color.Gray)
                Text(text = remedio.nome, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (remedio.tomado) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(40.dp)
                )
            } else {
                Button(
                    onClick = { /* Ação futura */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2))
                ) {
                    Text("TOMAR", fontSize = 16.sp)
                }
            }
        }
    }
}