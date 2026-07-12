package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.ui.viewmodel.CollectionViewModel
import kotlinx.coroutines.launch
import java.util.Random

/**
 * Spec field definition for the gamification picker.
 * Each entry maps to a getter (to check if filled) and a setter.
 */
data class SpecField(
    val key: String,
    val label: String,
    val hint: String,
    val getter: (Guitar) -> String,
    val setter: (Guitar, String) -> Guitar
)

object SpecFields {
    val fields = listOf(
        SpecField("serialNumber", "Serial Number", "e.g. CS20001",
            { it.serialNumber }, { g, v -> g.copy(serialNumber = v) }),
        SpecField("year", "Year", "e.g. 1999",
            { it.year?.toString() ?: "" }, { g, v -> g.copy(year = v.toIntOrNull()) }),
        SpecField("countryOfOrigin", "Country of Origin", "e.g. Japan",
            { it.countryOfOrigin }, { g, v -> g.copy(countryOfOrigin = v) }),
        SpecField("bodyWood", "Body Wood", "e.g. Alder",
            { it.bodyWood }, { g, v -> g.copy(bodyWood = v) }),
        SpecField("topWood", "Top Wood", "e.g. Flame Maple",
            { it.topWood }, { g, v -> g.copy(topWood = v) }),
        SpecField("neckWood", "Neck Wood", "e.g. Maple",
            { it.neckWood }, { g, v -> g.copy(neckWood = v) }),
        SpecField("fretboardWood", "Fretboard Wood", "e.g. Rosewood",
            { it.fretboardWood }, { g, v -> g.copy(fretboardWood = v) }),
        SpecField("neckProfile", "Neck Profile", "e.g. C, Slim Taper",
            { it.neckProfile }, { g, v -> g.copy(neckProfile = v) }),
        SpecField("scaleLength", "Scale Length (mm)", "e.g. 648",
            { it.scaleLength?.toString() ?: "" }, { g, v -> g.copy(scaleLength = v.toDoubleOrNull()) }),
        SpecField("numberOfFrets", "Number of Frets", "e.g. 22",
            { it.numberOfFrets.toString() }, { g, v -> g.copy(numberOfFrets = v.toIntOrNull() ?: g.numberOfFrets) }),
        SpecField("nutWidth", "Nut Width (mm)", "e.g. 43",
            { it.nutWidth?.toString() ?: "" }, { g, v -> g.copy(nutWidth = v.toDoubleOrNull()) }),
        SpecField("nutMaterial", "Nut Material", "e.g. Bone, Tusq",
            { it.nutMaterial }, { g, v -> g.copy(nutMaterial = v) }),
        SpecField("fretSize", "Fret Size", "e.g. Medium Jumbo",
            { it.fretSize }, { g, v -> g.copy(fretSize = v) }),
        SpecField("fretMaterial", "Fret Material", "e.g. Nickel Silver",
            { it.fretMaterial }, { g, v -> g.copy(fretMaterial = v) }),
        SpecField("inlays", "Inlays", "e.g. Dot, Block, Trapezoid",
            { it.inlays }, { g, v -> g.copy(inlays = v) }),
        SpecField("finish", "Finish Type", "e.g. Nitro, Poly",
            { it.finish }, { g, v -> g.copy(finish = v) }),
        SpecField("finishColor", "Finish Color", "e.g. Sunburst, Black",
            { it.finishColor }, { g, v -> g.copy(finishColor = v) }),
        SpecField("pickupConfiguration", "Pickup Configuration", "e.g. SSS, HH, HSH",
            { it.pickupConfiguration }, { g, v -> g.copy(pickupConfiguration = v) }),
        SpecField("neckPickup", "Neck Pickup", "e.g. Seymour Duncan SSL-1",
            { it.neckPickup }, { g, v -> g.copy(neckPickup = v) }),
        SpecField("bridgePickup", "Bridge Pickup", "e.g. Seymour Duncan SSL-5",
            { it.bridgePickup }, { g, v -> g.copy(bridgePickup = v) }),
        SpecField("pickupBrand", "Pickup Brand", "e.g. Seymour Duncan",
            { it.pickupBrand }, { g, v -> g.copy(pickupBrand = v) }),
        SpecField("bridgeType", "Bridge Type", "e.g. Tune-o-matic, Floyd Rose",
            { it.bridgeType }, { g, v -> g.copy(bridgeType = v) }),
        SpecField("tuningMachines", "Tuning Machines", "e.g. Grover Rotomatics",
            { it.tuningMachines }, { g, v -> g.copy(tuningMachines = v) }),
        SpecField("hardwareFinish", "Hardware Finish", "e.g. Chrome, Gold",
            { it.hardwareFinish }, { g, v -> g.copy(hardwareFinish = v) }),
        SpecField("weight", "Weight (kg)", "e.g. 3.6",
            { it.weight?.toString() ?: "" }, { g, v -> g.copy(weight = v.toDoubleOrNull()) }),
        SpecField("bodyShape", "Body Shape", "e.g. Stratocaster, Les Paul",
            { it.bodyShape }, { g, v -> g.copy(bodyShape = v) }),
        SpecField("bodyConstruction", "Body Construction", "e.g. Solid, Chambered",
            { it.bodyConstruction }, { g, v -> g.copy(bodyConstruction = v) })
    )
}

data class SpecChallenge(
    val guitar: Guitar,
    val field: SpecField
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySpecScreen(
    onBack: () -> Unit,
    viewModel: CollectionViewModel = viewModel()
) {
    val allGuitars by viewModel.guitars.collectAsState()
    val scope = rememberCoroutineScope()
    val random = remember { Random() }

    var challenge by remember { mutableStateOf<SpecChallenge?>(null) }
    var answer by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }
    var totalFilled by remember { mutableStateOf(0) }

    // Pick a challenge on first load
    LaunchedEffect(allGuitars) {
        if (challenge == null && allGuitars.isNotEmpty()) {
            challenge = pickChallenge(allGuitars, random)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Spec Challenge") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (allGuitars.isEmpty()) {
                Text("Add guitars to your collection first!", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("The daily challenge picks a random guitar and asks you to fill in one missing spec.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (challenge == null) {
                Text("All your guitars have all known specs filled in! 🎉",
                    style = MaterialTheme.typography.titleMedium)
            } else {
                val guitar = challenge!!.guitar
                val field = challenge!!.field

                // Guitar emoji + name
                Text("🎸", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = guitar.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // The challenge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Fill in this spec:", style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = field.label,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = field.hint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Answer input
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; saved = false },
                    label = { Text("Enter ${field.label}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (saved) {
                        { Icon(Icons.Default.Check, contentDescription = "Saved", tint = MaterialTheme.colorScheme.tertiary) }
                    } else null
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        val updated = field.setter(guitar, answer)
                        viewModel.updateGuitar(updated)
                        saved = true
                        totalFilled++
                    },
                    enabled = answer.isNotBlank() && !saved,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Spec")
                }

                if (saved) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "✅ Saved! Total specs filled: $totalFilled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Get another challenge
                OutlinedButton(
                    onClick = {
                        challenge = pickChallenge(allGuitars, random)
                        answer = ""
                        saved = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Casino, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Challenge")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats
                val totalSpecs = allGuitars.size * SpecFields.fields.size
                val filledSpecs = allGuitars.sumOf { g ->
                    SpecFields.fields.count { it.getter(g).isNotBlank() }
                }
                val percent = if (totalSpecs > 0) (filledSpecs * 100 / totalSpecs) else 0
                Text(
                    "Collection Completeness: $percent% ($filledSpecs / $totalSpecs)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Picks a random guitar that has at least one unfilled spec field,
 * then picks a random unfilled field from that guitar.
 */
private fun pickChallenge(
    guitars: List<Guitar>,
    random: Random
): SpecChallenge? {
    // Find guitars with at least one unfilled spec
    val candidates = guitars.mapNotNull { guitar ->
        val unfilled = SpecFields.fields.filter { it.getter(guitar).isBlank() }
        if (unfilled.isNotEmpty()) guitar to unfilled else null
    }

    if (candidates.isEmpty()) return null

    // Pick random guitar
    val (guitar, unfilledFields) = candidates[random.nextInt(candidates.size)]

    // Pick random unfilled field
    val field = unfilledFields[random.nextInt(unfilledFields.size)]

    return SpecChallenge(guitar, field)
}
