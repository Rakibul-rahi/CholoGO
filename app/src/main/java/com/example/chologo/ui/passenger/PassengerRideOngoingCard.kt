package com.example.chologo.ui.passenger

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.data.model.RideNowRequest
import com.example.chologo.data.model.VehicleType

/**
 * What a passenger sees while they are actually sitting in the vehicle.
 *
 * Rebuilt from a plain default-Material3 card that sat in the middle of an
 * otherwise dark, lime-accented app - light surface, generic typography,
 * "Rider: X" / "Phone: Y" label-prefixed lines, and a hardcoded bicycle
 * icon regardless of what the rider actually drives.
 *
 * More than a restyle, though. The old card left out the one thing this
 * screen exists to answer mid-trip - which vehicle is this, and how do I
 * reach the person driving it - even though the request has carried a
 * vehicle snapshot since matching. So the plate and model are now the
 * card's most prominent element after the route, and calling the rider is
 * a filled primary button rather than an outlined afterthought: if
 * something is wrong at 11 PM, that is the button being reached for.
 */
@Composable
fun PassengerRideOngoingCard(
    request: RideNowRequest,
    onCallRider: () -> Unit = {}
) {
    PassengerSectionCard(
        title = "Trip in Progress",
        subtitle = "You're on your way. Your rider will mark the trip complete on arrival.",
        icon = VehicleType.emoji(request.matchedVehicleType)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LivePulseBadge()
            MiniBadge(text = request.tripTime.ifBlank { "Now" }, accent = AccentBlue)
        }

        Spacer(modifier = Modifier.height(16.dp))

        ActiveRouteLine(
            pickup = request.pickup,
            destination = request.destination
        )

        Spacer(modifier = Modifier.height(16.dp))

        OngoingVehiclePanel(
            vehicleType = request.matchedVehicleType,
            vehicleModel = request.matchedVehicleModel,
            vehicleNumber = request.matchedVehicleNumber,
            vehicleColor = request.matchedVehicleColor
        )

        Spacer(modifier = Modifier.height(12.dp))

        RideMetaRow(
            Icons.Default.Person,
            request.matchedRiderName.ifBlank { "Your rider" }
        )

        rememberRiderStats(request.matchedRiderId)?.let { stats ->
            Spacer(modifier = Modifier.height(8.dp))
            RideMetaRow(Icons.Default.Star, riderStatsLabel(stats))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onCallRider,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Lime,
                contentColor = Color.Black
            )
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call Rider", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * The vehicle, given the space it deserves.
 *
 * A number plate is read at a glance and often in bad light, so it gets
 * its own line at the largest size on the card. Everything else about the
 * vehicle - type, model, colour - is supporting detail underneath.
 *
 * Legacy matches carry no vehicle snapshot at all; VehicleType reads those
 * as a bike, which is what every rider was before cars existed, so this
 * degrades to a plain "Bike" rather than an empty panel.
 *
 * Takes loose strings rather than a request object so the Tomorrow tab's
 * ongoing card can show the identical panel - it is the same question at
 * the same moment, and the two should not look like different apps.
 */
@Composable
fun OngoingVehiclePanel(
    vehicleType: String,
    vehicleModel: String,
    vehicleNumber: String,
    vehicleColor: String
) {
    val isCar = VehicleType.isCar(vehicleType)
    val plate = vehicleNumber.trim()

    val descriptor = listOf(vehicleColor.trim(), vehicleModel.trim())
        .filter { it.isNotEmpty() }
        .joinToString(" ")

    // Whatever identifies the vehicle best goes on top, and the second
    // line is dropped rather than repeating it: a rider who filled in
    // nothing should read "Bike", not "Bike" twice.
    val headline = plate.ifBlank { descriptor.ifBlank { VehicleType.label(vehicleType) } }
    val subline = if (plate.isNotBlank()) descriptor else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Lime.copy(alpha = 0.06f))
            .border(1.dp, Lime.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isCar) Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
            contentDescription = null,
            tint = Lime,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = headline,
                color = TextHigh,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (subline.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = subline,
                    color = TextMed,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A slow breathing dot, so the card reads as *currently happening* rather
 * than as one more static status panel. Deliberately gentle - this sits on
 * screen for the length of a trip, and anything sharper would nag.
 */
@Composable
fun LivePulseBadge() {
    val transition = rememberInfiniteTransition(label = "ongoing_pulse")

    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ongoing_pulse_alpha"
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentEmerald.copy(alpha = 0.12f))
            .border(1.dp, AccentEmerald.copy(alpha = 0.22f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .alpha(pulse)
                .clip(CircleShape)
                .background(AccentEmerald)
        )

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = "On the move",
            color = AccentEmerald,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
