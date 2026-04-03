package com.example.chologo.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R

private val DarkCharcoal = Color(0xFF222831)
private val LimeGreen = Color(0xFF9BCB2D)
private val SoftWhite = Color(0xFFF5F5F5)

@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit = {},
    onSignupClick: () -> Unit = {}
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .padding(horizontal = 28.dp)
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Image(
                    painter = painterResource(id = R.drawable.chologologo),
                    contentDescription = "ChoLoGo Logo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Campus Ride Sharing",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkCharcoal
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Smart • Safe • Affordable",
                    fontSize = 14.sp,
                    color = DarkCharcoal.copy(alpha = 0.6f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 40.dp)
            ) {

                Button(
                    onClick = onLoginClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LimeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Login",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = onSignupClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(2.dp, DarkCharcoal)
                ) {
                    Text(
                        text = "Sign Up",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkCharcoal
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Choose rider or passenger after signup",
                    fontSize = 13.sp,
                    color = DarkCharcoal.copy(alpha = 0.55f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}