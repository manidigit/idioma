package com.manidigit.flashlearn.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.manidigit.flashlearn.ui.components.*

private data class Word(val source:String,val meaning:String,val category:String,val difficulty:String,val stage:String)
private val words = listOf(
    Word("aprovechar","استفاده کردن","Travel","HARD","WEEKLY"),
    Word("manzana","سیب","Food","EASY","LEARNED"),
    Word("quedar","ماندن / قرار گذاشتن","Daily Conversation","MEDIUM","DAILY"),
    Word("viajar","سفر کردن","Travel","EASY","MONTHLY"),
    Word("gracias","ممنون","Daily Conversation","EASY","LEARNED")
)

enum class Tab { HOME, VOCAB, REVIEW, PROGRESS, SETTINGS }

@Composable fun AppRoot() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var reviewType by remember { mutableStateOf("Daily") }
    var reviewIndex by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    Scaffold(bottomBar = { BottomBar(tab) { tab = it } }) { padding ->
        Box(Modifier.padding(padding)) {
            when(tab) {
                Tab.HOME -> HomeScreen { tab = Tab.REVIEW }
                Tab.VOCAB -> VocabularyScreen()
                Tab.REVIEW -> ReviewScreen(reviewType, { reviewType = it; reviewIndex = 0; revealed = false }, reviewIndex, revealed, { revealed = !revealed }, { correct -> if(correct) score++; reviewIndex = (reviewIndex + 1) % words.size; revealed = false })
                Tab.PROGRESS -> ProgressScreen(score)
                Tab.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable private fun BottomBar(selected: Tab, onSelect:(Tab)->Unit) {
    NavigationBar { listOf(Tab.HOME to Icons.Default.Home, Tab.VOCAB to Icons.Default.MenuBook, Tab.REVIEW to Icons.Default.Refresh, Tab.PROGRESS to Icons.Default.Insights, Tab.SETTINGS to Icons.Default.Settings).forEach { (tab, icon) ->
        NavigationBarItem(selected = selected == tab, onClick = { onSelect(tab) }, icon = { Icon(icon, null) }, label = { Text(tab.name.lowercase().replaceFirstChar { it.uppercase() }) })
    }}
}

@Composable private fun HomeScreen(start:()->Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { BrandHeader("Good evening 👋", "Spanish → Persian  •  Your personal memory trainer") }
        item { HeroCard("Today's Learning", "20 cards are waiting for you", .72f, "Start review", start) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier=Modifier.fillMaxWidth()) { StatCard("Streak", "7 days", "🔥", Modifier.weight(1f)); StatCard("Mastered", "324", "🧠", Modifier.weight(1f)) } }
        item { Text("Review queue", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
        item { StageCard("Daily", 12, "Keep your memory active", "☀️", start) }
        item { StageCard("Weekly", 8, "Strengthen retention", "📅", start) }
        item { StageCard("Monthly", 3, "Long-term memory", "🌙", start) }
        item { Text("Learning momentum", style=MaterialTheme.typography.titleLarge, fontWeight=FontWeight.Bold) }
        item { Card(shape=MaterialTheme.shapes.large) { Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) { Text("You are on a strong run", fontWeight=FontWeight.Bold); LinearProgressIndicator(progress={.78f}, modifier=Modifier.fillMaxWidth()); Text("78% memory strength", color=MaterialTheme.colorScheme.onSurfaceVariant) } } }
    }
}

@Composable private fun ReviewScreen(type:String,onType:(String)->Unit,index:Int,revealed:Boolean,onReveal:()->Unit,onAnswer:(Boolean)->Unit) {
    val word=words[index]
    LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween) { Text("Review", style=MaterialTheme.typography.headlineSmall, fontWeight=FontWeight.Bold); Text("${index+1} / ${words.size}") } }
        item { SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf("Daily","Weekly","Monthly","Random").forEachIndexed { i, t -> SegmentedButton(selected=t==type,onClick={onType(t)},shape=SegmentedButtonDefaults.itemShape(i,4)) { Text(t) } } } }
        item { Card(onClick=onReveal, modifier=Modifier.fillMaxWidth().animateContentSize(), shape=MaterialTheme.shapes.extraLarge) { Column(Modifier.padding(28.dp).fillMaxWidth(), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(14.dp)) { Text("SPANISH", style=MaterialTheme.typography.labelLarge, color=MaterialTheme.colorScheme.primary); Text(word.source, style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold); IconButton(onClick={}) { Icon(Icons.Default.VolumeUp, "Play pronunciation") }; AnimatedContent(revealed,label="answer") { show -> if(show) { Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("Persian", style=MaterialTheme.typography.labelMedium); Text(word.meaning, style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.SemiBold); Text("Hay que aprovechar el tiempo.", color=MaterialTheme.colorScheme.onSurfaceVariant) } } else Text("Tap the card to reveal", color=MaterialTheme.colorScheme.onSurfaceVariant) } } } }
        item { if(!revealed) Button(onClick=onReveal, Modifier.fillMaxWidth().height(54.dp)) { Text("Show answer") } else Row(horizontalArrangement=Arrangement.spacedBy(12.dp),modifier=Modifier.fillMaxWidth()) { OutlinedButton(onClick={onAnswer(false)},Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.Close,null); Spacer(Modifier.width(6.dp)); Text("I was wrong") }; Button(onClick={onAnswer(true)},Modifier.weight(1f).height(54.dp)) { Icon(Icons.Default.Check,null); Spacer(Modifier.width(6.dp)); Text("I knew it") } } }
    }
}

@Composable private fun VocabularyScreen() {
    var query by remember { mutableStateOf("") }
    val filtered=words.filter { it.source.contains(query,true)||it.meaning.contains(query,true) }
    Column(Modifier.fillMaxSize()) { BrandHeader("Vocabulary", "Your living language library")
        OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().padding(horizontal=20.dp),placeholder={Text("Search words, meanings, examples…")},leadingIcon={Icon(Icons.Default.Search,null)},singleLine=true)
        LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) { items(filtered) { word -> Card(Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large) { Row(Modifier.padding(18.dp),verticalAlignment=Alignment.CenterVertically) { Column(Modifier.weight(1f)){Text(word.source,style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text(word.meaning);Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){AssistChip(onClick={},label={Text(word.category)});AssistChip(onClick={},label={Text(word.difficulty)})}};IconButton(onClick={}){Icon(Icons.Default.StarBorder,"Favorite")}} } } } }
    }

@Composable private fun ProgressScreen(score:Int) { LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){item{BrandHeader("Progress","See what your memory is actually retaining")};item{Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){StatCard("Memory strength","78%","🧠",Modifier.weight(1f));StatCard("Momentum","+12%","⚡",Modifier.weight(1f))}};item{Card(shape=MaterialTheme.shapes.large){Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){Text("Mastery",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold);Text("324 / 1,245 words mastered");LinearProgressIndicator(progress={.26f},Modifier.fillMaxWidth())}}};item{Text("Milestones",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)};item{StageCard("First 100",100,"Vocabulary builder","🎯",{})};item{StageCard("Seven-day momentum",7,"Keep your review streak","🔥",{})};item{StageCard("Long-term memory",50,"Monthly reviews passed","🌙",{})};item{Text("This session: $score correct",color=MaterialTheme.colorScheme.primary)}}}

@Composable private fun SettingsScreen(){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){item{BrandHeader("Settings","Make FlashLearn work your way")};item{ListItem(headlineContent={Text("Language pair")},supportingContent={Text("Spanish → Persian")},leadingContent={Icon(Icons.Default.Translate,null)},trailingContent={Icon(Icons.Default.ChevronRight,null)})};item{ListItem(headlineContent={Text("Theme")},supportingContent={Text("System default")},leadingContent={Icon(Icons.Default.DarkMode,null)},trailingContent={Icon(Icons.Default.ChevronRight,null)})};item{ListItem(headlineContent={Text("Backup & Export")},supportingContent={Text("JSON • CSV • XLSX • SQLite")},leadingContent={Icon(Icons.Default.Backup,null)},trailingContent={Icon(Icons.Default.ChevronRight,null)})};item{ListItem(headlineContent={Text("AI Translation")},supportingContent={Text("Provider configured separately; no key in source")},leadingContent={Icon(Icons.Default.AutoAwesome,null)},trailingContent={Icon(Icons.Default.ChevronRight,null)})}}}
