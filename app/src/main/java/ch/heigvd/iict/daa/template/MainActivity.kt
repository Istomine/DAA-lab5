package ch.heigvd.iict.daa.template

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MyImageAdapter
    private lateinit var imageCacheDir: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialiser le répertoire de cache
        val baseCacheDir = cacheDir // Récupère le répertoire de cache de l'application
        imageCacheDir = File(baseCacheDir, "images")
        if (!imageCacheDir.exists()) {
            imageCacheDir.mkdir()
        }

        // Générer la liste des URLs
        val imageUrls = generateImageUrls()

        // Configurer le RecyclerView et l'adaptateur
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 3) // Nombre de colonnes
        adapter = MyImageAdapter(imageUrls, imageCacheDir, lifecycleScope)
        recyclerView.adapter = adapter
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_cache -> {
                clearCache()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Fonction pour vider le cache
    private fun clearCache() {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                imageCacheDir.listFiles()?.forEach { it.delete() }
            }
            adapter.notifyDataSetChanged()
        }
    }

    // Générer la liste des URLs
    private fun generateImageUrls(): List<String> {
        val baseUrl = "https://daa.iict.ch/images/"
        return (1..10000).map { "$baseUrl$it.jpg" }
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.clearAllJobs() // Nettoyer les coroutines de l'adaptateur
    }
}
