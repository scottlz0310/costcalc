package io.github.scottlz0310.shoptools.feature.menu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.scottlz0310.shoptools.R
import io.github.scottlz0310.shoptools.feature.menu.ToolFeature

@Composable
fun MenuScreen(
    features: List<ToolFeature>,
    onOpenFeature: (route: String) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.menu_banner),
            contentDescription = stringResource(R.string.menu_banner_description),
            contentScale = ContentScale.FillWidth,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
        )
        Text(
            text = stringResource(R.string.menu_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        features.forEach { feature ->
            FeatureCard(feature = feature, onClick = { onOpenFeature(feature.route) })
        }
    }
}

@Composable
private fun FeatureCard(
    feature: ToolFeature,
    onClick: () -> Unit,
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // タイトルと説明で内容が伝わるため、イラストは装飾として扱う。
            Image(
                painter = painterResource(feature.graphicRes),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(feature.titleRes),
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = stringResource(feature.descriptionRes),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
