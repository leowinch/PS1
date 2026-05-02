package com.example.teste

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
// -------------------------------------------------------
// IMPORTANTE: Substitua pelos seus valores reais do Supabase
// Encontre em: Settings → API no painel do Supabase
// -------------------------------------------------------
object SupabaseConfig {
    const val SUPABASE_URL = "https://qofaajpypkupbafgheoi.supabase.co"
    const val SUPABASE_ANON_KEY = "sb_publishable_MN_mO3WOHssYuDmWGjq1Jw_gnzvDnQz"
}

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = SupabaseConfig.SUPABASE_URL,
        supabaseKey = SupabaseConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
    }
}
