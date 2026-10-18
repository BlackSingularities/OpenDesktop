package com.opendesktop.launcher.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.opendesktop.launcher.BuildConfig
import com.opendesktop.launcher.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val color: Color,
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onConnectDrive: () -> Unit,
) {
    val pages = remember {
        listOf(
            OnboardingPage(
                icon = Icons.Rounded.Home,
                title = "Witaj w OpenDesktop",
                description = "Otwarty launcher na Androida z pełną personalizacją, 20 stylami szuflady i integracją z Google Drive.",
                color = Color(0xFF8AB4FF),
            ),
            OnboardingPage(
                icon = Icons.Rounded.Widgets,
                title = "Pulpit Twoich Marzeń",
                description = "Przeciągaj ikony, twórz foldery, dodawaj widżety. Większe ikony wypełniają ekran. Dostosuj rozmiar, kształt i animacje.",
                color = Color(0xFFFF9D6C),
            ),
            OnboardingPage(
                icon = Icons.Rounded.GridView,
                title = "20 Stylów Szuflady",
                description = "Wybierz spośród klasycznych siatek, list, kafelków lub efektów 3D: Kula, Helix, Cylinder, Wave i więcej!",
                color = Color(0xFF9C6CFF),
            ),
            OnboardingPage(
                icon = Icons.Rounded.Palette,
                title = "Efekty i Animacje",
                description = "Cząsteczki w tle, ripple przy dotknięciu, sparkle przy starcie. Włącz to co lubisz w ustawieniach.",
                color = Color(0xFFFF6C9D),
            ),
            OnboardingPage(
                icon = Icons.Rounded.Cloud,
                title = "Google Drive Backup",
                description = "Połącz konto Google i synchronizuj układ między urządzeniami. Auto-backup co 5 minut. Twoje dane, Twoja chmura.",
                color = Color(0xFF6CCCFF),
            ),
        )
    }

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E1A))
    ) {
        // Gradient tło
        Canvas(Modifier.fillMaxSize()) {
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1F3A),
                        Color(0xFF0A0E1A),
                        Color(0xFF0A0E1A),
                    ),
                ),
            )
        }

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            // Logo i wersja
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.opendesktop_mark),
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "OpenDesktop",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Wersja ${BuildConfig.VERSION_NAME}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )
                }
            }

            // Pager z stronami
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPageContent(pages[page])
            }

            // Wskaźniki (dots)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = spring(),
                        label = "dot",
                    )
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) pages[index].color
                                else Color.White.copy(alpha = 0.3f)
                            ),
                    )
                }
            }

            // Przyciski
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage == pages.lastIndex,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Przycisk główny - Połącz Drive lub Rozpocznij
                        Button(
                            onClick = {
                                if (pagerState.currentPage == pages.lastIndex) {
                                    onConnectDrive()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pages[pagerState.currentPage].color,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(Icons.Rounded.Cloud, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Połącz Google Drive",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Przycisk pomijający
                        TextButton(
                            onClick = onComplete,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                "Pomiń, użyj lokalnie",
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = pagerState.currentPage < pages.lastIndex,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Pomiń
                        OutlinedButton(
                            onClick = onComplete,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text("Pomiń", color = Color.White)
                        }

                        // Dalej
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = pages[pagerState.currentPage].color,
                            ),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                "Dalej",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.ArrowForward, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Ikona z tłem
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            page.color.copy(alpha = 0.4f),
                            page.color.copy(alpha = 0.1f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                page.icon,
                null,
                tint = page.color,
                modifier = Modifier.size(64.dp),
            )
        }

        Spacer(Modifier.height(40.dp))

        // Tytuł
        Text(
            page.title,
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // Opis
        Text(
            page.description,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp,
        )
    }
}
