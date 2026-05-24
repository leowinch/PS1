package com.example.teste


import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

@Composable
fun CardOfensiva(
    streak: Int,
    record: Int,
    tomadoHoje: Boolean,
    modifier: Modifier = Modifier // Adicionado por boa prática
) {
    val mensagemMotivacao = when {
        streak == 0 -> "💊 Comece hoje! Tome seu remédio!"
        streak < 3  -> "🔥 Boa! Continue assim!"
        streak < 7  -> "💪 Você está indo muito bem!"
        streak < 14 -> "⭐ Incrível! Já são $streak dias seguidos!"
        else        -> "🏆 Você é campeão! $streak dias sem parar!"
    }

    val hoje = LocalDate.now().dayOfWeek.value % 7 // 0=Dom … 6=Sáb
    val diasSemana = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")

    Card(
        modifier = modifier // Usando o modifier que vem por parâmetro
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Banner motivacional
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    mensagemMotivacao,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100),
                    lineHeight = 24.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chama + número de dias + recorde
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bloco de fogo principal
                Row(
                    modifier = Modifier
                        .background(Color(0xFFFFF3E0), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Chama pulsando
                    val infiniteTransition = rememberInfiniteTransition(label = "flame")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(700, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    Text(
                        "🔥",
                        fontSize = 52.sp,
                        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "$streak",
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100),
                            lineHeight = 52.sp
                        )
                        Text(
                            "dias seguidos",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFBF360C)
                        )
                    }
                }

                // Recorde pessoal
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .background(Color(0xFFFFF8E1), RoundedCornerShape(14.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("🏆", fontSize = 28.sp)
                    Text(
                        "$record",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF5C3D11)
                    )
                    Text(
                        "Recorde",
                        fontSize = 13.sp,
                        color = Color(0xFF795548),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Calendário semanal
            Row(modifier = Modifier.fillMaxWidth()) {
                diasSemana.forEachIndexed { idx, nomeDia ->
                    val diff = idx - hoje
                    val estaHoje = diff == 0
                    val diasAtras = -diff
                    val tomadoNesseDia = diff < 0 && diasAtras <= streak
                    val perdidoNesseDia = diff < 0 && diasAtras > streak

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            nomeDia,
                            fontSize = 12.sp,
                            color = if (estaHoje) Color(0xFF3949AB) else Color.Gray,
                            fontWeight = if (estaHoje) FontWeight.Black else FontWeight.Normal
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = when {
                                        estaHoje && tomadoHoje -> Color(0xFFFFF3E0)
                                        estaHoje               -> Color(0xFFE8EAF6)
                                        tomadoNesseDia         -> Color(0xFFFFF3E0)
                                        perdidoNesseDia        -> Color(0xFFFFEBEE)
                                        else                   -> Color(0xFFF5F5F5)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when {
                                    estaHoje && tomadoHoje -> "🔥"
                                    estaHoje               -> "●"
                                    tomadoNesseDia         -> "🔥"
                                    perdidoNesseDia        -> "✕"
                                    else                   -> "○"
                                },
                                fontSize = if (tomadoNesseDia || (estaHoje && tomadoHoje)) 20.sp else 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}