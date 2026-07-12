package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.data.model.*
import com.guitarvault.app.ui.components.CustomFieldEditor
import com.guitarvault.app.ui.components.LabeledTextField
import com.guitarvault.app.ui.components.LabeledNumberField
import com.guitarvault.app.ui.components.SpecSection
import com.guitarvault.app.ui.viewmodel.CollectionViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGuitarScreen(
    guitarId: String?,
    onBack: () -> Unit,
    viewModel: CollectionViewModel = viewModel()
) {
    val existing = remember(guitarId) { guitarId?.let { viewModel.getGuitarById(it) } }

    // Form state
    var brand by remember { mutableStateOf(existing?.brand ?: "") }
    var model by remember { mutableStateOf(existing?.model ?: "") }
    var subModel by remember { mutableStateOf(existing?.subModel ?: "") }
    var yearStr by remember { mutableStateOf(existing?.year?.toString() ?: "") }
    var serialNumber by remember { mutableStateOf(existing?.serialNumber ?: "") }
    var countryOfOrigin by remember { mutableStateOf(existing?.countryOfOrigin ?: "") }
    var guitarType by remember { mutableStateOf(existing?.guitarType ?: GuitarType.ELECTRIC) }
    var handedness by remember { mutableStateOf(existing?.handedness ?: Handedness.RIGHT) }
    var bodyStyle by remember { mutableStateOf(existing?.bodyStyle ?: "") }
    var bodyShape by remember { mutableStateOf(existing?.bodyShape ?: "") }
    var bodyWood by remember { mutableStateOf(existing?.bodyWood ?: "") }
    var topWood by remember { mutableStateOf(existing?.topWood ?: "") }
    var backWood by remember { mutableStateOf(existing?.backWood ?: "") }
    var sidesWood by remember { mutableStateOf(existing?.sidesWood ?: "") }
    var finish by remember { mutableStateOf(existing?.finish ?: "") }
    var finishColor by remember { mutableStateOf(existing?.finishColor ?: "") }
    var bodyConstruction by remember { mutableStateOf(existing?.bodyConstruction ?: "") }
    var neckWood by remember { mutableStateOf(existing?.neckWood ?: "") }
    var fretboardWood by remember { mutableStateOf(existing?.fretboardWood ?: "") }
    var neckProfile by remember { mutableStateOf(existing?.neckProfile ?: "") }
    var neckConstruction by remember { mutableStateOf(existing?.neckConstruction ?: "") }
    var scaleLengthStr by remember { mutableStateOf(existing?.scaleLength?.toString() ?: "") }
    var numberOfFretsStr by remember { mutableStateOf(existing?.numberOfFrets?.toString() ?: "22") }
    var fretSize by remember { mutableStateOf(existing?.fretSize ?: "") }
    var fretMaterial by remember { mutableStateOf(existing?.fretMaterial ?: "") }
    var nutWidthStr by remember { mutableStateOf(existing?.nutWidth?.toString() ?: "") }
    var nutMaterial by remember { mutableStateOf(existing?.nutMaterial ?: "") }
    var inlays by remember { mutableStateOf(existing?.inlays ?: "") }
    var pickupConfig by remember { mutableStateOf(existing?.pickupConfiguration ?: "") }
    var neckPickup by remember { mutableStateOf(existing?.neckPickup ?: "") }
    var middlePickup by remember { mutableStateOf(existing?.middlePickup ?: "") }
    var bridgePickup by remember { mutableStateOf(existing?.bridgePickup ?: "") }
    var pickupBrand by remember { mutableStateOf(existing?.pickupBrand ?: "") }
    var electronics by remember { mutableStateOf(existing?.electronics ?: "") }
    var controlsDesc by remember { mutableStateOf(existing?.controlsDescription ?: "") }
    var activeElectronics by remember { mutableStateOf(existing?.activeElectronics ?: false) }
    var onboardPreamp by remember { mutableStateOf(existing?.onboardPreamp ?: "") }
    var bridgeType by remember { mutableStateOf(existing?.bridgeType ?: "") }
    var bridgeBrand by remember { mutableStateOf(existing?.bridgeBrand ?: "") }
    var tailpieceType by remember { mutableStateOf(existing?.tailpieceType ?: "") }
    var tuningMachines by remember { mutableStateOf(existing?.tuningMachines ?: "") }
    var tuningRatio by remember { mutableStateOf(existing?.tuningMachineRatio ?: "") }
    var tremoloType by remember { mutableStateOf(existing?.tremoloType ?: "") }
    var hardwareFinish by remember { mutableStateOf(existing?.hardwareFinish ?: "") }
    var pickguard by remember { mutableStateOf(existing?.pickguard ?: "") }
    var numberOfStringsStr by remember { mutableStateOf(existing?.numberOfStrings?.toString() ?: "6") }
    var weightStr by remember { mutableStateOf(existing?.weight?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var tagsStr by remember { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }

    val customFields = remember { mutableStateListOf<CustomField>().apply { existing?.customFields?.let { addAll(it) } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing != null) "Edit Guitar" else "Add Guitar") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    IconButton(onClick = {
                        val guitar = Guitar(
                            id = existing?.id ?: java.util.UUID.randomUUID().toString(),
                            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            brand = brand, model = model, subModel = subModel,
                            year = yearStr.toIntOrNull(),
                            serialNumber = serialNumber, countryOfOrigin = countryOfOrigin,
                            guitarType = guitarType, handedness = handedness,
                            bodyStyle = bodyStyle, bodyShape = bodyShape,
                            bodyWood = bodyWood, topWood = topWood, backWood = backWood, sidesWood = sidesWood,
                            finish = finish, finishColor = finishColor, bodyConstruction = bodyConstruction,
                            neckWood = neckWood, fretboardWood = fretboardWood,
                            neckProfile = neckProfile, neckConstruction = neckConstruction,
                            scaleLength = scaleLengthStr.toDoubleOrNull(),
                            numberOfFrets = numberOfFretsStr.toIntOrNull() ?: 22,
                            fretSize = fretSize, fretMaterial = fretMaterial,
                            nutWidth = nutWidthStr.toDoubleOrNull(), nutMaterial = nutMaterial, inlays = inlays,
                            pickupConfiguration = pickupConfig,
                            neckPickup = neckPickup, middlePickup = middlePickup, bridgePickup = bridgePickup,
                            pickupBrand = pickupBrand, electronics = electronics, controlsDescription = controlsDesc,
                            activeElectronics = activeElectronics, onboardPreamp = onboardPreamp,
                            bridgeType = bridgeType, bridgeBrand = bridgeBrand, tailpieceType = tailpieceType,
                            tuningMachines = tuningMachines, tuningMachineRatio = tuningRatio,
                            tremoloType = tremoloType, hardwareFinish = hardwareFinish, pickguard = pickguard,
                            numberOfStrings = numberOfStringsStr.toIntOrNull() ?: 6,
                            weight = weightStr.toDoubleOrNull(),
                            notes = notes,
                            tags = tagsStr.split(",").map { it.trim() }.filter { it.isNotBlank() },
                            customFields = customFields.toList(),
                            photos = existing?.photos ?: emptyList(),
                            valuation = existing?.valuation ?: Valuation(),
                            insurance = existing?.insurance ?: InsuranceInfo(),
                            conditionHistory = existing?.conditionHistory ?: emptyList(),
                            maintenanceLog = existing?.maintenanceLog ?: emptyList(),
                            provenance = existing?.provenance ?: emptyList(),
                            stringInfo = existing?.stringInfo ?: StringInfo()
                        )
                        if (existing != null) viewModel.updateGuitar(guitar) else viewModel.addGuitar(guitar)
                        onBack()
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Identity
            SpecSection(title = "Identity") {
                LabeledTextField("Brand", brand, { brand = it })
                LabeledTextField("Model", model, { model = it })
                LabeledTextField("Sub-Model", subModel, { subModel = it })
                LabeledNumberField("Year", yearStr, { yearStr = it })
                LabeledTextField("Serial Number", serialNumber, { serialNumber = it })
                LabeledTextField("Country of Origin", countryOfOrigin, { countryOfOrigin = it })
                GuitarTypeDropdown(selected = guitarType, onSelected = { guitarType = it })
                HandednessToggle(selected = handedness, onSelected = { handedness = it })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Body
            SpecSection(title = "Body") {
                LabeledTextField("Body Style", bodyStyle, { bodyStyle = it })
                LabeledTextField("Body Shape", bodyShape, { bodyShape = it })
                LabeledTextField("Body Wood", bodyWood, { bodyWood = it })
                LabeledTextField("Top Wood", topWood, { topWood = it })
                LabeledTextField("Back Wood", backWood, { backWood = it })
                LabeledTextField("Sides Wood", sidesWood, { sidesWood = it })
                LabeledTextField("Construction", bodyConstruction, { bodyConstruction = it })
                LabeledTextField("Finish", finish, { finish = it })
                LabeledTextField("Finish Color", finishColor, { finishColor = it })
                LabeledNumberField("Weight (kg)", weightStr, { weightStr = it })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Neck
            SpecSection(title = "Neck") {
                LabeledTextField("Neck Wood", neckWood, { neckWood = it })
                LabeledTextField("Fretboard Wood", fretboardWood, { fretboardWood = it })
                LabeledTextField("Neck Profile", neckProfile, { neckProfile = it })
                LabeledTextField("Neck Construction", neckConstruction, { neckConstruction = it })
                LabeledNumberField("Scale Length (mm)", scaleLengthStr, { scaleLengthStr = it })
                LabeledNumberField("Number of Frets", numberOfFretsStr, { numberOfFretsStr = it })
                LabeledTextField("Fret Size", fretSize, { fretSize = it })
                LabeledTextField("Fret Material", fretMaterial, { fretMaterial = it })
                LabeledNumberField("Nut Width (mm)", nutWidthStr, { nutWidthStr = it })
                LabeledTextField("Nut Material", nutMaterial, { nutMaterial = it })
                LabeledTextField("Inlays", inlays, { inlays = it })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Electronics
            SpecSection(title = "Electronics") {
                LabeledTextField("Pickup Configuration", pickupConfig, { pickupConfig = it })
                LabeledTextField("Neck Pickup", neckPickup, { neckPickup = it })
                LabeledTextField("Middle Pickup", middlePickup, { middlePickup = it })
                LabeledTextField("Bridge Pickup", bridgePickup, { bridgePickup = it })
                LabeledTextField("Pickup Brand", pickupBrand, { pickupBrand = it })
                LabeledTextField("Electronics", electronics, { electronics = it })
                LabeledTextField("Controls", controlsDesc, { controlsDesc = it })
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = activeElectronics, onCheckedChange = { activeElectronics = it })
                    Text("Active Electronics")
                }
                if (activeElectronics) {
                    LabeledTextField("Onboard Preamp", onboardPreamp, { onboardPreamp = it })
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Hardware
            SpecSection(title = "Hardware") {
                LabeledTextField("Bridge Type", bridgeType, { bridgeType = it })
                LabeledTextField("Bridge Brand", bridgeBrand, { bridgeBrand = it })
                LabeledTextField("Tailpiece", tailpieceType, { tailpieceType = it })
                LabeledTextField("Tuning Machines", tuningMachines, { tuningMachines = it })
                LabeledTextField("Tuning Ratio", tuningRatio, { tuningRatio = it })
                LabeledTextField("Tremolo Type", tremoloType, { tremoloType = it })
                LabeledTextField("Hardware Finish", hardwareFinish, { hardwareFinish = it })
                LabeledTextField("Pickguard", pickguard, { pickguard = it })
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Other
            SpecSection(title = "Other") {
                LabeledNumberField("Number of Strings", numberOfStringsStr, { numberOfStringsStr = it })
                LabeledTextField("Tags (comma-separated)", tagsStr, { tagsStr = it })
                LabeledTextField("Notes", notes, { notes = it }, singleLine = false)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Custom Fields
            SpecSection(title = "Custom Fields") {
                CustomFieldEditor(
                    fields = customFields.toList(),
                    onAdd = { customFields.add(it) },
                    onRemove = { id -> customFields.removeAll { it.id == id } }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GuitarTypeDropdown(selected: GuitarType, onSelected: (GuitarType) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text("Type: ${selected.displayName}") }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GuitarType.entries.forEach { type ->
                DropdownMenuItem(text = { Text(type.displayName) }, onClick = { onSelected(type); expanded = false })
            }
        }
    }
}

@Composable
private fun HandednessToggle(selected: Handedness, onSelected: (Handedness) -> Unit) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Handedness.entries.forEach { hand ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                RadioButton(selected = selected == hand, onClick = { onSelected(hand) })
                Text(hand.displayName)
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}
