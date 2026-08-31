package com.example.chologo.ui.components


import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chologo.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BgStart = Color(0xFF1A1030)
private val BgEnd = Color(0xFF0D1520)
private val FooterBg = Color(0xFF12161C)
private val BlueAccent = Color(0xFF60A5FA)
private val LimeAccent = Color(0xFFC6F135)
private val TextHigh = Color(0xFFF1F5F9)
private val TextMed = Color(0xFF8B96A5)
private val BorderBlue = Color(0x3360A5FA)
private val DotInactive = Color.White.copy(alpha = 0.18f)

data class LocalAd(
    val companyName: String,
    val title: String,
    val description: String,
    val imageRes: Int,
    val companyUrl: String
)

object LocalAds {
    val allAds = listOf(
        LocalAd(
            companyName = "CoyToy Bangladesh",
            title = "Where every gift holds a memory",
            description = "Unique gifts, cute finds, and memorable picks.",
            imageRes = R.drawable.coytoy_ad,
            companyUrl = "https://www.facebook.com/coytoybangladesh/"
        ),
        LocalAd(
            companyName = "Sayora",
            title = "Jewelry, souvenir & fashion",
            description = "Find us at Sayeman Heritage and Sayeman Resort.",
            imageRes = R.drawable.sayora_ad,
            companyUrl = "https://www.facebook.com/profile.php?id=61576789233032/"
        ),
        LocalAd(
            companyName = "Defne",
            title = "Frame Your Style",
            description = "Sunglasses and eyewear for your everyday look.",
            imageRes = R.drawable.defne_ad,
            companyUrl = "https://www.facebook.com/defne.fa.co/"
        )
    )
}

@Composable
fun LocalAdCarouselBanner(
    modifier: Modifier = Modifier,
    autoSlideMillis: Long = 3500L
) {
    val context = LocalContext.current
    val ads = remember { LocalAds.allAds.shuffled() }
    val startPage = remember { if (ads.isNotEmpty()) Random.nextInt(ads.size) else 0 }

    val pagerState = rememberPagerState(
        initialPage = startPage,
        pageCount = { ads.size }
    )

    LaunchedEffect(ads.size) {
        if (ads.size > 1) {
            while (true) {
                delay(autoSlideMillis)
                val nextPage = (pagerState.currentPage + 1) % ads.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    if (ads.isEmpty()) return

    // A real hero card, not a squeezed-in strip: a tall image area (where a
    // wide banner like Sayora's actually has room to breathe, instead of
    // being crushed into a 38dp square) with its own solid caption footer
    // underneath, so text never has to fight the artwork for legibility.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(180.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BgStart, BgEnd)
                )
            )
            .border(
                width = 1.dp,
                color = BorderBlue,
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            val ad = ads[page]

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.companyUrl))
                        context.startActivity(intent)
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp)
                ) {
                    Image(
                        painter = painterResource(id = ad.imageRes),
                        contentDescription = ad.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Subtle scrim so the "Sponsored" tag stays legible over
                    // any artwork, without dimming the ad itself much.
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .align(Alignment.TopStart)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "SPONSORED",
                            color = LimeAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(FooterBg)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = ad.title,
                            color = TextHigh,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${ad.companyName} · ${ad.description}",
                            color = TextMed,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ads.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .then(
                                        if (index == pagerState.currentPage) {
                                            Modifier
                                                .width(14.dp)
                                                .height(5.dp)
                                        } else {
                                            Modifier.size(5.dp)
                                        }
                                    )
                                    .clip(
                                        if (index == pagerState.currentPage) {
                                            RoundedCornerShape(4.dp)
                                        } else {
                                            CircleShape
                                        }
                                    )
                                    .background(
                                        if (index == pagerState.currentPage) BlueAccent else DotInactive
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}