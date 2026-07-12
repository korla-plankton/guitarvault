package com.guitarvault.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.guitarvault.app.data.model.Guitar
import com.guitarvault.app.ui.components.SpecRow
import com.guitarvault.app.ui.components.SpecSection

@Composable
fun SpecsTab(guitar: Guitar, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Identity
        SpecSection(title = "Identity") {
            SpecRow("Brand", guitar.brand)
            SpecRow("Model", guitar.model)
            SpecRow("Sub-Model", guitar.subModel)
            SpecRow("Year", guitar.year?.toString() ?: "")
            SpecRow("Serial Number", guitar.serialNumber)
            SpecRow("Country of Origin", guitar.countryOfOrigin)
            SpecRow("Production Number", guitar.productionNumber)
            SpecRow("Type", guitar.guitarType.displayName)
            SpecRow("Handedness", guitar.handedness.displayName)
            SpecRow("Strings", guitar.numberOfStrings.toString())
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Body
        SpecSection(title = "Body") {
            SpecRow("Body Style", guitar.bodyStyle)
            SpecRow("Body Shape", guitar.bodyShape)
            SpecRow("Body Wood", guitar.bodyWood)
            SpecRow("Top Wood", guitar.topWood)
            SpecRow("Back Wood", guitar.backWood)
            SpecRow("Sides Wood", guitar.sidesWood)
            SpecRow("Construction", guitar.bodyConstruction)
            SpecRow("Finish", guitar.finish)
            SpecRow("Finish Color", guitar.finishColor)
            SpecRow("Weight", guitar.weight?.let { "${it} kg" } ?: "")
            SpecRow("Cutaway", if (guitar.cutaway) "Yes" else "No")
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Neck
        SpecSection(title = "Neck") {
            SpecRow("Neck Wood", guitar.neckWood)
            SpecRow("Fretboard Wood", guitar.fretboardWood)
            SpecRow("Neck Profile", guitar.neckProfile)
            SpecRow("Neck Construction", guitar.neckConstruction)
            SpecRow("Scale Length", guitar.scaleLength?.let { "${it} mm" } ?: "")
            SpecRow("Number of Frets", guitar.numberOfFrets.toString())
            SpecRow("Fret Size", guitar.fretSize)
            SpecRow("Fret Material", guitar.fretMaterial)
            SpecRow("Nut Width", guitar.nutWidth?.let { "${it} mm" } ?: "")
            SpecRow("Nut Material", guitar.nutMaterial)
            SpecRow("Inlays", guitar.inlays)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Electronics
        SpecSection(title = "Electronics") {
            SpecRow("Pickup Configuration", guitar.pickupConfiguration)
            SpecRow("Neck Pickup", guitar.neckPickup)
            SpecRow("Middle Pickup", guitar.middlePickup)
            SpecRow("Bridge Pickup", guitar.bridgePickup)
            SpecRow("Pickup Brand", guitar.pickupBrand)
            SpecRow("Electronics", guitar.electronics)
            SpecRow("Controls", guitar.controlsDescription)
            SpecRow("Active Electronics", if (guitar.activeElectronics) "Yes" else "No")
            SpecRow("Onboard Preamp", guitar.onboardPreamp)
            SpecRow("Battery Type", guitar.batteryType)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Hardware
        SpecSection(title = "Hardware") {
            SpecRow("Bridge Type", guitar.bridgeType)
            SpecRow("Bridge Brand", guitar.bridgeBrand)
            SpecRow("Tailpiece", guitar.tailpieceType)
            SpecRow("Tuning Machines", guitar.tuningMachines)
            SpecRow("Tuning Ratio", guitar.tuningMachineRatio)
            SpecRow("Tremolo Type", guitar.tremoloType)
            SpecRow("Hardware Finish", guitar.hardwareFinish)
            SpecRow("Pickguard", guitar.pickguard)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Acoustic-specific
        if (guitar.guitarType == com.guitarvault.app.data.model.GuitarType.ACOUSTIC ||
            guitar.guitarType == com.guitarvault.app.data.model.GuitarType.CLASSICAL) {
            SpecSection(title = "Acoustic Details") {
                SpecRow("Soundhole Diameter", guitar.soundholeDiameter?.let { "${it} mm" } ?: "")
                SpecRow("Bracing Pattern", guitar.bracingPattern)
                SpecRow("Acoustic Pickup", guitar.acousticPickup)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Strings
        SpecSection(title = "Strings") {
            SpecRow("String Brand", guitar.stringInfo.brand)
            SpecRow("String Model", guitar.stringInfo.model)
            SpecRow("Gauge", guitar.stringInfo.gauge)
            SpecRow("Material", guitar.stringInfo.material)
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Case
        SpecSection(title = "Case & Accessories") {
            SpecRow("Case Included", if (guitar.caseIncluded) "Yes" else "No")
            SpecRow("Case Type", guitar.caseType)
            SpecRow("Case Brand", guitar.caseBrand)
            guitar.accessories.forEach { accessory ->
                SpecRow("Accessory", accessory)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Notes
        if (guitar.notes.isNotBlank()) {
            SpecSection(title = "Notes") {
                Text(guitar.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
