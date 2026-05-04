package com.example.teste

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.Calendar

object AgendaGenerator {

    private val DIAS_SEMANA = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
    private val MESES = listOf(
        "Jan", "Fev", "Mar", "Abr", "Mai", "Jun",
        "Jul", "Ago", "Set", "Out", "Nov", "Dez"
    )

    // Retorna lista de Calendars representando cada dia do tratamento (começa amanhã)
    private fun diasDeTratamento(diasTratamento: Int): List<Calendar> {
        val lista = mutableListOf<Calendar>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_MONTH, 1) // começa amanhã
        repeat(diasTratamento) {
            lista.add(cal.clone() as Calendar)
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return lista
    }

    // Agrupa uma lista de dias em semanas (cada semana começa no Domingo)
    private fun agruparEmSemanas(dias: List<Calendar>): List<List<Calendar?>> {
        if (dias.isEmpty()) return emptyList()

        val semanas = mutableListOf<List<Calendar?>>()
        val diasRestantes = dias.toMutableList()
        var primeiroDia = true

        while (diasRestantes.isNotEmpty()) {
            val semana = Array<Calendar?>(7) { null }

            if (primeiroDia) {
                // Posiciona os dias da primeira semana na coluna correta (Dom=0 ... Sáb=6)
                val primeiroColuna = diasRestantes.first().get(Calendar.DAY_OF_WEEK) - 1
                val inseridos = mutableListOf<Calendar>()
                for (dia in diasRestantes) {
                    val coluna = dia.get(Calendar.DAY_OF_WEEK) - 1
                    if (coluna >= primeiroColuna && semana[coluna] == null) {
                        semana[coluna] = dia
                        inseridos.add(dia)
                    } else if (coluna < primeiroColuna) {
                        break
                    }
                }
                diasRestantes.removeAll(inseridos)
                primeiroDia = false
            } else {
                // Semanas completas: preenche do Domingo ao Sábado
                for (col in 0..6) {
                    if (diasRestantes.isNotEmpty()) {
                        semana[col] = diasRestantes.removeFirst()
                    }
                }
            }

            semanas.add(semana.toList())
        }

        return semanas
    }

    fun gerarHtml(medicamentosComHorarios: List<MedicamentoComHorarios>): String {
        val corpo = StringBuilder()

        medicamentosComHorarios.forEach { item ->
            val dias = diasDeTratamento(item.prescrito.diasTratamento)
            val semanas = agruparEmSemanas(dias)
            val numSemanas = semanas.size

            // Cabeçalho do medicamento
            corpo.append("""
                <div class="remedio-bloco">
                    <div class="remedio-titulo">
                        💊 ${item.prescrito.nome} &nbsp;·&nbsp; ${item.prescrito.concentracao} &nbsp;·&nbsp; ${item.prescrito.forma}
                        <span class="badge">${item.prescrito.diasTratamento} dias · $numSemanas semana(s)</span>
                    </div>
            """)

            // Para cada horário, gera uma tabela com as semanas
            item.horarios.forEach { h ->
                corpo.append("""
                    <div class="horario-bloco">
                        <div class="horario-label">
                            ${emojiTurno(h.turno)} ${h.horario} &nbsp;—&nbsp; ${h.turno}
                        </div>
                        <table>
                """)

                semanas.forEachIndexed { semIdx, semana ->
                    // Linha de cabeçalho da semana
                    corpo.append("<tr class='header-row'>")
                    corpo.append("<th class='sem-label'>Sem. ${semIdx + 1}</th>")
                    for (col in 0..6) {
                        val dia = semana[col]
                        if (dia != null) {
                            val diaN = dia.get(Calendar.DAY_OF_MONTH)
                            val mesN = MESES[dia.get(Calendar.MONTH)]
                            val nomeDia = DIAS_SEMANA[col]
                            // Mostra nome do dia + data (ex: "Ter\n5 Mai")
                            corpo.append("<th class='dia-header'>$nomeDia<br/><span class='data-pequena'>$diaN $mesN</span></th>")
                        } else {
                            corpo.append("<th class='dia-vazio'>${DIAS_SEMANA[col]}</th>")
                        }
                    }
                    corpo.append("</tr>")

                    // Linha dos quadrados para marcar tomada
                    corpo.append("<tr class='box-row'>")
                    corpo.append("<td class='sem-label-vazia'></td>")
                    for (col in 0..6) {
                        val dia = semana[col]
                        if (dia != null) {
                            corpo.append("<td><div class='box'></div></td>")
                        } else {
                            corpo.append("<td class='dia-inativo'></td>")
                        }
                    }
                    corpo.append("</tr>")
                }

                corpo.append("</table></div>") // fecha horario-bloco
            }

            corpo.append("</div>") // fecha remedio-bloco
            corpo.append("<div class='separador'></div>")
        }

        val hoje = Calendar.getInstance()
        val dataGeracao = "${hoje.get(Calendar.DAY_OF_MONTH)}/${hoje.get(Calendar.MONTH) + 1}/${hoje.get(Calendar.YEAR)}"

        // Calcula a data de início (amanhã) para exibir no rodapé
        val amanha = Calendar.getInstance()
        amanha.add(Calendar.DAY_OF_MONTH, 1)
        val inicioTratamento = "${amanha.get(Calendar.DAY_OF_MONTH)} ${MESES[amanha.get(Calendar.MONTH)]}"

        return """
            <html>
            <head>
            <meta charset="UTF-8"/>
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                    font-family: Arial, sans-serif;
                    font-size: 11px;
                    color: #1a1a1a;
                    padding: 16px;
                    background: white;
                }
                h2 {
                    text-align: center;
                    font-size: 16px;
                    letter-spacing: 1px;
                    margin-bottom: 2px;
                    text-transform: uppercase;
                }
                .subtitulo {
                    text-align: center;
                    font-size: 11px;
                    color: #555;
                    margin-bottom: 4px;
                }
                .data-geracao {
                    text-align: center;
                    font-size: 10px;
                    color: #888;
                    margin-bottom: 12px;
                }
                .linha-paciente {
                    font-size: 12px;
                    margin-bottom: 14px;
                    border-bottom: 1px solid #333;
                    padding-bottom: 4px;
                }
                .remedio-bloco {
                    margin-bottom: 6px;
                    page-break-inside: avoid;
                }
                .remedio-titulo {
                    background: #4A148C;
                    color: white;
                    padding: 6px 10px;
                    font-size: 12px;
                    font-weight: bold;
                    border-radius: 4px 4px 0 0;
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                }
                .badge {
                    background: rgba(255,255,255,0.2);
                    padding: 2px 8px;
                    border-radius: 10px;
                    font-size: 10px;
                    font-weight: normal;
                }
                .horario-bloco {
                    border: 1px solid #ddd;
                    border-top: none;
                    margin-bottom: 0;
                }
                .horario-label {
                    background: #F3E5F5;
                    color: #4A148C;
                    font-weight: bold;
                    padding: 4px 10px;
                    font-size: 11px;
                    border-bottom: 1px solid #ddd;
                }
                table {
                    width: 100%;
                    border-collapse: collapse;
                }
                th, td {
                    border: 1px solid #ccc;
                    text-align: center;
                    vertical-align: middle;
                }
                .sem-label {
                    font-size: 9px;
                    color: #888;
                    padding: 3px 4px;
                    background: #fafafa;
                    width: 34px;
                    font-weight: normal;
                }
                .sem-label-vazia {
                    width: 34px;
                    background: #fafafa;
                }
                .dia-header {
                    font-size: 9px;
                    font-weight: bold;
                    padding: 3px 2px;
                    background: #f5f5f5;
                    color: #333;
                    line-height: 1.3;
                }
                .dia-vazio {
                    font-size: 9px;
                    color: #ccc;
                    background: #fafafa;
                    padding: 3px 2px;
                }
                .data-pequena {
                    font-weight: normal;
                    color: #555;
                    font-size: 8px;
                }
                .box-row td {
                    padding: 4px 2px;
                    height: 28px;
                }
                .box {
                    width: 16px;
                    height: 16px;
                    border: 1.5px solid #333;
                    margin: auto;
                    border-radius: 2px;
                }
                .dia-inativo {
                    background: #f0f0f0;
                }
                .header-row th {
                    border-bottom: none;
                }
                .separador {
                    height: 10px;
                }
            </style>
            </head>
            <body>
                <h2>Agenda de Medicamentos</h2>
                <div class="subtitulo">ESF ALTO DA BOA VISTA</div>
                <div class="data-geracao">Gerada em: $dataGeracao &nbsp;·&nbsp; Início do tratamento: $inicioTratamento</div>
                <div class="linha-paciente">Paciente: ___________________________________________</div>
                $corpo
            </body>
            </html>
        """.trimIndent()
    }

    private fun emojiTurno(turno: String) = when (turno) {
        "Manhã" -> "🌅"
        "Tarde" -> "☀️"
        else -> "🌙"
    }
}

fun imprimirAgenda(context: Context, lista: List<MedicamentoComHorarios>) {
    val webView = WebView(context)
    val html = AgendaGenerator.gerarHtml(lista)

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "Agenda_${System.currentTimeMillis()}"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager.print(
                jobName,
                printAdapter,
                PrintAttributes.Builder()
                    .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("pdf", "pdf", 300, 300))
                    .setMinMargins(PrintAttributes.Margins(500, 500, 500, 500))
                    .build()
            )
        }
    }

    webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
}