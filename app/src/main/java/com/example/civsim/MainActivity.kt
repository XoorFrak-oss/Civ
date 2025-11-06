package com.example.civsim

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

@Composable
fun App() {
    MaterialTheme {
        var state by remember {
            mutableStateOf(
                State(
                    people = listOf(
                        Person("Aeon",'M',18.0, role = Role.GATHER),
                        Person("Naiara",'F',18.0, role = Role.RESEARCH)
                    )
                )
            )
        }
        val engine = remember { Engine(safety = true) }

        Scaffold(
            topBar = { SmallTopAppBar(title = { Text("CivSim (proto)") }) }
        ) { pad ->
            Column(Modifier.padding(pad).padding(16.dp).fillMaxSize()) {
                Text("Année ${state.year} • ${state.season}", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Eau ${"%.2f".format(state.inv[Resource.WATER])} | Fruits ${"%.2f".format(state.inv[Resource.FRUITS])}")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { state = engine.nextSeason(state) }) { Text("Tour suivant") }
                    Button(onClick = {
                        repeat(4) { state = engine.nextSeason(state) }
                    }) { Text("+1 an") }
                }
                Spacer(Modifier.height(16.dp))
                Text("Population :")
                state.people.forEach { p ->
                    Text("• ${p.name} (${p.sex}) — ${"%.1f".format(p.age)} ans — ${p.role} — HP ${p.hp}${if (!p.alive) " ☠️" else ""}")
                }
                Spacer(Modifier.height(16.dp))
                Text("Journal :", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.verticalScroll(rememberScrollState()).weight(1f
