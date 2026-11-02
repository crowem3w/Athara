package org.example.authentication.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import org.example.authentication.R
import org.example.authentication.data.ImageDecoding
import org.example.authentication.domain.PersonalSetup

private const val AboutMaxLength = 150

/** Cover banner behind the profile picture. */
private val CoverShape = RoundedCornerShape(20.dp)
private val CoverMaxWidth = 340.dp
private val AvatarSize = 112.dp
private val AvatarInset = 16.dp
private val AvatarRing = 5.dp
private val BadgeSize = 34.dp
private val CoverFill = Color(0xFFD8DBE0)
private val CoverHint = Color(0xFF65676B)
private val PlaceholderFill = Color(0xFFE4E6EB)
private val BadgeFill = Color(0xFFE4E6EB)
private val BadgeIcon = Color(0xFF050505)

/**
 * Fields of the personal account setup step: profile picture, about, location, then Continue and
 * Skip for now. Everything is optional. Meant to be the content of [AccountSetupScreen].
 */
@Composable
fun ColumnScope.PersonalSetupFields(
    onContinue: (PersonalSetup) -> Unit,
    onSkip: () -> Unit,
) {
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var coverUri by rememberSaveable { mutableStateOf<String?>(null) }
    var about by rememberSaveable { mutableStateOf("") }
    var location by rememberSaveable { mutableStateOf("") }

    // Which picture the gallery / camera result is for
    var target by rememberSaveable { mutableStateOf(PhotoTarget.Avatar) }
    fun setPicture(uri: String?) {
        when (target) {
            PhotoTarget.Avatar -> photoUri = uri
            PhotoTarget.Cover -> coverUri = uri
        }
    }

    // Picked / photographed image waiting to be cropped
    var cropUri by rememberSaveable { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) cropUri = uri.toString()
    }
    val context = LocalContext.current
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { taken ->
        if (taken) cropUri = cameraUri
    }
    cropUri?.let { uri ->
        PhotoCropDialog(
            uri = Uri.parse(uri),
            aspect = if (target == PhotoTarget.Avatar) 1f else 2f,
            circle = target == PhotoTarget.Avatar,
            onDone = { cropped -> setPicture(cropped); cropUri = null },
            onCancel = { cropUri = null },
        )
    }
    val photo = rememberPhoto(photoUri, 600)
    val cover = rememberPhoto(coverUri, 1000)

    SetupLabel(R.string.authentication_setup_photo)
    ProfilePictureFrame(
        photo = photo,
        cover = cover,
        onGallery = { picked ->
            target = picked
            picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onCamera = { picked ->
            target = picked
            // The camera app writes into a file in our cache, shared through the FileProvider.
            runCatching {
                val dir = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File.createTempFile("photo_", ".jpg", dir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.authentication.fileprovider", file)
                cameraUri = uri.toString()
                camera.launch(uri)
            }
        },
        onRemove = { picked ->
            when (picked) {
                PhotoTarget.Avatar -> photoUri = null
                PhotoTarget.Cover -> coverUri = null
            }
        },
        modifier = Modifier.align(Alignment.CenterHorizontally),
    )

    // About
    SetupLabel(R.string.authentication_setup_about)
    CompactTextField(
        value = about,
        onValueChange = { about = it.take(AboutMaxLength) },
        placeholder = stringResource(R.string.authentication_setup_about_hint),
        height = 104.dp,
        singleLine = false,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
    )
    Text(
        text = "${about.length}/$AboutMaxLength",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.align(Alignment.End).padding(end = 4.dp),
    )

    // Location
    SetupLabel(R.string.authentication_setup_location)
    CompactTextField(
        value = location,
        onValueChange = { location = it },
        placeholder = stringResource(R.string.authentication_setup_location_hint),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
    )

    Button(
        onClick = { onContinue(PersonalSetup(photoUri, coverUri, about.trim(), location.trim())) },
        modifier = Modifier.fillMaxWidth().height(ButtonHeight),
    ) { Text(stringResource(R.string.authentication_setup_continue)) }

    TextButton(
        onClick = onSkip,
        modifier = Modifier.align(Alignment.CenterHorizontally),
    ) { Text(stringResource(R.string.authentication_setup_skip)) }
}

/** Which of the two pictures a gallery / camera / remove action is for. */
private enum class PhotoTarget { Avatar, Cover }

/**
 * Facebook-style profile header. A rounded 2:1 cover banner (flat light gray until a cover photo is
 * chosen), inset a little from the panel edges, with the circular avatar on its bottom-left edge,
 * half on the cover and half below it. The avatar has a thick ring in the panel color and a camera
 * badge on its bottom-right. Tapping the cover or the avatar (or its badge) opens a menu: choose
 * from gallery, take a photo and, when one is set, remove it.
 */
@Composable
private fun ProfilePictureFrame(
    photo: ImageBitmap?,
    cover: ImageBitmap?,
    onGallery: (PhotoTarget) -> Unit,
    onCamera: (PhotoTarget) -> Unit,
    onRemove: (PhotoTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val panel = MaterialTheme.colorScheme.background
    val description = stringResource(R.string.authentication_setup_photo)
    val coverDescription = stringResource(R.string.authentication_setup_cover)
    var avatarMenu by rememberSaveable { mutableStateOf(false) }
    var coverMenu by rememberSaveable { mutableStateOf(false) }

    Box(
        modifier = modifier
            .padding(horizontal = 12.dp)
            .padding(bottom = AvatarSize / 2) // room for the half of the avatar that hangs below the cover
            .widthIn(max = CoverMaxWidth)
            .fillMaxWidth(),
    ) {
        // Cover banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .clip(CoverShape)
                .background(CoverFill)
                .clickable { coverMenu = true },
        ) {
            if (cover != null) {
                Image(
                    bitmap = cover,
                    contentDescription = coverDescription,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = stringResource(R.string.authentication_setup_cover_hint),
                    style = MaterialTheme.typography.labelMedium,
                    color = CoverHint,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            PhotoMenu(
                expanded = coverMenu,
                onDismiss = { coverMenu = false },
                removeLabel = R.string.authentication_setup_cover_remove,
                hasPicture = cover != null,
                onGallery = { onGallery(PhotoTarget.Cover) },
                onCamera = { onCamera(PhotoTarget.Cover) },
                onRemove = { onRemove(PhotoTarget.Cover) },
            )
        }

        // Avatar: bottom-left of the cover, half on it and half below
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = AvatarInset, y = AvatarSize / 2)
                .size(AvatarSize),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().clip(CircleShape).clickable { avatarMenu = true },
                shape = CircleShape,
                color = PlaceholderFill,
                border = BorderStroke(AvatarRing, panel),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (photo != null) {
                        Image(
                            bitmap = photo,
                            contentDescription = description,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                        )
                    } else {
                        Image(
                            painter = painterResource(R.drawable.authentication_ic_personal),
                            contentDescription = description,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            }
            // Camera badge on the circle's bottom-right edge
            Box(
                modifier = Modifier
                    .size(BadgeSize)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(BadgeFill)
                    .border(2.dp, panel, CircleShape)
                    .clickable { avatarMenu = true },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(18.dp)) {
                    val w = size.width
                    val h = size.height
                    drawRoundRect(BadgeIcon, Offset(w * 0.3f, 0f), Size(w * 0.4f, h * 0.3f), CornerRadius(w * 0.08f))
                    drawRoundRect(BadgeIcon, Offset(0f, h * 0.2f), Size(w, h * 0.8f), CornerRadius(w * 0.18f))
                    drawCircle(BadgeFill, radius = w * 0.2f, center = Offset(w / 2, h * 0.6f))
                }
            }
            PhotoMenu(
                expanded = avatarMenu,
                onDismiss = { avatarMenu = false },
                removeLabel = R.string.authentication_setup_photo_remove,
                hasPicture = photo != null,
                onGallery = { onGallery(PhotoTarget.Avatar) },
                onCamera = { onCamera(PhotoTarget.Avatar) },
                onRemove = { onRemove(PhotoTarget.Avatar) },
            )
        }
    }
}

/** Gallery / camera menu, plus a remove entry while a picture is set. */
@Composable
private fun PhotoMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    removeLabel: Int,
    hasPicture: Boolean,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onRemove: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.authentication_setup_photo_gallery)) },
            onClick = { onDismiss(); onGallery() },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.authentication_setup_photo_camera)) },
            onClick = { onDismiss(); onCamera() },
        )
        if (hasPicture) {
            DropdownMenuItem(
                text = { Text(stringResource(removeLabel)) },
                onClick = { onDismiss(); onRemove() },
            )
        }
    }
}

@Composable
private fun SetupLabel(label: Int) {
    Text(
        text = stringResource(label),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/** Decodes the picked or photographed image off the main thread, downsampled and rotated upright. */
@Composable
private fun rememberPhoto(uri: String?, maxSide: Int): ImageBitmap? {
    val resolver = LocalContext.current.contentResolver
    val photo by produceState<ImageBitmap?>(initialValue = null, uri, maxSide) {
        value = if (uri == null) null else withContext(Dispatchers.IO) {
            runCatching { ImageDecoding.decode(resolver, Uri.parse(uri), maxSide)?.asImageBitmap() }.getOrNull()
        }
    }
    return photo
}
