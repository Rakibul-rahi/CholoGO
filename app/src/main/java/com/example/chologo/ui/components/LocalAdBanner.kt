package com.example.chologo.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.example.chologo.R
import kotlinx.coroutines.delay
import kotlin.random.Random

private val BannerBg = Color(0xFF111827)
private val OverlayDark = Color(0xB3000000)
private val GreenPrimary = Color(0xFF8DC63F)
private val TextPrimary = Color(0xFFE6EDF3)
private val TextSecondary = Color(0xFFD1D5DB)
private val ChipBg = Color(0x66243141)
private val DotInactive = Color(0x66FFFFFF)

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
            description = "Unique gifts, cute finds, and memorable picks for every occasion.",
            imageRes = R.drawable.coytoy_ad,
            companyUrl = "https://www.facebook.com/coytoybangladesh/"
        ),
        LocalAd(
            companyName = "Sayora",
            title = "🛍️ Where every gift holds a memory",
            description = "Jewelry, souvenir, fashion, and home. Find us @sayemanheritage & @sayemanresort",
            imageRes = R.drawable.sayora_ad,
            companyUrl = "https://www.facebook.com/profile.php?id=61576789233032/"
        ),
        LocalAd(
            companyName = "Defne",
            title = "Sunglasses & Eyewear Store",
            description = "Frame Your Style. Define Your Look",
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = BannerBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val ad = ads[page]

            fun openCompanyLink() {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ad.companyUrl))
                context.startActivity(intent)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { openCompanyLink() }
            ) {
                Image(
                    painter = painterResource(id = ad.imageRes),
                    contentDescription = ad.companyName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    OverlayDark,
                                    Color(0x99000000),
                                    Color(0x55000000)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            color = ChipBg
                        ) {
                            Text(
                                text = ad.companyName,
                                color = GreenPrimary,
                                fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = ad.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = ad.description,
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ads.forEachIndexed { index, _ ->
                            Box(
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(if (index == pagerState.currentPage) 10.dp else 7.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (index == pagerState.currentPage) GreenPrimary else DotInactive
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}