package com.ist.pharmacist.ui.screens

//import androidx.compose.material.icons.filled.History
//import androidx.compose.material.icons.filled.Medication
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.ist.pharmacist.R
import com.ist.pharmacist.ui.views.PharmaciesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MedicineSearchBarScreen(
    backHandler: () -> Unit,
    viewModel: PharmaciesViewModel,
    onPharmacyPanel: (String) -> Unit = {},
    context: Context
) {
    var text by remember { mutableStateOf("") }
    var active by remember { mutableStateOf(false) }
    val medicines = viewModel.getMedicines().value?.values?.toList() ?: emptyList()
    var searchResults by remember { mutableStateOf(emptyList<String>()) }
    var filteredMedicines by remember { mutableStateOf(medicines) }

    val inriaSerifFont = FontFamily(
        Font(R.font.inria_serif_regular),
    )

    LaunchedEffect(text) {
        if (active) {
            filteredMedicines = if (text.isBlank()) {
                medicines
            } else {
                medicines.filter { medicine ->
                    medicine.name.contains(text, ignoreCase = true)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            stringResource(id = R.string.search_medicine),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        backHandler()
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            tint = colorResource(id = R.color.black),
                            contentDescription = "Localized description"
                        )
                    }
                },
            )
        },
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.SearchBar(
                    query = text,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp, 0.dp, 15.dp, 15.dp),
                    onQueryChange = {
                        text = it
                    },
                    onSearch = {
                        active = false
                        viewModel.viewModelScope.launch {
                            searchResults = viewModel.searchPharmaciesWithMedicineName(
                                text,
                                context
                            )
                        }
                    },
                    active = active,
                    onActiveChange = {
                        active = it
                    },
                    placeholder = {
                        Text(text = "Search medicine...")
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Icon")
                    },
                    trailingIcon = {
                        if (active) {
                            Icon(
                                modifier = Modifier.clickable {
                                    if (text.isEmpty()) {
                                        active = false
                                        searchResults = emptyList()
                                    } else {
                                        text = ""
                                    }
                                },
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Icon"
                            )
                        }
                    }
                ) {
                    LazyColumn {
                        items(if (active) filteredMedicines else emptyList()) { medicine ->
                            Row(
                                modifier = Modifier
                                    .padding(all = 14.dp)
                                    .clickable {
                                        text = medicine.name
                                        active = false
                                        viewModel.viewModelScope.launch {
                                            searchResults = viewModel.searchPharmaciesWithMedicineName(
                                                text,
                                                context
                                            )
                                        }
                                    }
                            ) {
                                Icon(
                                    modifier = Modifier.padding(end = 10.dp),
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Medication Icon"
                                )
                                Text(text = medicine.name)
                            }
                        }
                    }
                }

                val state = rememberLazyListState()
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                        .scrollbar(state, horizontal = false)
                ) {
                    Log.d("SearchResults", searchResults.toString())
                    items(searchResults) { result ->
                        val (pharmacyID, distance) = result.split(" - ")
                        val pharmacyName = viewModel.getPharmacy(pharmacyID)?.name
                        Button(
                            onClick = {
                                onPharmacyPanel(pharmacyID)
                            },
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier
                                .width(300.dp)
                                .height(92.dp)
                                .padding(vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.pharmacy_marker),
                                    contentDescription = "Medicine Icon",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (pharmacyName != null) {
                                    Text(
                                        text = pharmacyName,
                                        fontSize = 15.sp,
                                        fontFamily = inriaSerifFont,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Text(
                                    text = distance,
                                    fontSize = 12.sp,
                                    fontFamily = inriaSerifFont,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
