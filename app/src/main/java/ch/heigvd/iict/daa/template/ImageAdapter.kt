package ch.heigvd.iict.daa.template

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import java.io.File
import java.net.URL

class MyImageAdapter(
    private val images: List<String>,
    private val cacheDir: File,
    private val coroutineScope: LifecycleCoroutineScope // Scope pour gérer les coroutines
) : RecyclerView.Adapter<MyImageAdapter.MyViewHolder>() {

    private val jobMap = mutableMapOf<Int, Job>() // Stocker les jobs pour chaque position

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val imageUrl = images[position]
        val fileName = imageUrl.hashCode().toString() + ".png"
        val cacheFile = File(cacheDir, fileName)

        // Annuler une coroutine existante pour cette position
        jobMap[position]?.cancel()

        // Démarrer une nouvelle coroutine pour charger l'image
        val job = coroutineScope.launch {
            withContext(Dispatchers.Main) {
                holder.progressBar.visibility = View.VISIBLE
                holder.imageView.setImageBitmap(null)
            }

            val bitmap = withContext(Dispatchers.IO) {
                loadImage(cacheFile, imageUrl)
            }

            withContext(Dispatchers.Main) {
                holder.progressBar.visibility = View.GONE
                if (bitmap != null) {
                    holder.imageView.setImageBitmap(bitmap)
                } else {
                    holder.imageView.setImageResource(R.drawable.placeholder)
                }
            }
        }

        jobMap[position] = job
    }

    override fun getItemCount(): Int = images.size

    override fun onViewRecycled(holder: MyViewHolder) {
        super.onViewRecycled(holder)
        // Annuler les coroutines associées à une vue recyclée
        jobMap[holder.adapterPosition]?.cancel()
        jobMap.remove(holder.adapterPosition)
    }

    // Vérifier et charger l'image
    private suspend fun loadImage(cacheFile: File, imageUrl: String): Bitmap? {
        val isCacheValid = cacheFile.exists() && isCacheRecent(cacheFile, 5)
        return if (isCacheValid) {
            BitmapFactory.decodeFile(cacheFile.absolutePath)
        } else {
            downloadAndCacheImage(cacheFile, imageUrl)
        }
    }

    // Vérifier si le cache est récent (en minutes)
    private fun isCacheRecent(file: File, maxAgeMinutes: Int): Boolean {
        val lastModified = file.lastModified()
        val maxAgeMillis = maxAgeMinutes * 60 * 1000
        return System.currentTimeMillis() - lastModified < maxAgeMillis
    }

    // Télécharger l'image et la mettre en cache
    private suspend fun downloadAndCacheImage(cacheFile: File, imageUrl: String): Bitmap? {
        return try {
            val bitmap = withContext(Dispatchers.IO) {
                val url = URL(imageUrl)
                BitmapFactory.decodeStream(url.openConnection().getInputStream())
            }
            cacheFile.outputStream().use { out ->
                bitmap?.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun clearAllJobs() {
        jobMap.values.forEach { it.cancel() }
        jobMap.clear()
    }
}
