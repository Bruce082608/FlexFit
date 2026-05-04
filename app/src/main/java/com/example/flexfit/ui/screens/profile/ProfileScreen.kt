package com.example.flexfit.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.flexfit.data.model.BodyProportions
import com.example.flexfit.data.model.UserProfile
import com.example.flexfit.data.repository.AppPreferencesRepository
import com.example.flexfit.data.repository.BodyCalibrationRepository
import com.example.flexfit.data.repository.UserProfileRepository
import com.example.flexfit.data.repository.WorkoutRecordRepository
import com.example.flexfit.ml.BodyProportionAnalysis
import com.example.flexfit.ml.BodyProportionAnalyzer
import com.example.flexfit.ml.PoseDetectorCallback
import com.example.flexfit.ml.PoseDetectorWrapper
import com.example.flexfit.ui.i18n.AppLanguage
import com.example.flexfit.ui.i18n.LocalAppLanguage
import com.example.flexfit.ui.i18n.l10n
import com.example.flexfit.ui.i18n.localize
import com.example.flexfit.ui.theme.AccentPurple
import com.example.flexfit.ui.theme.DeepPurple
import com.example.flexfit.ui.theme.ErrorRed
import com.example.flexfit.ui.theme.LightBackground
import com.example.flexfit.ui.theme.LightPurple
import com.example.flexfit.ui.theme.SuccessGreen
import com.example.flexfit.ui.theme.TextPrimary
import com.example.flexfit.ui.theme.TextSecondary
import com.example.flexfit.ui.theme.TextTertiary
import com.example.flexfit.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onOpenProfileInfo: (ProfileInfoType) -> Unit,
    onOpenBodyCalibration: () -> Unit
) {
    val profile by UserProfileRepository.profile.collectAsState()
    val bodyProportions by BodyCalibrationRepository.bodyProportions.collectAsState()
    val totalWorkouts by WorkoutRecordRepository.totalWorkouts.collectAsState()
    val averageAccuracy by WorkoutRecordRepository.averageAccuracy.collectAsState()
    val currentStreak by WorkoutRecordRepository.currentStreak.collectAsState()
    val appLanguage = LocalAppLanguage.current

    var showBodyStatsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        ProfileHeader(
            profile = profile,
            totalWorkouts = totalWorkouts,
            currentStreak = currentStreak,
            averageAccuracy = averageAccuracy,
            onEditProfile = onEditProfile
        )

        Spacer(modifier = Modifier.height(24.dp))

        BodyStatsSection(
            height = profile.height,
            weight = profile.weight,
            onEdit = { showBodyStatsDialog = true }
        )

        Spacer(modifier = Modifier.height(24.dp))

        BodyCalibrationEntryCard(
            isCalibrated = bodyProportions?.isComplete == true,
            onClick = onOpenBodyCalibration
        )

        Spacer(modifier = Modifier.height(24.dp))

        FitnessGoalSection(
            currentGoal = profile.fitnessGoal,
            onGoalChange = UserProfileRepository::updateFitnessGoal
        )

        Spacer(modifier = Modifier.height(24.dp))

        SettingsSection(
            currentLanguage = appLanguage,
            onEditProfile = onEditProfile,
            onLanguage = { showLanguageDialog = true },
            onNotifications = { onOpenProfileInfo(ProfileInfoType.NOTIFICATIONS) },
            onDeviceSettings = { onOpenProfileInfo(ProfileInfoType.DEVICE_SETTINGS) },
            onFeedback = { onOpenProfileInfo(ProfileInfoType.FEEDBACK) }
        )

        Spacer(modifier = Modifier.height(24.dp))

        AboutSection(
            onAboutFlexFit = { onOpenProfileInfo(ProfileInfoType.ABOUT_FLEXFIT) },
            onRateApp = { onOpenProfileInfo(ProfileInfoType.RATE_APP) }
        )

        Spacer(modifier = Modifier.height(128.dp))
    }

    if (showBodyStatsDialog) {
        BodyStatsDialog(
            profile = profile,
            onDismiss = { showBodyStatsDialog = false },
            onSave = { height, weight ->
                UserProfileRepository.updateBodyStats(height, weight)
                showBodyStatsDialog = false
            }
        )
    }

    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = appLanguage,
            onDismiss = { showLanguageDialog = false },
            onSelect = { language ->
                AppPreferencesRepository.updateLanguage(language)
                showLanguageDialog = false
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    totalWorkouts: Int,
    currentStreak: Int,
    averageAccuracy: Float,
    onEditProfile: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DeepPurple, AccentPurple)
                )
            )
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onEditProfile,
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = l10n("Edit profile"),
                tint = Color.White
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AvatarCircle(
                name = profile.name,
                avatarStyle = profile.avatarStyle,
                avatarUri = profile.avatarUri,
                size = 100
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = profile.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat(value = "$totalWorkouts", label = l10n("Workouts"))
                ProfileStat(value = "$currentStreak", label = l10n("Day Streak"))
                ProfileStat(value = "${averageAccuracy.toInt()}%", label = l10n("Avg Score"))
            }
        }
    }
}

@Composable
private fun AvatarCircle(
    name: String,
    avatarStyle: Int,
    avatarUri: String?,
    size: Int
) {
    val palette = avatarPalette(avatarStyle)
    val imageBitmap = rememberAvatarBitmap(avatarUri)
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "F" }

    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = l10n("Profile avatar"),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = initials,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ProfileStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun BodyStatsSection(
    height: Float,
    weight: Float,
    onEdit: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = l10n("Body Stats"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = l10n("Edit body stats"),
                        tint = DeepPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onEdit() },
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatInputCard(
                    label = l10n("Height"),
                    value = height.formatNumber(),
                    unit = "cm",
                    modifier = Modifier.weight(1f)
                )
                StatInputCard(
                    label = l10n("Weight"),
                    value = weight.formatNumber(),
                    unit = "kg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatInputCard(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = LightPurple.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BodyCalibrationEntryCard(
    isCalibrated: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DeepPurple),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = l10n("Custom Body Data", "定制化身材数据"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isCalibrated) {
                        l10n("Body ratio profile is ready.", "身体比例系数已生成。")
                    } else {
                        l10n("Upload a front full-body photo before training.", "训练前上传正面全身照生成身体比例系数。")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.78f)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.82f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun FitnessGoalSection(
    currentGoal: String,
    onGoalChange: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = l10n("Fitness Goal"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            val goals = listOf("Build Strength", "Lose Weight", "Stay Fit", "Muscle Tone")
            goals.forEach { goal ->
                GoalOption(
                    goal = l10n(goal),
                    isSelected = currentGoal == goal,
                    onClick = { onGoalChange(goal) }
                )
            }
        }
    }
}

@Composable
private fun GoalOption(
    goal: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) DeepPurple.copy(alpha = 0.1f) else Color.Transparent)
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isSelected) DeepPurple else LightPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else DeepPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = goal,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) DeepPurple else TextPrimary
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(SuccessGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(
    currentLanguage: AppLanguage,
    onEditProfile: () -> Unit,
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onDeviceSettings: () -> Unit,
    onFeedback: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = l10n("Settings"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                icon = Icons.Default.Edit,
                title = l10n("Edit Profile"),
                onClick = onEditProfile
            )
            SettingsItem(
                icon = Icons.Default.Language,
                title = l10n("Language"),
                subtitle = currentLanguage.displayName,
                onClick = onLanguage
            )
            SettingsItem(
                icon = Icons.Default.Notifications,
                title = l10n("Notifications"),
                onClick = onNotifications
            )
            SettingsItem(
                icon = Icons.Default.Settings,
                title = l10n("Device Settings"),
                onClick = onDeviceSettings
            )
            SettingsItem(
                icon = Icons.Default.Mail,
                title = l10n("Feedback"),
                onClick = onFeedback
            )
        }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DeepPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LanguageDialog(
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit,
    onSelect: (AppLanguage) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l10n("Choose Language")) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(language) }
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLanguage == language,
                            onClick = { onSelect(language) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(l10n("Cancel"))
            }
        }
    )
}

@Composable
private fun AboutSection(
    onAboutFlexFit: () -> Unit,
    onRateApp: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = l10n("About"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            SettingsItem(
                icon = Icons.Default.Info,
                title = l10n("About FlexFit"),
                onClick = onAboutFlexFit
            )
            SettingsItem(
                icon = Icons.Default.Star,
                title = l10n("Rate App"),
                onClick = onRateApp
            )
        }
    }
}

@Composable
private fun BodyStatsDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSave: (Float, Float) -> Unit
) {
    var heightText by remember(profile.height) { mutableStateOf(profile.height.formatNumber()) }
    var weightText by remember(profile.weight) { mutableStateOf(profile.weight.formatNumber()) }

    val height = heightText.toFloatOrNull()
    val weight = weightText.toFloatOrNull()
    val heightValid = height != null && height in 80f..250f
    val weightValid = weight != null && weight in 25f..250f

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l10n("Edit Body Stats")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filterDecimalInput() },
                    label = { Text(l10n("Height")) },
                    suffix = { Text("cm") },
                    singleLine = true,
                    isError = heightText.isNotBlank() && !heightValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filterDecimalInput() },
                    label = { Text(l10n("Weight")) },
                    suffix = { Text("kg") },
                    singleLine = true,
                    isError = weightText.isNotBlank() && !weightValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (!heightValid || !weightValid) {
                    Text(
                        text = l10n("Enter height from 80-250 cm and weight from 25-250 kg."),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(height ?: profile.height, weight ?: profile.weight) },
                enabled = heightValid && weightValid,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Text(l10n("Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(l10n("Cancel"))
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onChooseAvatar: (((String) -> Unit) -> Unit),
    onSave: (String, String, Int, String?) -> Unit
) {
    var nameText by remember(profile.name) { mutableStateOf(profile.name) }
    var emailText by remember(profile.email) { mutableStateOf(profile.email) }
    var avatarStyle by remember(profile.avatarStyle) { mutableIntStateOf(profile.avatarStyle) }
    var avatarUri by remember(profile.avatarUri) { mutableStateOf(profile.avatarUri) }

    val nameValid = nameText.trim().length in 2..32
    val emailValid = emailText.trim().let { "@" in it && "." in it && it.length <= 80 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(l10n("Edit Profile")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AvatarCircle(
                        name = nameText,
                        avatarStyle = avatarStyle,
                        avatarUri = avatarUri,
                        size = 72
                    )
                }

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it.take(32) },
                    label = { Text(l10n("Nickname")) },
                    singleLine = true,
                    isError = nameText.isNotBlank() && !nameValid,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = emailText,
                    onValueChange = { emailText = it.take(80) },
                    label = { Text(l10n("Email")) },
                    singleLine = true,
                    isError = emailText.isNotBlank() && !emailValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = l10n("Avatar"),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onChooseAvatar { selectedUri -> avatarUri = selectedUri } },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) {
                        Text(l10n("Choose Photo"))
                    }
                    OutlinedButton(
                        onClick = { avatarUri = null },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(l10n("Remove"))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(UserProfileRepository.AVATAR_STYLE_COUNT) { index ->
                        AvatarChoice(
                            index = index,
                            selected = avatarStyle == index,
                            onClick = { avatarStyle = index },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (!nameValid || !emailValid) {
                    Text(
                        text = l10n("Use a nickname with 2-32 characters and a valid email address."),
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nameText, emailText, avatarStyle, avatarUri) },
                enabled = nameValid && emailValid,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Text(l10n("Save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(l10n("Cancel"))
            }
        }
    )
}

@Composable
private fun AvatarChoice(
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(avatarPalette(index)))
        )
        if (selected) {
            Spacer(modifier = Modifier.width(6.dp))
            Text(l10n("On"), color = DeepPurple)
        }
    }
}

@Composable
private fun AboutFlexFitDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = DeepPurple
            )
        },
        title = { Text(l10n("About FlexFit")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = l10n(
                        "FlexFit is an AI fitness coaching demo that uses your phone camera or local workout videos to analyze Pull Up and Shoulder Press form.",
                        "FlexFit 是一个 AI 健身指导演示应用，可使用手机相机或本地训练视频分析引体向上和肩上推举动作。"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = l10n(
                        "Real-time feedback is powered by on-device pose detection and local rule-based scoring for Depth, Alignment, and Stability.",
                        "实时反馈由端侧姿态检测和本地规则评分驱动，覆盖深度、对齐和稳定性。"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = l10n(
                        "After a workout, FlexFit can optionally use DeepSeek-compatible AI Analysis for personalized coaching. The local demo path still works without network access.",
                        "训练结束后，FlexFit 可选择使用兼容 DeepSeek 的 AI 分析生成个性化建议。本地演示流程在无网络时仍可使用。"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
            ) {
                Text(l10n("Done"))
            }
        }
    )
}

@Composable
private fun SimpleInfoDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                textAlign = TextAlign.Start
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(l10n("OK"))
            }
        }
    )
}

private data class InfoDialogState(
    val title: String,
    val message: String
)

enum class ProfileInfoType(
    val routeValue: String,
    val pageTitle: String,
    val body: String
) {
    NOTIFICATIONS(
        routeValue = "notifications",
        pageTitle = "Notifications",
        body = "Workout reminders are ready for the demo profile. Full scheduling can be connected later from Android system notification settings."
    ),
    DEVICE_SETTINGS(
        routeValue = "device_settings",
        pageTitle = "Device Settings",
        body = "FlexFit uses the phone camera, local video picker, on-device MediaPipe pose detection, and optional post-workout network access for AI Analysis."
    ),
    FEEDBACK(
        routeValue = "feedback",
        pageTitle = "Feedback",
        body = "Send demo feedback to the FlexFit team with the exercise name, device model, and what happened during the session."
    ),
    ABOUT_FLEXFIT(
        routeValue = "about_flexfit",
        pageTitle = "About FlexFit",
        body = "FlexFit is an AI fitness coaching demo that uses your phone camera or local workout videos to analyze Pull Up and Shoulder Press form.\n\nReal-time feedback is powered by on-device pose detection and local rule-based scoring for Depth, Alignment, and Stability.\n\nAfter a workout, FlexFit can optionally use DeepSeek-compatible AI Analysis for personalized coaching. The local demo path still works without network access."
    ),
    RATE_APP(
        routeValue = "rate_app",
        pageTitle = "Rate App",
        body = "Thanks for trying FlexFit. Store rating is not connected in the demo build yet, but this page is wired and ready for the release flow."
    );

    fun localizedTitle(language: AppLanguage): String {
        return language.localize(pageTitle)
    }

    fun localizedBody(language: AppLanguage): String {
        return when (this) {
            NOTIFICATIONS -> language.text(
                body,
                "演示资料的训练提醒已准备就绪。完整排程后续可接入 Android 系统通知设置。"
            )
            DEVICE_SETTINGS -> language.text(
                body,
                "FlexFit 会使用手机相机、本地视频选择器、端侧 MediaPipe 姿态检测，以及可选的训练后网络 AI 分析。"
            )
            FEEDBACK -> language.text(
                body,
                "向 FlexFit 团队发送演示反馈时，请附上动作名称、设备型号和训练过程中发生的情况。"
            )
            ABOUT_FLEXFIT -> language.text(
                body,
                "FlexFit 是一个 AI 健身指导演示应用，可使用手机相机或本地训练视频分析引体向上和肩上推举动作。\n\n实时反馈由端侧姿态检测和本地规则评分驱动，覆盖深度、对齐和稳定性。\n\n训练结束后，FlexFit 可选择使用兼容 DeepSeek 的 AI 分析生成个性化建议。本地演示流程在无网络时仍可使用。"
            )
            RATE_APP -> language.text(
                body,
                "感谢试用 FlexFit。演示版本暂未连接应用商店评分，但页面已经接好，可用于发布流程。"
            )
        }
    }

    companion object {
        fun fromRouteValue(value: String?): ProfileInfoType {
            return entries.firstOrNull { it.routeValue == value } ?: ABOUT_FLEXFIT
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun BodyCalibrationScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bodyProportions by BodyCalibrationRepository.bodyProportions.collectAsState()
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var calibrationSaved by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember(bodyProportions?.sourceUri) { mutableStateOf(bodyProportions?.sourceUri) }
    val preview = rememberAvatarBitmap(selectedPhotoUri)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            selectedPhotoUri = uri.toString()
            isProcessing = true
            statusMessage = null
            calibrationSaved = false
            scope.launch {
                val result = analyzeBodyCalibrationPhoto(context, uri)
                result
                    .onSuccess { proportions ->
                        BodyCalibrationRepository.save(proportions)
                        calibrationSaved = true
                    }
                    .onFailure { error ->
                        statusMessage = error.message ?: "Photo analysis failed."
                        calibrationSaved = false
                    }
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            ProfileTopBar(
                title = l10n("Custom Body Data", "定制化身材数据"),
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = LightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = l10n("Upload Front Full-body Photo", "上传正面全身照"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = l10n(
                            "FlexFit uses the standard-table rules to extract 22 body ratio coefficients for later form judgement.",
                            "FlexFit 会按照标准表规则提取 22 个身体比例系数，用于后续动作标准度判断。"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )

                    if (preview != null) {
                        Image(
                            bitmap = preview,
                            contentDescription = l10n("Selected body photo"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LightPurple.copy(alpha = 0.34f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = DeepPurple,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { photoPickerLauncher.launch(arrayOf("image/*")) },
                        enabled = !isProcessing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(l10n("Analyzing", "正在分析"))
                        } else {
                            Text(l10n("Choose Photo", "选择照片"))
                        }
                    }

                    if (calibrationSaved || statusMessage != null) {
                        Text(
                            text = if (calibrationSaved) {
                                l10n("Body ratio coefficients saved.", "身体比例系数已保存。")
                            } else {
                                statusMessage.orEmpty()
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (bodyProportions?.isComplete == true) SuccessGreen else ErrorRed
                        )
                    }
                }
            }

            CalibrationResultCard(bodyProportions = bodyProportions)
        }
    }
}

@Composable
private fun CalibrationResultCard(bodyProportions: BodyProportions?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = l10n("Calibration Status", "定制状态"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = if (bodyProportions?.isComplete == true) {
                    l10n("Ready for camera and video training.", "已可用于摄像头和视频训练。")
                } else {
                    l10n("Not ready. Upload and analyze a photo first.", "尚未完成。请先上传并分析照片。")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )

            if (bodyProportions?.isComplete == true) {
                Text(
                    text = l10n(
                        "Coefficients: ${bodyProportions.coefficients.size} generated",
                        "已生成系数：${bodyProportions.coefficients.size} 个"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = SuccessGreen,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatInputCard(
                        label = l10n("Left shoulder", "左肩"),
                        value = bodyProportions.leftEarShoulderCoefficient?.formatNumber() ?: "-",
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                    StatInputCard(
                        label = l10n("Right shoulder", "右肩"),
                        value = bodyProportions.rightEarShoulderCoefficient?.formatNumber() ?: "-",
                        unit = "",
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedButton(
                    onClick = { BodyCalibrationRepository.clear() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(l10n("Clear Body Data", "清空身材数据"))
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(onNavigateBack: () -> Unit) {
    val profile by UserProfileRepository.profile.collectAsState()
    val context = LocalContext.current
    var nameText by remember(profile.name) { mutableStateOf(profile.name) }
    var emailText by remember(profile.email) { mutableStateOf(profile.email) }
    var avatarStyle by remember(profile.avatarStyle) { mutableIntStateOf(profile.avatarStyle) }
    var avatarUri by remember(profile.avatarUri) { mutableStateOf(profile.avatarUri) }

    val avatarPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            avatarUri = uri.toString()
        }
    }

    val nameValid = nameText.trim().length in 2..32
    val emailValid = emailText.trim().let { "@" in it && "." in it && it.length <= 80 }

    Scaffold(
        topBar = {
            ProfileTopBar(
                title = l10n("Edit Profile"),
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = LightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AvatarCircle(
                            name = nameText,
                            avatarStyle = avatarStyle,
                            avatarUri = avatarUri,
                            size = 92
                        )
                    }

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it.take(32) },
                        label = { Text(l10n("Nickname")) },
                        singleLine = true,
                        isError = nameText.isNotBlank() && !nameValid,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = emailText,
                        onValueChange = { emailText = it.take(80) },
                        label = { Text(l10n("Email")) },
                        singleLine = true,
                        isError = emailText.isNotBlank() && !emailValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = l10n("Avatar"),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { avatarPickerLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                        ) {
                            Text(l10n("Choose Photo"))
                        }
                        OutlinedButton(
                            onClick = { avatarUri = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(l10n("Remove"))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(UserProfileRepository.AVATAR_STYLE_COUNT) { index ->
                            AvatarChoice(
                                index = index,
                                selected = avatarStyle == index,
                                onClick = { avatarStyle = index },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    if (!nameValid || !emailValid) {
                        Text(
                            text = l10n("Use a nickname with 2-32 characters and a valid email address."),
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }

                    Button(
                        onClick = {
                            UserProfileRepository.updateProfile(nameText, emailText, avatarStyle, avatarUri)
                            onNavigateBack()
                        },
                        enabled = nameValid && emailValid,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepPurple)
                    ) {
                        Text(l10n("Save"))
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileInfoScreen(
    type: ProfileInfoType,
    onNavigateBack: () -> Unit
) {
    val appLanguage = LocalAppLanguage.current

    Scaffold(
        topBar = {
            ProfileTopBar(
                title = type.localizedTitle(appLanguage),
                onNavigateBack = onNavigateBack
            )
        },
        containerColor = LightBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = when (type) {
                            ProfileInfoType.ABOUT_FLEXFIT -> Icons.Default.FitnessCenter
                            ProfileInfoType.RATE_APP -> Icons.Default.Star
                            ProfileInfoType.FEEDBACK -> Icons.Default.Mail
                            ProfileInfoType.NOTIFICATIONS -> Icons.Default.Notifications
                            ProfileInfoType.DEVICE_SETTINGS -> Icons.Default.Settings
                        },
                        contentDescription = null,
                        tint = DeepPurple,
                        modifier = Modifier.size(34.dp)
                    )
                    Text(
                        text = type.localizedTitle(appLanguage),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = type.localizedBody(appLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun ProfileTopBar(
    title: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = l10n("Back"),
                    tint = TextPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = LightBackground)
    )
}

private fun Float.formatNumber(): String {
    return if (this % 1f == 0f) {
        toInt().toString()
    } else {
        String.format("%.1f", this)
    }
}

private fun String.filterDecimalInput(): String {
    val filtered = filter { it.isDigit() || it == '.' }
    val firstDot = filtered.indexOf('.')
    return if (firstDot == -1) {
        filtered.take(5)
    } else {
        filtered.take(firstDot + 1) + filtered.drop(firstDot + 1).replace(".", "").take(1)
    }
}

private fun avatarPalette(index: Int): List<Color> {
    return when (index) {
        1 -> listOf(SuccessGreen, Color(0xFF2E7D32))
        2 -> listOf(WarningOrange, Color(0xFFF57C00))
        3 -> listOf(Color(0xFF1565C0), Color(0xFF26A69A))
        else -> listOf(DeepPurple, AccentPurple)
    }
}

@Composable
private fun rememberAvatarBitmap(avatarUri: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(avatarUri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(avatarUri) {
        bitmap = avatarUri?.let { loadAvatarBitmap(context, it) }
    }

    return bitmap
}

private suspend fun loadAvatarBitmap(context: Context, avatarUri: String): ImageBitmap? {
    return withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(avatarUri))?.use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
}

private suspend fun analyzeBodyCalibrationPhoto(
    context: Context,
    uri: Uri
): Result<BodyProportions> {
    return withContext(Dispatchers.Default) {
        val bitmap = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        } ?: return@withContext Result.failure(IllegalArgumentException("Photo could not be opened."))

        val detector = PoseDetectorWrapper(context.applicationContext)
        var analysisResult: Result<BodyProportions>? = null

        try {
            detector.initialize()
            detector.processBitmap(
                bitmap,
                object : PoseDetectorCallback {
                    override fun onPoseDetected(keypoints: FloatArray, confidence: Float) {
                        analysisResult = Result.failure(IllegalStateException("Pose landmarks were incomplete."))
                    }

                    override fun onPoseDetected(
                        keypoints: FloatArray,
                        landmarkConfidences: FloatArray,
                        confidence: Float
                    ) {
                        analysisResult = when (val analysis = BodyProportionAnalyzer.analyze(keypoints, landmarkConfidences)) {
                            is BodyProportionAnalysis.Success -> {
                                Result.success(
                                    BodyProportions(
                                        coefficients = analysis.coefficients,
                                        sourceUri = uri.toString(),
                                        updatedAtMillis = System.currentTimeMillis()
                                    )
                                )
                            }
                            is BodyProportionAnalysis.Failure -> Result.failure(
                                IllegalArgumentException(analysis.reason)
                            )
                        }
                    }

                    override fun onPoseNotDetected() {
                        analysisResult = Result.failure(IllegalArgumentException("No person was detected in the photo."))
                    }

                    override fun onError(error: String) {
                        analysisResult = Result.failure(IllegalStateException(error))
                    }
                }
            )
        } catch (error: Exception) {
            analysisResult = Result.failure(error)
        } finally {
            detector.close()
            bitmap.recycle()
        }

        analysisResult ?: Result.failure(IllegalStateException("Photo analysis did not return a result."))
    }
}
