package org.ghostsinthelab.app.makedown.reader.markdown

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.ghostsinthelab.app.makedown.reader.text.PlainTextReader
import org.ghostsinthelab.app.makedown.ui.LocalReaderFontFamily

@Composable
fun MarkdownReader(
    source: String,
    showRaw: Boolean,
    modifier: Modifier = Modifier,
) {
    if (showRaw) {
        PlainTextReader(text = source, modifier = modifier)
        return
    }

    val blocks = remember(source) { MarkdownToBlocks.convert(source) }
    val listState = rememberLazyListState()
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        itemsIndexed(blocks) { _, block ->
            RenderBlock(block)
            Spacer(Modifier.padding(vertical = 4.dp))
        }
    }
}

@Composable
private fun RenderBlock(block: MdBlock) {
    val readerFont = LocalReaderFontFamily.current
    when (block) {
        is MdBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            Text(
                text = block.text,
                style = style,
                fontFamily = readerFont,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
            )
        }
        is MdBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = readerFont,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is MdBlock.BulletList -> {
            for ((idx, item) in block.items.withIndex()) {
                ListRow(marker = "•", itemBlocks = item, key = idx)
            }
        }
        is MdBlock.OrderedList -> {
            for ((idx, item) in block.items.withIndex()) {
                ListRow(marker = "${block.start + idx}.", itemBlocks = item, key = idx)
            }
        }
        is MdBlock.BlockQuote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
            ) {
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline),
                )
                Spacer(Modifier.width(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    for (child in block.children) {
                        RenderBlock(child)
                        Spacer(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }
        is MdBlock.CodeBlock -> {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = block.code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    softWrap = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                )
            }
        }
        MdBlock.HorizontalRule -> {
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
private fun ListRow(marker: String, itemBlocks: List<MdBlock>, key: Int) {
    val readerFont = LocalReaderFontFamily.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = "$marker ",
            style = MaterialTheme.typography.bodyLarge,
            fontFamily = readerFont,
            modifier = Modifier.width(28.dp),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            for (child in itemBlocks) {
                RenderBlock(child)
            }
        }
    }
}
