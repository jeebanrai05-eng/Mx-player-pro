package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.VideoItem
import com.example.ui.theme.ProBlue
import com.example.ui.theme.ProBlueDark

data class OnlineFeaturedItem(
    val title: String,
    val subtitle: String,
    val posterUrl: String,
    val streamVideo: VideoItem
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreOnlineSheet(
    featuredVideos: List<VideoItem>,
    onPlayVideo: (VideoItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val onlineCatalog = listOf(
        OnlineFeaturedItem(
            title = "KGF Chapter 2",
            subtitle = "Action / Blockbuster",
            posterUrl = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=400&q=80",
            streamVideo = featuredVideos.getOrElse(0) {
                VideoItem(
                    id = -201L,
                    title = "KGF Chapter 2 (Official Clip)",
                    displayName = "kgf_chapter_2.mp4",
                    contentUriString = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4",
                    duration = 30000L,
                    size = 22718509L,
                    width = 1920,
                    height = 1080,
                    dateAdded = System.currentTimeMillis() / 1000,
                    folderName = "Explore Online",
                    path = "https://raw.githubusercontent.com/bower-media-samples/big-buck-bunny-1080p-30s/master/video.mp4",
                    mimeType = "video/mp4"
                )
            }
        ),
        OnlineFeaturedItem(
            title = "Bhediya",
            subtitle = "Creature Comedy",
            posterUrl = "https://images.unsplash.com/photo-1518676590629-3dcbd9c5a5c9?w=400&q=80",
            streamVideo = featuredVideos.getOrElse(1) {
                VideoItem(
                    id = -202L,
                    title = "Bhediya (Theatrical Trailer)",
                    displayName = "bhediya_trailer.mp4",
                    contentUriString = "https://vjs.zencdn.net/v/oceans.mp4",
                    duration = 46000L,
                    size = 23014356L,
                    width = 1280,
                    height = 720,
                    dateAdded = System.currentTimeMillis() / 1000,
                    folderName = "Explore Online",
                    path = "https://vjs.zencdn.net/v/oceans.mp4",
                    mimeType = "video/mp4"
                )
            }
        ),
        OnlineFeaturedItem(
            title = "Pathaan",
            subtitle = "Spy Universe Action",
            posterUrl = "https://images.unsplash.com/photo-1574375927938-d5a98e8ffe85?w=400&q=80",
            streamVideo = featuredVideos.getOrElse(2) {
                VideoItem(
                    id = -203L,
                    title = "Pathaan (Action Sequence)",
                    displayName = "pathaan_action.mp4",
                    contentUriString = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/face-demographics-walking.mp4",
                    duration = 20000L,
                    size = 6406124L,
                    width = 1920,
                    height = 1080,
                    dateAdded = System.currentTimeMillis() / 1000,
                    folderName = "Explore Online",
                    path = "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/face-demographics-walking.mp4",
                    mimeType = "video/mp4"
                )
            }
        ),
        OnlineFeaturedItem(
            title = "Shivaay",
            subtitle = "Himalayan Thriller",
            posterUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=400&q=80",
            streamVideo = featuredVideos.getOrElse(3) {
                VideoItem(
                    id = -204L,
                    title = "Shivaay (Action Scene)",
                    displayName = "shivaay.mp4",
                    contentUriString = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                    duration = 5000L,
                    size = 1128375L,
                    width = 1280,
                    height = 720,
                    dateAdded = System.currentTimeMillis() / 1000,
                    folderName = "Explore Online",
                    path = "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4",
                    mimeType = "video/mp4"
                )
            }
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F1E36), // Sleek deep sapphire matching reference
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cloud Play Icon with Glowing Halo
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0070F3)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(54.dp)
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Explore Online",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Never run out of new videos to watch.\nTap Video & enjoy the best of Ultra Player!",
                fontSize = 13.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Movie / Video Poster Cards Row
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(onlineCatalog) { item ->
                    Card(
                        modifier = Modifier
                            .width(108.dp)
                            .clickable {
                                onDismiss()
                                onPlayVideo(item.streamVideo)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.68f)
                                    .background(Color(0xFF334155))
                            ) {
                                AsyncImage(
                                    model = item.posterUrl,
                                    contentDescription = item.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.matchParentSize()
                                )

                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, Color(0xCC000000))
                                            )
                                        )
                                )

                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(6.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Glowing Blue Video Action Pill Button (Exact match from reference image)
            Button(
                onClick = {
                    onDismiss()
                    val videoToPlay = onlineCatalog.firstOrNull()?.streamVideo ?: featuredVideos.first()
                    onPlayVideo(videoToPlay)
                },
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ProBlue
                ),
                modifier = Modifier
                    .width(180.dp)
                    .height(52.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ProBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "Video",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
