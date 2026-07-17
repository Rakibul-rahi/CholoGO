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
private val BlueAccent = Color(0xFF60A5FA)
private val LimeAccent = Color(0xFFC6F135)
private val TextHigh = Color(0xFFF1F5F9)
private val TextMed = Color(0xFF8B96A5)
private val BorderBlue = Color(0x3360A5FA)
private val DotInactive = Color.White.copy(alpha = 0.12f)

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .height(76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(BgStart, BgEnd)
                )
            )
            .border(
                width = 1.dp,
                color = BorderBlue,
                shape = RoundedCornerShape(18.dp)
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth()
        ) { page ->

            val ad = ads[page]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.companyUrl))
                        context.startActivity(intent)
                    }
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(BlueAccent.copy(alpha = 0.12f))
                        .border(
                            width = 1.dp,
                            color = BlueAccent.copy(alpha = 0.20f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = ad.imageRes),
                        contentDescription = ad.companyName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = ad.title,
                        color = TextHigh,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = ad.description,
                        color = TextMed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sponsored by ${ad.companyName}",
                        color = LimeAccent.copy(alpha = 0.85f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

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