package io.github.yorulmazsinan.kokpit.home.cards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.yorulmazsinan.kokpit.ui.theme.Dimens

@Composable
fun KokpitCard(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(Dimens.CardRadius), color = MaterialTheme.colorScheme.surface) {
        Box(Modifier.padding(Dimens.CardPadding), content = content)
    }
}
