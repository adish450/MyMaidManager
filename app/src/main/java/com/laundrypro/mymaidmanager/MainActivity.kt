package com.laundrypro.mymaidmanager

import android.os.Bundle
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.gson.JsonParser
import com.laundrypro.mymaidmanager.models.AttendanceRecord
import com.laundrypro.mymaidmanager.models.Maid
import com.laundrypro.mymaidmanager.models.Task
import com.laundrypro.mymaidmanager.ui.theme.MyMaidManagerTheme
import com.laundrypro.mymaidmanager.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*

// Define navigation routes
sealed class Screen(val route: String) {
    object Auth : Screen("auth_screen")
    object MainList : Screen("main_list_screen")
    object MaidDetail : Screen("maid_detail_screen/{maidId}") {
        fun createRoute(maidId: String) = "maid_detail_screen/$maidId"
    }
}

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

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
fun AppNavigator(navController: NavHostController, authViewModel: AuthViewModel) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        composable(Screen.Auth.route) {
            AuthScreen(authViewModel, onLoginSuccess = {
                navController.navigate(Screen.MainList.route) {
                    popUpTo(Screen.Auth.route) { inclusive = true }
                }
            })
        }
        composable(Screen.MainList.route) {
            MainListScreen(
                onLogout = {
                    authViewModel.logout()
                },
                onMaidClicked = { maidId ->
                    navController.navigate(Screen.MaidDetail.createRoute(maidId))
                }
            )
        }
        composable(Screen.MaidDetail.route) { backStackEntry ->
            // THE FIX IS HERE: Add a null check before calling the screen
            val maidId = backStackEntry.arguments?.getString("maidId")
            if (maidId != null) {
                MaidDetailScreen(maidId = maidId, onNavigateUp = { navController.navigateUp() })
            } else {
                // Optional: Handle the error case, e.g., navigate back or show an error
                // For now, we do nothing, which prevents the crash.
            }
        }
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    LaunchedEffect(authState) {
        if (authState is AuthState.Unauthenticated) {
            navController.navigate(Screen.Auth.route) { popUpTo(0) }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainListScreen(onLogout: () -> Unit, onMaidClicked: (String) -> Unit) {
    val maidViewModel: MaidViewModel = viewModel()
    val uiState by maidViewModel.maidListUIState.collectAsStateWithLifecycle()
    var showAddMaidDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        maidViewModel.fetchMaids()
    }

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
                            items(state.maids) { maid ->
                                MaidCard(maid, onClick = { onMaidClicked(maid.id) })
                            }
                        }
                    }
                }
                is MaidListUIState.Error -> Text(state.message, modifier = Modifier.align(Alignment.Center))
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
            Text(maid.name, style = MaterialTheme.typography.titleMedium)
            Text(maid.mobile, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(maid.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaidDetailScreen(maidId: String, onNavigateUp: () -> Unit) {
    val viewModel: MaidViewModel = viewModel()
    val uiState by viewModel.maidDetailUIState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = maidId) {
        viewModel.fetchMaidDetails(maidId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Maid Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when (val state = uiState) {
                is MaidDetailUIState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                is MaidDetailUIState.Success -> MaidDetailContent(maid = state.maid, viewModel = viewModel)
                is MaidDetailUIState.Error -> Text(state.message, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun MaidDetailContent(maid: Maid, viewModel: MaidViewModel) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAttendanceDialog by remember { mutableStateOf<Task?>(null) }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { name, price, freq ->
                viewModel.addTask(maid.id, name, price, freq)
                showAddTaskDialog = false
            }
        )
    }

    showAttendanceDialog?.let { task ->
        AttendanceOtpDialog(
            maidName = maid.name,
            viewModel = viewModel,
            onDismiss = {
                viewModel.resetOtpState()
                showAttendanceDialog = null
            },
            onVerify = { otp ->
                viewModel.verifyOtpForAttendance(maid.id, otp, task.name)
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { MaidInfoSection(maid) }
        item {
            TasksSection(
                tasks = maid.tasks,
                onAddTask = { showAddTaskDialog = true },
                onDeleteTask = { taskId -> viewModel.deleteTask(maid.id, taskId) }
            )
        }
        item {
            AttendanceSection(
                attendance = maid.attendance,
                tasks = maid.tasks,
                onMarkAttendance = { task ->
                    viewModel.requestOtpForAttendance(maid.id)
                    showAttendanceDialog = task
                }
            )
        }
    }
}

@Composable
fun MaidInfoSection(maid: Maid) {
    Column {
        Text(maid.name, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Mobile: ${maid.mobile}", style = MaterialTheme.typography.bodyLarge)
        Text("Address: ${maid.address}", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun TasksSection(tasks: List<Task>, onAddTask: () -> Unit, onDeleteTask: (String) -> Unit) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tasks", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onAddTask) { Icon(Icons.Default.AddCircle, "Add Task") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (tasks.isEmpty()) {
            Text("No tasks assigned.")
        } else {
            tasks.forEach { task -> TaskItem(task, onDelete = { onDeleteTask(task.id) }) }
        }
    }
}

@Composable
fun TaskItem(task: Task, onDelete: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(task.name, fontWeight = FontWeight.Bold)
            Text("₹${task.price} - ${task.frequency}", style = MaterialTheme.typography.bodySmall)
        }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "Delete Task") }
    }
}

@Composable
fun AttendanceSection(attendance: List<AttendanceRecord>, tasks: List<Task>, onMarkAttendance: (Task) -> Unit) {
    Column {
        Text("Attendance", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Mark attendance for:", style = MaterialTheme.typography.titleMedium)
        if (tasks.isEmpty()) {
            Text("Add a task to mark attendance.", style = MaterialTheme.typography.bodySmall)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tasks.forEach { task ->
                    Button(onClick = { onMarkAttendance(task) }) { Text(task.name) }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("History (Last 5)", style = MaterialTheme.typography.titleMedium)
        if (attendance.isEmpty()) {
            Text("No attendance records yet.")
        } else {
            attendance.take(5).forEach { record -> AttendanceItem(record) }
        }
    }
}

@Composable
fun AttendanceItem(record: AttendanceRecord) {
    val formattedDate = remember(record.date) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            parser.timeZone = TimeZone.getTimeZone("UTC")
            val date = parser.parse(record.date)
            date?.let { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(it) } ?: record.date
        } catch (e: Exception) {
            record.date // Fallback
        }
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(formattedDate, modifier = Modifier.weight(1f))
        Text(record.taskName, fontWeight = FontWeight.Bold)
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
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Add New Task", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Task Name (e.g., Cooking)") })
                OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price (e.g., 3000)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                OutlinedTextField(value = frequency, onValueChange = { frequency = it }, label = { Text("Frequency (e.g., Daily)") })
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

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    LaunchedEffect(authState){
        if(authState is AuthState.Authenticated){
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
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
        modifier = Modifier.padding(24.dp),
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
            enabled = !isLoading
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
            enabled = !isLoading
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
        modifier = Modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Create Account", style = MaterialTheme.typography.titleLarge)

        OutlinedTextField(
            value = name, onValueChange = { name = it },
            label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = !isLoading
        )

        OutlinedTextField(
            value = email, onValueChange = { email = it },
            label = { Text("Email") }, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), enabled = !isLoading
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
            enabled = !isLoading
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
            enabled = !isLoading
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
fun AddMaidDialog(
    onDismiss: () -> Unit,
    onAddMaid: (name: String, mobile: String, address: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    val isButtonEnabled = name.isNotBlank() && mobile.isNotBlank() && address.isNotBlank()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Add New Maid", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onAddMaid(name, mobile, address) }, enabled = isButtonEnabled) { Text("Add") }
                }
            }
        }
    }
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
            Toast.makeText(context, "OTP sent to maid's mobile (check server console)", Toast.LENGTH_LONG).show()
        }
        if (otpState is OtpState.Idle && (viewModel.maidDetailUIState.value is MaidDetailUIState.Success || viewModel.maidListUIState.value is MaidListUIState.Success)) {
            Toast.makeText(context, "Attendance Marked!", Toast.LENGTH_SHORT).show()
            onDismiss()
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
                    text = "Enter the 6-digit OTP sent to ${maidName}'s mobile.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = otpValue,
                    onValueChange = { if (it.length <= 6) otpValue = it.filter { c -> c.isDigit() } },
                    label = { Text("OTP") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
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