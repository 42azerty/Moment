package com.example.moments


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.moments.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MomentAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val CAMERA_PERMISSION_CODE = 100
    private val CAMERA_REQUEST_CODE = 101
    private lateinit var searchView: SearchView
    private var originalList: List<Moment> = listOf()
    companion object {
        private const val FILEPATH = "moments.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.fab.setOnClickListener {
            NewMoment().show(supportFragmentManager, null)
        }

        adapter = MomentAdapter(this)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.itemAnimator = DefaultItemAnimator()

        adapter.momentList = retrieveMoments()
        adapter.notifyItemRangeInserted(0, adapter.momentList.size)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE)
        }
        originalList = adapter.momentList.toList()

    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)
        searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterMoments(newText)
                return true
            }
        })

        return true
    }

    private fun filterMoments(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.momentList = originalList.toMutableList()
        } else {
            adapter.momentList = originalList.filter {
                it.title.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        adapter.notifyDataSetChanged()
    }
    override fun onStart() {
        super.onStart()

        val nightThemeSelected = sharedPreferences.getBoolean("theme", false)
        if (nightThemeSelected) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val showDividingLines = sharedPreferences.getBoolean("dividingLines", false)
        if (showDividingLines) binding.recyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        else if (binding.recyclerView.itemDecorationCount > 0) binding.recyclerView.removeItemDecorationAt(0)
    }



    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun createNewMoment(moment: Moment) {
        adapter.momentList.add(moment)
        adapter.notifyItemInserted(adapter.momentList.size - 1)
        sortMoments()
        saveMoment()
    }

    fun deleteMoment(index: Int) {
        adapter.momentList.removeAt(index)
        adapter.notifyItemRemoved(index)
        saveMoment()
    }

    fun showMoment(index: Int) {
        val dialog = ShowMoment(adapter.momentList[index], index)
        dialog.show(supportFragmentManager, null)
    }

    private fun saveMoment() {
        val moments = adapter.momentList
        val gson = GsonBuilder().create()
        val jsonMoments = gson.toJson(moments)


        val outputStream = openFileOutput(FILEPATH, Context.MODE_PRIVATE)
        OutputStreamWriter(outputStream).use { writer ->
            writer.write(jsonMoments)
        }
    }
    internal fun sortMoments() {
        val sortOrder = PreferenceManager.getDefaultSharedPreferences(this)
            .getString("sort_order", "date") ?: "date"

        adapter.momentList = when (sortOrder) {
            "date" -> adapter.momentList.sortedByDescending { it.date }
            "title" -> adapter.momentList.sortedBy { it.title.uppercase() }
            "location" -> adapter.momentList.sortedBy { it.address ?: "" }
            else -> adapter.momentList
        }.toMutableList()

        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        sortMoments()
    }
    private fun retrieveMoments(): MutableList<Moment> {
        val momentList = mutableListOf<Moment>()
        if (getFileStreamPath(FILEPATH).isFile) {
            val fileInput = openFileInput(FILEPATH)
            BufferedReader(InputStreamReader(fileInput)).use { reader ->
                val stringBuilder = StringBuilder()
                for (line in reader.readLine()) stringBuilder.append(line)

                if (stringBuilder.isNotEmpty()){
                    val listType = object : TypeToken<List<Moment>>() {}.type
                    momentList.addAll(Gson().fromJson(stringBuilder.toString(), listType))
                }
            }
        }
        return momentList.toMutableList().also { sortMoments() }
    }
}