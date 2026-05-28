package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Lead
import com.example.ui.viewmodel.LeadViewModel
import com.example.ui.Translations
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TurkeyClientMap(
    viewModel: LeadViewModel,
    modifier: Modifier = Modifier
) {
    val leads by viewModel.leads.collectAsState()
    val selectedLead by viewModel.selectedLead.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val lang by viewModel.appLanguage.collectAsState()

    // Interactive simulated user position & range settings
    var userCityCenter by remember { mutableStateOf("Trabzon") }
    var searchRadiusKm by remember { mutableStateOf(200f) }
    var showOnlyHighOpportunity by remember { mutableStateOf(false) }

    // Coordinates of major central nodes
    val cityCoordinates = mapOf(
        "Trabzon" to Pair(41.0027, 39.7168),
        "Istanbul" to Pair(41.0082, 28.9784),
        "Ankara" to Pair(39.9334, 32.8597),
        "Izmir" to Pair(38.4237, 27.1428),
        "Antalya" to Pair(36.8969, 30.7133),
        "Bursa" to Pair(40.1885, 29.0610)
    )

    val currentCenterCoords = cityCoordinates[userCityCenter] ?: Pair(41.0027, 39.7168)

    // Map limits
    val westLong = 25.5
    val eastLong = 45.0
    val southLat = 35.5
    val northLat = 42.5

    // Pulsing and radar scanner animations
    val infiniteTransition = rememberInfiniteTransition(label = "RadarScanning")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "Radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "Alpha"
    )

    // Calculate distances to leads and filter based on radius and score
    val leadsWithDistance = remember(leads, userCityCenter, searchRadiusKm, showOnlyHighOpportunity) {
        leads.map { lead ->
            val dLat = lead.latitude - currentCenterCoords.first
            val dLng = lead.longitude - currentCenterCoords.second
            // Approximation of distance in km: 1 degree latitude ~ 111 km, longitude ~ 85 km in Turkey
            val distance = sqrt((dLat * 111.0) * (dLat * 111.0) + (dLng * 85.0) * (dLng * 85.0))
            Pair(lead, distance)
        }.filter { (lead, dist) ->
            val isWithinRadius = dist <= searchRadiusKm
            val isScoreMatch = !showOnlyHighOpportunity || lead.leadScorePercent >= 80
            isWithinRadius && isScoreMatch
        }.sortedBy { it.second }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A)) // Clean, modern slate dark background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header segment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = Translations.get("map_title", lang),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = Translations.get("map_subtitle", lang),
                        fontSize = 11.sp,
                        color = Color.LightGray.copy(alpha = 0.7f)
                    )
                }

                // Interactive city base quick filter
                var cityExpanded by remember { mutableStateOf(false) }
                Box {
                    Button(
                        onClick = { cityExpanded = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(userCityCenter, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = cityExpanded,
                        onDismissRequest = { cityExpanded = false },
                        modifier = Modifier.background(Color(0xFF1E293B))
                    ) {
                        cityCoordinates.keys.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city, color = Color.White, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    userCityCenter = city
                                    cityExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Radar Scan configurations (Simplified, modern sleek design)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "${Translations.get("distance_filter", lang)}: ${searchRadiusKm.toInt()} km",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Checkbox(
                                checked = showOnlyHighOpportunity,
                                onCheckedChange = { showOnlyHighOpportunity = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF38BDF8),
                                    uncheckedColor = Color.LightGray
                                )
                            )
                            Text("${Translations.get("high_yield", lang)} (>=80%)", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }

                    Slider(
                        value = searchRadiusKm,
                        onValueChange = { searchRadiusKm = it },
                        valueRange = 50f..600f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF38BDF8),
                            activeTrackColor = Color(0xFF38BDF8),
                            inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                        )
                    )
                }
            }

            // Central Turkey Geographical Canvas with locator scanning zones
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0B1329))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            ) {
                val density = LocalDensity.current
                var canvasWidth by remember { mutableStateOf(0f) }
                var canvasHeight by remember { mutableStateOf(0f) }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            if (size.width > 0 && size.height > 0) {
                                canvasWidth = size.width.toFloat()
                                canvasHeight = size.height.toFloat()
                            }
                        }
                        .pointerInput(leadsWithDistance) {
                            detectTapGestures { offset ->
                                val touchThreshold = with(density) { 36.dp.toPx() }
                                var closestLead: Lead? = null
                                var minDistance = Double.MAX_VALUE

                                leadsWithDistance.forEach { (lead, _) ->
                                    val projX = ((lead.longitude - westLong) / (eastLong - westLong)) * canvasWidth
                                    val projY = ((northLat - lead.latitude) / (northLat - southLat)) * canvasHeight
                                    val dist = sqrt((offset.x - projX) * (offset.x - projX) + (offset.y - projY) * (offset.y - projY))
                                    if (dist < minDistance && dist < touchThreshold) {
                                        minDistance = dist.toDouble()
                                        closestLead = lead
                                    }
                                }

                                if (closestLead != null) {
                                    viewModel.selectLead(closestLead!!)
                                }
                            }
                        }
                ) {
                    // Draw coordinates graticule references
                    val gridColor = Color(0xFF334155).copy(alpha = 0.3f)
                    for (lat in 36..42) {
                        val y = ((northLat - lat.toDouble()) / (northLat - southLat)).toFloat() * size.height
                        drawLine(color = gridColor, start = Offset(0f, y), end = Offset(size.width, y), strokeWidth = 1f)
                    }
                    for (lng in 26..44 step 2) {
                        val x = ((lng.toDouble() - westLong) / (eastLong - westLong)).toFloat() * size.width
                        drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, size.height), strokeWidth = 1f)
                    }

                    // Base Turkey Outline Points
                    val turkeyCoastline = listOf(
                        Triple(41.67, 26.55, "Edirne"),
                        Triple(41.01, 28.97, "Istanbul"),
                        Triple(41.80, 31.50, "Zonguldak coast"),
                        Triple(42.02, 35.15, "Sinop Outer Tip"),
                        Triple(41.30, 36.33, "Samsun Gulf"),
                        Triple(41.00, 39.71, "Trabzon Harbor"),
                        Triple(41.02, 40.52, "Rize Coastal Edge"),
                        Triple(41.35, 41.50, "Hopa Border"),
                        Triple(41.25, 43.10, "Ardahan Border"),
                        Triple(39.95, 44.10, "Kars Border"),
                        Triple(37.30, 44.80, "Hakkari Corner"),
                        Triple(37.32, 42.10, "Mardin Border"),
                        Triple(36.18, 36.50, "Hatay Enclave"),
                        Triple(36.85, 34.63, "Mersin Coast"),
                        Triple(36.90, 30.70, "Antalya Gulf"),
                        Triple(36.20, 29.50, "Kas Tip"),
                        Triple(37.03, 27.43, "Bodrum Tip"),
                        Triple(38.42, 27.14, "Izmir Bay"),
                        Triple(40.15, 26.40, "Canakkale"),
                        Triple(41.00, 27.50, "Tekirdag")
                    )

                    val outlinePath = Path().apply {
                        turkeyCoastline.forEachIndexed { i, pt ->
                            val px = ((pt.second - westLong) / (eastLong - westLong)).toFloat() * size.width
                            val py = ((northLat - pt.first) / (northLat - southLat)).toFloat() * size.height
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }

                    drawPath(path = outlinePath, color = Color(0xFF1E293B).copy(alpha = 0.4f))
                    drawPath(path = outlinePath, color = Color(0xFF475569), style = Stroke(width = 2.5f))

                    // Draw the user city center locator pulsing zone
                    val centerPx = ((currentCenterCoords.second - westLong) / (eastLong - westLong)).toFloat() * size.width
                    val centerPy = ((northLat - currentCenterCoords.first) / (northLat - southLat)).toFloat() * size.height

                    // Pulsing scanning echo circle representing searchRadiusKm
                    // searchRadiusKm scale: 1 degree Lng ~ 85km -> convert to pixel range on map
                    val kmPerDegree = 98.0 // Avg degree weight
                    val radiusInDegrees = searchRadiusKm / kmPerDegree
                    val radiusInPixels = (radiusInDegrees / (eastLong - westLong)) * size.width

                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.08f),
                        radius = radiusInPixels.toFloat(),
                        center = Offset(centerPx, centerPy)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.25f),
                        radius = radiusInPixels.toFloat(),
                        center = Offset(centerPx, centerPy),
                        style = Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
                    )

                    // User Locator Node
                    drawCircle(
                        color = Color(0xFF10B981).copy(alpha = pulseAlpha),
                        radius = pulseRadius,
                        center = Offset(centerPx, centerPy),
                        style = Stroke(width = 2f)
                    )
                    drawCircle(
                        color = Color(0xFF10B981),
                        radius = 8f,
                        center = Offset(centerPx, centerPy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3f,
                        center = Offset(centerPx, centerPy)
                    )

                    // Draw Leads
                    leadsWithDistance.forEach { (lead, _) ->
                        val px = ((lead.longitude - westLong) / (eastLong - westLong)).toFloat() * size.width
                        val py = ((northLat - lead.latitude) / (northLat - southLat)).toFloat() * size.height

                        val isLeadSelected = selectedLead?.id == lead.id
                        val nodeColor = if (lead.leadScorePercent >= 80) Color(0xFFEC4899) else Color(0xFF38BDF8)

                        if (isLeadSelected) {
                            drawCircle(
                                color = nodeColor.copy(alpha = 0.2f),
                                radius = 24f,
                                center = Offset(px, py)
                            )
                            drawCircle(
                                color = nodeColor,
                                radius = 10f,
                                center = Offset(px, py),
                                style = Stroke(width = 2f)
                            )
                        }

                        drawCircle(
                            color = Color(0xFF0F172A),
                            radius = 7f,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = nodeColor,
                            radius = 5f,
                            center = Offset(px, py)
                        )
                    }
                }

                // Floating map indicators info card
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(Translations.get("radar_indicator", lang), fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                            Text("${Translations.get("your_location", lang)} ($userCityCenter)", fontSize = 9.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEC4899)))
                            Text("${Translations.get("high_yield_prospect", lang)} (>=80%)", fontSize = 9.sp, color = Color.White)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF38BDF8)))
                            Text("${Translations.get("standard_b2b_prospect", lang)} (<80%)", fontSize = 9.sp, color = Color.White)
                        }
                    }
                }
            }

            // HORIZONTAL "JOBS AROUND ME" SCROLLABLE LIST
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${Translations.get("prospects_around_me", lang)} (${leadsWithDistance.size} ${Translations.get("active_jobs", lang)})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (leadsWithDistance.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .background(Color(0xFF1E293B).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = Translations.get("no_prospects_around", lang),
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(leadsWithDistance) { (lead, distance) ->
                            val isSelected = selectedLead?.id == lead.id
                            Card(
                                onClick = { viewModel.selectLead(lead) },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF1E293B).copy(alpha = 0.5f)
                                ),
                                border = BorderStroke(
                                    width = 1.2.dp,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.05f)
                                ),
                                modifier = Modifier
                                    .width(180.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = lead.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(if (lead.leadScorePercent >= 80) Color(0xFFFCE7F3) else Color(0xFFE0F2FE))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${lead.leadScorePercent}%",
                                                color = if (lead.leadScorePercent >= 80) Color(0xFFDB2777) else Color(0xFF0369A1),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Place,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", distance)} ${Translations.get("km_away", lang)}",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp
                                        )
                                    }

                                    Text(
                                        text = lead.analysisNotes.ifEmpty { Translations.get("awaiting_analysis", lang) },
                                        color = Color.LightGray.copy(alpha = 0.8f),
                                        fontSize = 9.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // POP-UP DETAILED PROSPECT SUMMARY CONTROL BAR
            AnimatedVisibility(
                visible = selectedLead != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                selectedLead?.let { lead ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        border = BorderStroke(1.dp, if (lead.leadScorePercent >= 80) Color(0xFFEC4899) else Color(0xFF38BDF8))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (lead.leadScorePercent >= 80) Icons.Default.Star else Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (lead.leadScorePercent >= 80) Color(0xFFEC4899) else Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(lead.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        Text(
                                            text = "${lead.city} • ${Translations.get("kvkk_title", lang).take(4)} tracker: [${String.format(java.util.Locale.US, "%.3f", lead.latitude)}, ${String.format(java.util.Locale.US, "%.3f", lead.longitude)}]",
                                            fontSize = 10.sp,
                                            color = Color.LightGray.copy(alpha = 0.6f)
                                        )
                                    }
                                }

                                Surface(
                                    color = if (lead.leadScorePercent >= 80) Color(0x33EC4899) else Color(0x3338BDF8),
                                    shape = RoundedCornerShape(50),
                                    border = BorderStroke(1.dp, if (lead.leadScorePercent >= 80) Color(0xFFEC4899) else Color(0xFF38BDF8))
                                ) {
                                    Text(
                                        text = "%${lead.leadScorePercent} ${Translations.get("yield", lang)}",
                                        color = if (lead.leadScorePercent >= 80) Color(0xFFF472B6) else Color(0xFF38BDF8),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            Text(
                                text = lead.analysisNotes,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.simulateCustomerReply(lead) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(Translations.get("simulate_reply", lang), fontSize = 11.sp, color = Color.White)
                                }

                                Button(
                                    onClick = { viewModel.sendManualEmail(lead, "Quantum B2B Growth Strategy for ${lead.name}", "Hi there,\n\nWe saw you have no advertising active in ${lead.city} and we can help!") },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (lead.leadScorePercent >= 80) Color(0xFFEC4899) else Color(0xFF38BDF8)
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("map_proposal_button")
                                ) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(Translations.get("send_proposal", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
