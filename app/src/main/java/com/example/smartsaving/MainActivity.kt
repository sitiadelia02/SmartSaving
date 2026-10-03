package com.example.smartsaving

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

// ----------------------------------------------------
// PALET WARNA DARI GAMBAR KAMU
// ----------------------------------------------------
val ColorCoral = Color(0xFFFD8A8A)
val ColorCream = Color(0xFFF1F7B5)
val ColorTeal = Color(0xFFA8D1D1)
val ColorPeriwinkle = Color(0xFF9EA1D4)
val ColorBgLight = Color(0xFFFAFAFA)
val ColorTextDark = Color(0xFF2D3748)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainScreenWithBottomNav()
        }
    }
}

// ----------------------------------------------------
// 1. DATA MODEL & VIEWMODEL
// ----------------------------------------------------
data class CategoryItem(
    val name: String,
    var allocated: Double,
    var spent: Double = 0.0
)

data class ExpenseTransaction(
    val id: String = UUID.randomUUID().toString(),
    val categoryName: String,
    val amount: Double,
    val note: String
)

class SmartSavingViewModel : ViewModel() {
    var totalMonthlyBudget by mutableStateOf(0.0)
        private set

    var categories = mutableStateListOf<CategoryItem>()
    var transactions = mutableStateListOf<ExpenseTransaction>()

    val totalAllocated: Double
        get() = categories.sumOf { it.allocated }

    val totalSpent: Double
        get() = categories.sumOf { it.spent }

    fun setBudget(amount: Double) {
        totalMonthlyBudget = amount
    }

    fun addOrUpdateCategory(name: String, amount: Double) {
        val existingIndex = categories.indexOfFirst { it.name.equals(name, ignoreCase = true) }
        if (existingIndex != -1) {
            val existing = categories[existingIndex]
            categories[existingIndex] = existing.copy(allocated = existing.allocated + amount)
        } else {
            categories.add(CategoryItem(name = name, allocated = amount))
        }
    }

    fun recordExpense(categoryName: String, amount: Double, note: String) {
        val cat = categories.find { it.name == categoryName }
        if (cat != null) {
            val index = categories.indexOf(cat)
            categories[index] = cat.copy(spent = cat.spent + amount)
            transactions.add(0, ExpenseTransaction(categoryName = categoryName, amount = amount, note = note))
        }
    }

    fun deleteTransaction(transaction: ExpenseTransaction) {
        val cat = categories.find { it.name == transaction.categoryName }
        if (cat != null) {
            val index = categories.indexOf(cat)
            val newSpent = (cat.spent - transaction.amount).coerceAtLeast(0.0)
            categories[index] = cat.copy(spent = newSpent)
        }
        transactions.remove(transaction)
    }

    fun deleteCategory(categoryName: String) {
        categories.removeAll { it.name == categoryName }
        transactions.removeAll { it.categoryName == categoryName }
    }

    fun deleteAllCategories() {
        totalMonthlyBudget = 0.0
        categories.clear()
        transactions.clear()
    }
}

fun Double.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace(",00", "")
}

// ----------------------------------------------------
// 2. NAVIGASI UTAMA
// ----------------------------------------------------
enum class Screen(val title: String) {
    Home("Home"),
    Alokasi("Alokasi"),
    Catat("Catat"),
    Laporan("Laporan")
}

@Composable
fun MainScreenWithBottomNav(
    savingViewModel: SmartSavingViewModel = viewModel(),
    onNavigateToAlokasi: () -> Unit = {}
) {
    var currentScreen by remember { mutableStateOf(Screen.Home) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                Screen.values().forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Text(
                                text = when (screen) {
                                    Screen.Home -> "🏠"
                                    Screen.Alokasi -> "➕"
                                    Screen.Catat -> "📝"
                                    Screen.Laporan -> "📊"
                                },
                                fontSize = 18.sp
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ColorTextDark,
                            selectedTextColor = ColorTextDark,
                            indicatorColor = ColorCream
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (currentScreen) {
                Screen.Home -> DashboardScreen(
                    viewModel = savingViewModel,
                    onNavigateToAlokasi = { currentScreen = Screen.Alokasi }
                )
                Screen.Alokasi -> AlokasiScreen(savingViewModel)
                Screen.Catat -> CatatScreen(savingViewModel)
                Screen.Laporan -> LaporanScreen(savingViewModel)
            }
        }
    }
}

// ----------------------------------------------------
// 3. TAB HOME (TAMPILAN BARU SESUAI PALET)
// ----------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: SmartSavingViewModel,
    onNavigateToAlokasi: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgLight)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // --- HEADER ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("WisePocket", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = ColorTextDark)
                Text("Pocket Your Money Wisely", fontSize = 13.sp, color = Color.Gray)
            }

            // Tombol Reset dalam Ikon Gear Pengaturan biar rapi
            IconButton(
                onClick = {
                    viewModel.deleteAllCategories()
                    Toast.makeText(context, "Semua data berhasil di-reset!", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("⚙️", fontSize = 20.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- BANNER UTAMA (Coral & Periwinkle Gradient) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = listOf(ColorCoral, ColorPeriwinkle)
                        )
                    )
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "TOTAL BUDGET",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = viewModel.totalAllocated.toRupiah(),
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    Text(
                        text = "Total Uang: ${viewModel.totalMonthlyBudget.toRupiah()}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // --- QUICK STATS ---
        Text("Quick Stats", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextDark)
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Stat 1: Sisa Budget (Warna Cream)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorCream)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💳", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sisa Budget:", fontSize = 11.sp, color = ColorTextDark)
                    Text(
                        text = (viewModel.totalAllocated - viewModel.totalSpent).toRupiah(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextDark
                    )
                }
            }

            // Stat 2: Total Terpakai (Warna Teal)
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ColorTeal)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("💸", fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Terpakai:", fontSize = 11.sp, color = ColorTextDark)
                    Text(
                        text = viewModel.totalSpent.toRupiah(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorTextDark
                    )
                }
            }
        }

        // --- DAFTAR KATEGORI / EMPTY STATE ---
        Text("Daftar Kategori", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 12.dp))

        if (viewModel.categories.isEmpty()) {
            // Tampilan Empty State Modern
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("👛", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Belum Ada Pos Anggaran", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ColorTextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Silakan tambahkan kategori pertama Anda dari tab Alokasi.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateToAlokasi,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("+ Tambah Kategori", color = ColorTextDark, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            val cardColors = listOf(ColorCream, ColorTeal, ColorPeriwinkle)

            viewModel.categories.forEachIndexed { index, cat ->
                val percentage = if (viewModel.totalMonthlyBudget > 0) {
                    ((cat.allocated / viewModel.totalMonthlyBudget) * 100).toInt()
                } else 0

                val bg = cardColors[index % cardColors.size]

                CategoryCard(
                    title = cat.name,
                    amount = "Terkumpul: ${cat.allocated.toRupiah()} (Sisa: ${(cat.allocated - cat.spent).toRupiah()})",
                    percentage = "$percentage%",
                    bgColor = bg,
                    textColor = ColorTextDark,
                    onDeleteClick = {
                        viewModel.deleteCategory(cat.name)
                        Toast.makeText(context, "${cat.name} dihapus!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// ----------------------------------------------------
// 4. TAB ALOKASI
// ----------------------------------------------------
@Composable
fun AlokasiScreen(viewModel: SmartSavingViewModel) {
    var inputBudget by remember { mutableStateOf("") }
    var categoryNameInput by remember { mutableStateOf("") }
    var categoryAmountInput by remember { mutableStateOf("") }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgLight)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Alokasi Dana", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("1. Input Total Uang", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorTextDark)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = inputBudget,
                    onValueChange = { inputBudget = it },
                    label = { Text("Total Uang (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        val amount = inputBudget.toDoubleOrNull()
                        if (amount != null) {
                            viewModel.setBudget(amount)
                            Toast.makeText(context, "Uang Bulan Ini Disimpan!", Toast.LENGTH_SHORT).show()
                            inputBudget = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTeal)
                ) {
                    Text("Simpan Total Uang", color = ColorTextDark, fontWeight = FontWeight.Bold)
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("2. Bagi Uang ke Kategori", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorTextDark)
                Text(
                    text = "Sisa Uang Belum Dialokasikan: ${(viewModel.totalMonthlyBudget - viewModel.totalAllocated).toRupiah()}",
                    fontSize = 12.sp,
                    color = ColorCoral,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                OutlinedTextField(
                    value = categoryNameInput,
                    onValueChange = { categoryNameInput = it },
                    label = { Text("Nama Kategori (mis: Belanja Harian)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = categoryAmountInput,
                    onValueChange = { categoryAmountInput = it },
                    label = { Text("Nominal (Rp)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        val amount = categoryAmountInput.toDoubleOrNull()
                        if (categoryNameInput.isNotBlank() && amount != null && amount > 0) {
                            viewModel.addOrUpdateCategory(categoryNameInput.trim(), amount)
                            Toast.makeText(context, "Kategori '${categoryNameInput}' ditambahkan!", Toast.LENGTH_SHORT).show()
                            categoryNameInput = ""
                            categoryAmountInput = ""
                        } else {
                            Toast.makeText(context, "Isi nama & nominal dengan benar!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorCoral)
                ) {
                    Text("Tambahkan Kategori", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. TAB CATAT PENGELUARAN
// ----------------------------------------------------
@Composable
fun CatatScreen(viewModel: SmartSavingViewModel) {
    var selectedCategory by remember { mutableStateOf("") }
    var inputExpense by remember { mutableStateOf("") }
    var inputNote by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(viewModel.categories.size) {
        if (viewModel.categories.isNotEmpty() && selectedCategory.isEmpty()) {
            selectedCategory = viewModel.categories.first().name
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgLight)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Catat Pengeluaran", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp))

        if (viewModel.categories.isEmpty()) {
            Text("Belum ada kategori. Silakan buat dulu di tab Alokasi.", color = Color.Gray, fontSize = 14.sp)
        } else {
            Text("Pilih Kategori:", fontSize = 12.sp, color = Color.Gray)
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                viewModel.categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.name,
                        onClick = { selectedCategory = cat.name },
                        label = { Text(cat.name, fontSize = 12.sp) },
                        modifier = Modifier.padding(end = 6.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorCream,
                            selectedLabelColor = ColorTextDark
                        )
                    )
                }
            }

            OutlinedTextField(
                value = inputExpense,
                onValueChange = { inputExpense = it },
                label = { Text("Jumlah Pengeluaran (Rp)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            OutlinedTextField(
                value = inputNote,
                onValueChange = { inputNote = it },
                label = { Text("Catatan / Keterangan (contoh: Beli Kopi)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    val amount = inputExpense.toDoubleOrNull()
                    if (amount != null && amount > 0 && selectedCategory.isNotEmpty()) {
                        val noteText = if (inputNote.isBlank()) "Pengeluaran" else inputNote.trim()
                        viewModel.recordExpense(selectedCategory, amount, noteText)
                        Toast.makeText(context, "Berhasil Dicatat!", Toast.LENGTH_SHORT).show()
                        inputExpense = ""
                        inputNote = ""
                    } else {
                        Toast.makeText(context, "Masukkan jumlah pengeluaran!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTeal)
            ) {
                Text("Simpan Pengeluaran", color = ColorTextDark, fontWeight = FontWeight.Bold)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Riwayat Pengeluaran", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 10.dp))

            if (viewModel.transactions.isEmpty()) {
                Text("Belum ada riwayat pengeluaran.", color = Color.Gray, fontSize = 12.sp)
            } else {
                viewModel.transactions.forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.note, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorTextDark)
                                Text("Kategori: ${item.categoryName}", fontSize = 11.sp, color = Color.Gray)
                                Text("-${item.amount.toRupiah()}", fontWeight = FontWeight.Bold, color = ColorCoral, fontSize = 13.sp)
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteTransaction(item)
                                    Toast.makeText(context, "Riwayat dihapus!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("🗑️", fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 6. TAB LAPORAN
// ----------------------------------------------------
@Composable
fun LaporanScreen(viewModel: SmartSavingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBgLight)
            .padding(20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Laporan Keuangan", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ringkasan Total", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorTextDark)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Uang:")
                    Text(viewModel.totalMonthlyBudget.toRupiah(), fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Dialokasikan:")
                    Text(viewModel.totalAllocated.toRupiah(), fontWeight = FontWeight.Bold, color = ColorCoral)
                }
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Total Terpakai:")
                    Text(viewModel.totalSpent.toRupiah(), fontWeight = FontWeight.Bold, color = ColorPeriwinkle)
                }
            }
        }

        Text("Rincian per Kategori", fontWeight = FontWeight.Bold, color = ColorTextDark, modifier = Modifier.padding(bottom = 12.dp))

        if (viewModel.categories.isEmpty()) {
            Text("Belum ada kategori.", color = Color.Gray, fontSize = 12.sp)
        } else {
            viewModel.categories.forEach { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorCream)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorTextDark)
                        Text("Alokasi: ${cat.allocated.toRupiah()} | Terpakai: ${cat.spent.toRupiah()}", fontSize = 12.sp, color = ColorTextDark)
                    }
                }
            }
        }
    }
}

// COMPONENT CARD CATEGORY DI HOME
@Composable
fun CategoryCard(
    title: String,
    amount: String,
    percentage: String,
    bgColor: Color,
    textColor: Color,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                Text(amount, fontSize = 11.sp, color = textColor, modifier = Modifier.padding(top = 4.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(percentage, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor)
                IconButton(onClick = onDeleteClick) {
                    Text("❌", fontSize = 12.sp)
                }
            }
        }
    }
}