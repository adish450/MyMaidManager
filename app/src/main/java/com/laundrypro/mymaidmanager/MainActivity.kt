package com.laundrypro.mymaidmanager

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
// --- REMOVED UNUSED LIFECYCLE IMPORTS ---
// import androidx.compose.ui.platform.LocalLifecycleOwner
// import androidx.lifecycle.Lifecycle
// import androidx.lifecycle.LifecycleEventObserver
// --- END REMOVED IMPORTS ---
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.JsonParser
// Adjust import path
import com.laundrypro.mymaidmanager.models.AttendanceRecord
import com.laundrypro.mymaidmanager.models.Maid
import com.laundrypro.mymaidmanager.models.PayrollResponse
import com.laundrypro.mymaidmanager.models.Task
import com.laundrypro.mymaidmanager.ui.theme.MyMaidManagerTheme
import com.laundrypro.mymaidmanager.viewmodel.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MainActivity"

sealed class Screen(val route: String) {
    object Auth : Screen("auth_screen")
    object MainList : Screen("main_list_screen")
    object MaidDetail : Screen("maid_detail_screen/{maidId}") {
        fun createRoute(maidId: String) = "maid_detail_screen/$maidId"
    }
}

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyMaidManagerTheme {
                val navController = rememberNavController()
                AppNavigator(navController, authViewModel)
            }
        }
    }
}

@Composable
fun AppNavigator(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()

    // --- FIX: Create the ViewModel here to share it ---
    val maidViewModel: MaidViewModel = viewModel()

    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.MainList.route
        is AuthState.Unauthenticated -> Screen.Auth.route
        is AuthState.Unknown -> "loading_screen"
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("loading_screen") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable(Screen.Auth.route) {
            AuthScreen(authViewModel, onLoginSuccess = {
                navController.navigate(Screen.MainList.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }
        composable(Screen.MainList.route) {
            MainListScreen(
                // --- FIX: Pass the shared ViewModel ---
                maidViewModel = maidViewModel,
                onLogout = { authViewModel.logout() },
                onMaidClicked = { maidId ->
                    navController.navigate(Screen.MaidDetail.createRoute(maidId))
                }
            )
        }
        composable(Screen.MaidDetail.route) { backStackEntry ->
            val maidId = backStackEntry.arguments?.getString("maidId")
            if (maidId != null) {
                MaidDetailScreen(
                    // --- FIX: Pass the shared ViewModel ---
                    maidViewModel = maidViewModel,
                    maidId = maidId,
                    onNavigateUp = {
                        navController.navigateUp()
                    },
                    onMaidDeleted = {
                        navController.popBackStack() // Go back to the main list
                    }
                )
            } else {
                LaunchedEffect(Unit) { navController.navigateUp() }
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated && navController.currentDestination?.route != Screen.Auth.route) {
            navController.navigate(Screen.Auth.route) { popUpTo(0) }
        }
        if (authState is AuthState.Authenticated && navController.currentDestination?.route == "loading_screen") {
            navController.navigate(Screen.MainList.route) { popUpTo(0) }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainListScreen(
    // --- FIX: Accept the shared ViewModel ---
    maidViewModel: MaidViewModel,
    onLogout: () -> Unit,
    onMaidClicked: (String) -> Unit
) {
    // --- FIX: Use the passed-in ViewModel ---
    // val maidViewModel: MaidViewModel = viewModel() // <-- This was the bug
    val uiState by maidViewModel.maidListUIState.collectAsStateWithLifecycle()
    var showAddMaidDialog by remember { mutableStateOf(false) }

    // --- Add a one-time fetch on initial load ---
    LaunchedEffect(Unit) {
        // Only fetch if the list isn't already loaded
        if (uiState !is MaidListUIState.Success) {
            maidViewModel.fetchMaids()
        }
    }
    // --- END ---


    if (showAddMaidDialog) {
        AddMaidDialog(
            onDismiss = { showAddMaidDialog = false },
            onAddMaid = { name, mobile, address ->
                maidViewModel.addMaid(name, mobile, address)
                showAddMaidDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Maids") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddMaidDialog = true }) {
                Icon(Icons.Default.Add, "Add Maid")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MaidListUIState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MaidListUIState.Success -> {
                    if (state.maids.isEmpty()) {
                        Text("No maids found. Tap '+' to add one.", modifier = Modifier.align(Alignment.Center), textAlign = TextAlign.Center)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.maids, key = { it.id }) { maid ->
                                MaidCard(maid, onClick = { onMaidClicked(maid.id) })
                            }
                        }
                    }
                }
                is MaidListUIState.Error -> Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { maidViewModel.fetchMaids() }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
fun AddMaidDialog(
    onDismiss: () -> Unit,
    onAddMaid: (name: String, mobile: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val isMobileValid = mobile.length == 10
    val isButtonEnabled = name.isNotBlank() && isMobileValid && address.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add New Maid", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10) mobile = it.filter { c -> c.isDigit() } },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Text("+91 ") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mobile.isNotEmpty() && !isMobileValid,
                    singleLine = true
                )
                if (mobile.isNotEmpty() && !isMobileValid) { Text("Please enter a valid 10-digit mobile number.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth()) }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAddMaid(name, "+91$mobile", address) }, enabled = isButtonEnabled) { Text("Add") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaidCard(maid: Maid, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(maid.name ?: "", style = MaterialTheme.typography.titleMedium)
            Text(maid.mobile ?: "", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(maid.address ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaidDetailScreen(
    // --- FIX: Accept the shared ViewModel ---
    maidViewModel: MaidViewModel,
    maidId: String,
    onNavigateUp: () -> Unit,
    onMaidDeleted: () -> Unit
) {
    // --- FIX: Use the passed-in ViewModel ---
    // val viewModel: MaidViewModel = viewModel() // <-- This was the bug
    val uiState by maidViewModel.maidDetailUIState.collectAsStateWithLifecycle()
    val deleteState by maidViewModel.deleteState.collectAsStateWithLifecycle()

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // Listen for delete success and navigate up
    LaunchedEffect(deleteState) {
        if (deleteState is MaidDeleteState.Success) {
            Toast.makeText(context, "Maid deleted", Toast.LENGTH_SHORT).show()
            maidViewModel.resetDeleteState()
            onMaidDeleted() // Navigate back
        }
        if(deleteState is MaidDeleteState.Error) {
            Toast.makeText(context, (deleteState as MaidDeleteState.Error).message, Toast.LENGTH_SHORT).show()
            maidViewModel.resetDeleteState()
        }
    }

    LaunchedEffect(key1 = maidId) {
        maidViewModel.fetchMaidDetails(maidId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maid Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Maid")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Maid")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is MaidDetailUIState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MaidDetailUIState.Success -> {
                    // Pass current maid data to dialogs
                    if (showEditDialog) {
                        EditMaidDialog(
                            maid = state.maid,
                            onDismiss = { showEditDialog = false },
                            onConfirm = { name, mobile, address ->
                                maidViewModel.updateMaid(maidId, name, mobile, address)
                                showEditDialog = false
                            }
                        )
                    }
                    if (showDeleteDialog) {
                        DeleteConfirmationDialog(
                            maidName = state.maid.name,
                            onDismiss = { showDeleteDialog = false },
                            onConfirm = {
                                maidViewModel.deleteMaid(maidId)
                                showDeleteDialog = false
                            },
                            isLoading = deleteState is MaidDeleteState.Loading
                        )
                    }

                    MaidDetailContent(
                        maid = state.maid,
                        viewModel = maidViewModel // Pass the shared ViewModel
                    )
                }
                is MaidDetailUIState.Error -> Text(state.message, modifier = Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaidDetailContent(
    maid: Maid,
    viewModel: MaidViewModel // <-- Accept the shared ViewModel
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showEditTaskDialog by remember { mutableStateOf<Task?>(null) }

    var showAttendanceDialog by remember { mutableStateOf<Task?>(null) }
    var showManualAttendanceDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    val payrollState by viewModel.payrollUIState.collectAsStateWithLifecycle()
    val manualAttendanceState by viewModel.manualAttendanceState.collectAsStateWithLifecycle()
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current

    LaunchedEffect(key1 = maid.id) {
        if (payrollState !is PayrollUIState.Success) {
            viewModel.fetchPayroll(maid.id)
        }
    }

    LaunchedEffect(manualAttendanceState) {
        if (manualAttendanceState is ManualAttendanceState.Success) {
            Toast.makeText(context, "Absence Recorded", Toast.LENGTH_SHORT).show()
            showManualAttendanceDialog = false
            selectedDateMillis = null
            viewModel.resetManualAttendanceState()
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { name, price, freq ->
                viewModel.addTask(maid.id, name, price, freq)
                showAddTaskDialog = false
            }
        )
    }

    showEditTaskDialog?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { showEditTaskDialog = null },
            onEditTask = { taskId, name, price, freq ->
                viewModel.updateTask(maid.id, taskId, name, price, freq)
                showEditTaskDialog = null
            }
        )
    }

    showAttendanceDialog?.let { task ->
        AttendanceOtpDialog(
            maidName = maid.name ?: "Maid",
            viewModel = viewModel,
            onDismiss = {
                viewModel.resetOtpState()
                showAttendanceDialog = null
            },
            onVerify = { otp ->
                viewModel.verifyOtpForAttendance(maid.id, otp, task.name ?: "")
            }
        )
    }

    if (showDatePickerDialog) {
        CustomDatePickerDialog(
            initialSelectedDateMillis = selectedDateMillis ?: System.currentTimeMillis(),
            onDismissRequest = {
                showDatePickerDialog = false
                showManualAttendanceDialog = true // Re-open Manual Dialog
            },
            onDateSelected = { newDateMillis ->
                selectedDateMillis = newDateMillis
                Log.d(TAG, "Custom DatePicker Confirmed: ${selectedDateMillis}ms UTC")
                showDatePickerDialog = false
                showManualAttendanceDialog = true // Re-open Manual Dialog
            }
        )
    }

    if (showManualAttendanceDialog) {
        Log.d(TAG, "Rendering ManualAttendanceDialog")
        ManualAttendanceDialog(
            tasks = maid.tasks ?: emptyList(),
            selectedDateMillis = selectedDateMillis,
            manualAttendanceState = manualAttendanceState,
            onDismiss = {
                Log.d(TAG, "ManualAttendanceDialog dismissed")
                showManualAttendanceDialog = false
                selectedDateMillis = null
                viewModel.resetManualAttendanceState()
            },
            onConfirm = { date, taskName, status ->
                Log.d(TAG, "ManualAttendanceDialog confirmed: Date=$date, Task=$taskName, Status=$status")
                viewModel.addManualAttendance(maid.id, date, taskName, status)
            },
            onSelectDateClicked = {
                Log.d(TAG, "onSelectDateClicked callback received, swapping dialogs")
                showManualAttendanceDialog = false // Hide this dialog
                showDatePickerDialog = true // Show the date picker
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { MaidInfoSection(maid) }
        item { PayrollSection(payrollState) }
        item {
            TasksSection(
                tasks = maid.tasks ?: emptyList(),
                onAddTask = { showAddTaskDialog = true },
                onEditTask = { task -> showEditTaskDialog = task },
                onDeleteTask = { taskId -> viewModel.deleteTask(maid.id, taskId) }
            )
        }
        item {
            AttendanceSection(
                attendance = maid.attendance ?: emptyList(),
                tasks = maid.tasks ?: emptyList(),
                onMarkAttendance = { task ->
                    viewModel.requestOtpForAttendance(maid.id)
                    showAttendanceDialog = task
                },
                onMarkAbsent = {
                    Log.d(TAG, "Mark Absent Button Clicked - Setting showManualAttendanceDialog to true")
                    selectedDateMillis = System.currentTimeMillis()
                    showManualAttendanceDialog = true
                }
            )
        }
    }
}

@Composable
fun EditMaidDialog(
    maid: Maid,
    onDismiss: () -> Unit,
    onConfirm: (name: String, mobile: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf(maid.name ?: "") }
    var mobile by remember { mutableStateOf(maid.mobile?.removePrefix("+91") ?: "") }
    var address by remember { mutableStateOf(maid.address ?: "") }

    val isMobileValid = mobile.length == 10
    val isButtonEnabled = name.isNotBlank() && isMobileValid && address.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Maid", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(
                    value = mobile,
                    onValueChange = { if (it.length <= 10) mobile = it.filter { c -> c.isDigit() } },
                    label = { Text("Mobile Number") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Text("+91 ") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = mobile.isNotEmpty() && !isMobileValid,
                    singleLine = true
                )
                if (mobile.isNotEmpty() && !isMobileValid) { Text("Please enter a valid 10-digit mobile number.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth()) }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onConfirm(name, "+91$mobile", address) }, enabled = isButtonEnabled) { Text("Update") }
                }
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    maidName: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Maid") },
        text = { Text("Are you sure you want to delete ${maidName ?: "this maid"}? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDatePickerDialog(
    initialSelectedDateMillis: Long,
    onDismissRequest: () -> Unit,
    onDateSelected: (Long?) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .wrapContentHeight()
                    .widthIn(max = 360.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DatePicker(state = datePickerState)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismissRequest) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onDateSelected(datePickerState.selectedDateMillis)
                            }
                        ) { Text("OK") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualAttendanceDialog(
    tasks: List<Task>,
    selectedDateMillis: Long?,
    manualAttendanceState: ManualAttendanceState,
    onDismiss: () -> Unit,
    onConfirm: (date: String, taskName: String, status: String) -> Unit,
    onSelectDateClicked: () -> Unit
) {
    Log.d(TAG, "Inside ManualAttendanceDialog composable")
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var expanded by remember { mutableStateOf(false) }
    val isLoading = manualAttendanceState is ManualAttendanceState.Loading

    val formattedDate = remember(selectedDateMillis) {
        selectedDateMillis?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(it))
        } ?: ""
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Mark Attendance Manually", style = MaterialTheme.typography.headlineSmall)

                Button(onClick = onSelectDateClicked, enabled = !isLoading) {
                    Text(if (formattedDate.isNotEmpty()) "Date: $formattedDate" else "Select Date")
                }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isLoading) expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedTask?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Task") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        enabled = !isLoading
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        tasks.forEach { task ->
                            DropdownMenuItem(
                                text = { Text(task.name ?: "") },
                                onClick = {
                                    selectedTask = task
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                val status = "Absent"
                Text("Status: $status", style = MaterialTheme.typography.bodyLarge)

                if (manualAttendanceState is ManualAttendanceState.Error) {
                    Text(
                        text = manualAttendanceState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (formattedDate.isNotEmpty() && selectedTask != null) {
                                onConfirm(formattedDate, selectedTask!!.name ?: "", status)
                            }
                        },
                        enabled = formattedDate.isNotEmpty() && selectedTask != null && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Text("Confirm Absent")
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MaidInfoSection(maid: Maid) {
    // --- NEW: Add context for phone intent ---
    val context = LocalContext.current

    Column {
        Text(maid.name ?: "Unnamed Maid", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))

        // --- NEW: Clickable row for phone ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Mobile: ${maid.mobile ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
                Text("Address: ${maid.address ?: "N/A"}", style = MaterialTheme.typography.bodyLarge)
            }
            // --- NEW: Call Button ---
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${maid.mobile}")
                    }
                    context.startActivity(intent)
                },
                contentPadding = PaddingValues(12.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = "Call Maid")
            }
        }
    }
}

@Composable
fun PayrollSection(state: PayrollUIState) {
    Column {
        Text("Current Month's Payroll", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Box(
                modifier = Modifier.padding(16.dp).fillMaxWidth()
            ) {
                when (state) {
                    is PayrollUIState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is PayrollUIState.Error -> Text(state.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                    is PayrollUIState.Success -> PayrollDetails(state.payroll)
                }
            }
        }
    }
}

@Composable
fun PayrollDetails(payroll: PayrollResponse) {
    val currencyFormat = remember { DecimalFormat("₹ #,##0.00") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Billing Cycle: ${payroll.billingCycle.start ?: ""} to ${payroll.billingCycle.end ?: ""}", style = MaterialTheme.typography.bodySmall)
        Divider()
        PayrollRow("Total Salary:", currencyFormat.format(payroll.totalSalary))
        PayrollRow("Total Deductions:", currencyFormat.format(payroll.totalDeductions), isDeduction = true)
        Divider()
        PayrollRow("Final Payable Amount:", currencyFormat.format(payroll.payableAmount), isTotal = true)

        if (payroll.deductionsBreakdown.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Deductions Breakdown:", style = MaterialTheme.typography.titleSmall)
            payroll.deductionsBreakdown.forEach { item ->
                Text("  - ${item.taskName ?: "Unknown Task"}: ${item.missedDays} missed day(s) = -${currencyFormat.format(item.deductionAmount)}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun PayrollRow(label: String, amount: String, isDeduction: Boolean = false, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal)
        Spacer(modifier = Modifier.weight(1f))
        Text(text = if (isDeduction && !amount.contains("-")) "-$amount" else amount, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal, color = if (isDeduction) MaterialTheme.colorScheme.error else LocalContentColor.current)
    }
}


@Composable
fun TasksSection(
    tasks: List<Task>,
    onAddTask: () -> Unit,
    // --- NEW: Add onEditTask parameter ---
    onEditTask: (Task) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onAddTask) { Icon(Icons.Default.AddCircle, "Add Task") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text("No tasks assigned.")
        } else {
            // --- NEW: Pass onEdit handler to TaskItem ---
            tasks.forEach { task ->
                TaskItem(
                    task = task,
                    onEdit = { onEditTask(task) },
                    onDelete = { onDeleteTask(task.id) }
                )
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    // --- NEW: Add onEdit parameter ---
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.name ?: "", fontWeight = FontWeight.Bold)
            Text("₹${task.price} - ${task.frequency ?: ""}", style = MaterialTheme.typography.bodySmall)
        }
        // --- NEW: Edit Button ---
        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, "Edit Task")
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, "Delete Task")
        }
    }
}

@Composable
fun AttendanceSection(
    attendance: List<AttendanceRecord>,
    tasks: List<Task>,
    onMarkAttendance: (Task) -> Unit,
    onMarkAbsent: () -> Unit
) {
    Column {
        Text("Attendance", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        Text("Mark Today's Attendance:", style = MaterialTheme.typography.titleMedium)
        if (tasks.isEmpty()) {
            Text("Add a task first.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Present (OTP):", modifier = Modifier.align(Alignment.CenterVertically))
                tasks.forEach { task ->
                    Button(onClick = { onMarkAttendance(task) }) { Text(task.name ?: "") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onMarkAbsent) {
                Text("Manually Mark Absent")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Text("History (Last 5)", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        if (attendance.isEmpty()) {
            Text("No attendance records yet.")
        } else {
            val sortedAttendance = attendance.sortedByDescending {
                try {
                    val dateString = it.date ?: ""
                    val pattern = if (dateString.contains('.')) "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'" else "yyyy-MM-dd'T'HH:mm:ss'Z'"
                    val parser = SimpleDateFormat(pattern, Locale.getDefault())
                    parser.timeZone = TimeZone.getTimeZone("UTC")
                    parser.parse(dateString)?.time ?: 0L
                } catch (e: Exception) { 0L }
            }
            sortedAttendance.take(5).forEach { record -> AttendanceItem(record) }
        }
    }
}

@Composable
fun AttendanceItem(record: AttendanceRecord) {
    val formattedDate = remember(record.date) {
        try {
            val dateString = record.date ?: return@remember "Invalid Date"
            val pattern1 = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
            val pattern2 = "yyyy-MM-dd'T'HH:mm:ss'Z'"
            val parser = try { SimpleDateFormat(pattern1, Locale.getDefault()) } catch (e: IllegalArgumentException) { SimpleDateFormat(pattern2, Locale.getDefault()) }
            parser.timeZone = TimeZone.getTimeZone("UTC") // Input date is UTC
            val date = parser.parse(dateString)
            date?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it) } ?: dateString
        } catch (e: Exception) {
            Log.e(TAG, "Date parsing error for '${record.date}': ${e.message}")
            record.date ?: "Invalid Date"
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(formattedDate, modifier = Modifier.weight(1f))
        Text(record.taskName ?: "", fontWeight = FontWeight.Bold)
        if(record.status != "Present") {
            Text(" (${record.status ?: ""})", modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.error)
        }
    }
}


@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (name: String, price: Double, frequency: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add New Task", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name (e.g., Cooking)") }, singleLine = true)
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (e.g., 3000)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency (e.g., Daily)") }, singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onAddTask(name, price.toDoubleOrNull() ?: 0.0, frequency)
                    }) { Text("Add Task") }
                }
            }
        }
    }
}

// --- NEW DIALOG: EditTaskDialog ---
@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onEditTask: (taskId: String, name: String, price: Double, frequency: String) -> Unit
) {
    var name by remember { mutableStateOf(task.name ?: "") }
    var price by remember { mutableStateOf(task.price.toString()) }
    var frequency by remember { mutableStateOf(task.frequency ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Task", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name (e.g., Cooking)") }, singleLine = true)
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (e.g., 3000)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency (e.g., Daily)") }, singleLine = true)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        onEditTask(task.id, name, price.toDoubleOrNull() ?: 0.0, frequency)
                    }) { Text("Update Task") }
                }
            }
        }
    }
}


@Composable
fun AuthScreen(viewModel: AuthViewModel, onLoginSuccess: () -> Unit) {
    var showLogin by rememberSaveable { mutableStateOf(true) }
    val authResult by viewModel.authResult.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authResult) {
        if (authResult is AuthResult.Error) {
            val errorJsonString = (authResult as AuthResult.Error).message
            val displayMessage = try {
                JsonParser().parse(errorJsonString).asJsonObject.get("msg").asString
            } catch (e: Exception) {
                errorJsonString
            }
            Toast.makeText(context, displayMessage, Toast.LENGTH_LONG).show()
            viewModel.resetAuthResult()
        }
    }

    LaunchedEffect(viewModel.authState.collectAsStateWithLifecycle().value) {
        if (viewModel.authState.value is AuthState.Authenticated) {
            onLoginSuccess()
        }
    }


    Box(
        modifier = Modifier.fillMaxSize().background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    MaterialTheme.colorScheme.background
                )
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.CleaningServices,
                contentDescription = "App Logo",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(80.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "My Maid Manager",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))

            AnimatedContent(
                targetState = showLogin,
                label = "AuthFormAnimation",
                transitionSpec = {
                    (fadeIn(animationSpec = tween(150)) togetherWith
                            fadeOut(animationSpec = tween(150)))
                        .using(SizeTransform(clip = false))
                }
            ) { isLoginScreen ->
                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (isLoginScreen) {
                        LoginForm(
                            onSwitchToSignUp = { showLogin = false },
                            authResult = authResult,
                            onLoginClicked = { email, password ->
                                viewModel.loginUser(email, password)
                            }
                        )
                    } else {
                        SignUpForm(
                            onSwitchToLogin = { showLogin = true },
                            authResult = authResult,
                            onSignUpClicked = { name, email, password ->
                                viewModel.registerUser(name, email, password)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginForm(
    onSwitchToSignUp: () -> Unit,
    authResult: AuthResult,
    onLoginClicked: (String, String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val isLoading = authResult is AuthResult.Loading

    Column(
        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Welcome Back!", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, if (passwordVisible) "Hide password" else "Show password")
                }
            },
            enabled = !isLoading,
            singleLine = true
        )

        Button(
            onClick = { onLoginClicked(email, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = !isLoading && email.isNotEmpty() && password.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Login")
            }
        }

        TextButton(onClick = onSwitchToSignUp, enabled = !isLoading) {
            Text("Don't have an account? Sign Up")
        }
    }
}

@Composable
fun SignUpForm(
    onSwitchToLogin: () -> Unit,
    authResult: AuthResult,
    onSignUpClicked: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    val isLoading = authResult is AuthResult.Loading

    val passwordValidation = remember(password) { validatePassword(password) }
    val passwordsMatch = remember(password, confirmPassword) { password == confirmPassword }

    Column(
        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !isLoading,
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, null) }
            },
            isError = password.isNotEmpty() && passwordValidation.isNotEmpty(),
            enabled = !isLoading,
            singleLine = true
        )
        if (password.isNotEmpty() && passwordValidation.isNotEmpty()) {
            Text(
                text = passwordValidation.joinToString("\n"),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                val image = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) { Icon(imageVector = image, null) }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            enabled = !isLoading,
            singleLine = true
        )
        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Passwords do not match.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onSignUpClicked(name, email, password) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            enabled = passwordValidation.isEmpty() && passwordsMatch && email.isNotEmpty() && password.isNotEmpty() && name.isNotEmpty() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Sign Up")
            }
        }

        TextButton(onClick = onSwitchToLogin, enabled = !isLoading) {
            Text("Already have an account? Login")
        }
    }
}

private fun validatePassword(password: String): List<String> {
    val errors = mutableListOf<String>()
    if (password.length < 6) errors.add("Must be at least 6 characters long.")
    if (!password.any { it.isUpperCase() }) errors.add("Requires an uppercase letter.")
    if (!password.any { it.isDigit() }) errors.add("Requires a number.")
    if (password.all { it.isLetterOrDigit() }) errors.add("Requires a special character.")
    return errors
}

@Composable
fun AttendanceOtpDialog(
    maidName: String,
    viewModel: MaidViewModel,
    onDismiss: () -> Unit,
    onVerify: (String) -> Unit
) {
    val otpState by viewModel.otpState.collectAsStateWithLifecycle()
    var otpValue by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(otpState) {
        if (otpState is OtpState.OtpRequested) {
            Toast.makeText(context, "OTP has been sent to the maid's mobile.", Toast.LENGTH_LONG).show()
        }
        if (otpState is OtpState.Idle && (viewModel.maidDetailUIState.value is MaidDetailUIState.Success)) {
            if (otpValue.isNotEmpty()){
                Toast.makeText(context, "Attendance Marked Successfully!", Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Verify Attendance", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "Enter the 6-digit OTP sent to ${maidName ?: "Maid"}'s mobile.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { if (it.length <= 6) otpValue = it.filter { c -> c.isDigit() } },
                    label = { Text("OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                if (otpState is OtpState.Error) {
                    Text(
                        text = (otpState as OtpState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (otpState is OtpState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        TextButton(onClick = onDismiss) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onVerify(otpValue) },
                            enabled = otpValue.length == 6
                        ) {
                            Text("Verify")
                        }
                    }
                }
            }
        }
    }
}