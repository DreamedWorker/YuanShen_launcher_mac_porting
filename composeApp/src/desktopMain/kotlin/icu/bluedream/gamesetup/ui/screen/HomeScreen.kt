package icu.bluedream.gamesetup.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import hoyogamesetup.composeapp.generated.resources.*
import icu.bluedream.gamesetup.core.constant.GameType
import icu.bluedream.gamesetup.ui.compositions.NormalSpacer
import icu.bluedream.gamesetup.ui.viewmodel.HomeScreenViewModel
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

class HomeScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val viewModel = rememberScreenModel { HomeScreenViewModel() }
        var showMenu by remember { mutableStateOf(false) }
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text(stringResource(Res.string.home_title)) },
                    actions = {
                        IconButton({ showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "more option icon")
                        }
                        DropdownMenu(expanded = showMenu, { showMenu = !showMenu}) {
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.home_menu_about)) },
                                onClick = {},
                                leadingIcon = { Icon(Icons.Default.Person, "about") }
                            )
                        }
                    }
                )
            }
        ) { pd ->
            Column {
                Column(
                    modifier = Modifier.padding(pd).fillMaxWidth().padding(16.dp)
                ) {
                    Card {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp).padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Search, "choose or install a game",
                                modifier = Modifier.size(24.dp)
                            )
                            NormalSpacer()
                            Text(stringResource(Res.string.home_step_choose_or_install), style = MaterialTheme.typography.headlineSmall)
                            Spacer(Modifier.weight(1f))
                        }
                        Row(
                            modifier = Modifier.padding(4.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(Res.string.home_set_game_type))
                            Spacer(Modifier.weight(1f))
                            Column {
                                ElevatedButton({ viewModel.showGameType.value = true }) {
                                    Text(stringResource(GameType.getLiteralName(viewModel.selectedGame.value)))
                                }
                                DropdownMenu(viewModel.showGameType.value, { viewModel.showGameType.value = false }) {
                                    for (single in stringArrayResource(Res.array.gameType_list)) {
                                        DropdownMenuItem(
                                            text = { Text(single) },
                                            onClick = {
                                                viewModel.selectedGame.value = GameType.getGameType(single)
                                                viewModel.showGameType.value = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.padding(4.dp).padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(stringResource(Res.string.home_path_selected))
                            NormalSpacer()
                            Text(
                                viewModel.selectedDir.value,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                            )
                            NormalSpacer()
                            Spacer(Modifier.weight(1f))
                            Button({ viewModel.startChoosingPath() }) {
                                Text(stringResource(Res.string.home_choose))
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(horizontal = 16.dp).padding(bottom = 16.dp)
                ) {
                    ElevatedCard(
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Row {
                            Spacer(Modifier.weight(1f))
                            FloatingActionButton(
                                { viewModel.startTask() },
                                modifier = Modifier.padding(8.dp),
                                shape = FloatingActionButtonDefaults.smallShape
                            ) {
                                Icon(Icons.Default.Done, "start task icon button")
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                stringResource(Res.string.home_start_log_tip),
                                style = MaterialTheme.typography.bodySmall
                                    .copy(color = MaterialTheme.colorScheme.secondary)
                            )
                            Text(
                                viewModel.installLog.value,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}