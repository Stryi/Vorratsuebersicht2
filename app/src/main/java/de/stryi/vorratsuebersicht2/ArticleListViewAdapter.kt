package de.stryi.vorratsuebersicht2

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.recyclerview.widget.RecyclerView
import de.stryi.vorratsuebersicht2.database.Records.Article
import de.stryi.vorratsuebersicht2.database.Database
import de.stryi.vorratsuebersicht2.tools.AddToShoppingListDialog
import kotlinx.coroutines.*

class ArticleListViewAdapter(private val articles: List<Article>,
                             private val onItemClick: (articleId: Int) -> Unit) :
    RecyclerView.Adapter<ArticleListViewAdapter.ArticleViewHolder>()
{
    private val adapterScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    class ArticleViewHolder(view: View) : RecyclerView.ViewHolder(view)
    {
        val heading: TextView = view.findViewById(R.id.ArticleListView_Heading)
        val subHeading: TextView = view.findViewById(R.id.ArticleListView_SubHeading)
        val notesText:  TextView = view.findViewById(R.id.ArticleListView_Notes)
        val onShoppingList: ImageView = view.findViewById(R.id.ArticleListView_OnShoppingList)
        val shoppingQuantity: TextView = view.findViewById(R.id.ArticleListView_ShoppingQuantity)
        val isInStorage: ImageView = view.findViewById(R.id.ArticleListView_IsInStorage)
        val storageQuantity: TextView = view.findViewById(R.id.ArticleListView_StorageQuantity)
        val image: ImageView = view.findViewById(R.id.ArticleListView_Image)
        val option: TextView = view.findViewById(R.id.ArticleListView_Option)
        val articleListViewOption: TextView = view.findViewById(R.id.ArticleListView_Option)

        var minQuantity: Int?  = null
        var prefQuantity: Int? = null
        var imageJob: Job? = null
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.article_list_view, parent, false)
        return ArticleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        val article = articles[position]

        holder.itemView.tag = article.articleId
        holder.minQuantity = article.minQuantity
        holder.prefQuantity = article.prefQuantity

        holder.heading.text    = article.heading
        holder.subHeading.text = article.articleInfo
        holder.notesText.text  = article.notesText
        holder.notesText.visibility = if (article.notesText.isEmpty()) View.GONE else View.VISIBLE

        holder.onShoppingList.visibility   = if (article.isOnShoppingList) View.VISIBLE else View.GONE
        holder.shoppingQuantity.visibility = if (article.isOnShoppingList) View.VISIBLE else View.GONE
        holder.shoppingQuantity.text       = article.shoppingQuantityText

        holder.isInStorage.visibility     = if (article.isInStorage) View.VISIBLE else View.INVISIBLE
        holder.storageQuantity.visibility = if (article.isInStorage) View.VISIBLE else View.GONE
        holder.storageQuantity.text       = article.storageQuantityText

        // Mehr Platz für Notizen
        if (!article.isOnShoppingList && !article.isInStorage)
        {
            holder.onShoppingList.visibility   = View.GONE
            holder.shoppingQuantity.visibility = View.GONE
            holder.isInStorage.visibility      = View.GONE
            holder.storageQuantity.visibility  = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(article.articleId)
        }

        holder.itemView.setOnLongClickListener {
            showContextMenu(holder)
            true
        }

        holder.articleListViewOption.setOnClickListener {
            showContextMenu(holder)
        }

        // Bild asynchron laden
        holder.imageJob?.cancel()
        holder.image.setImageResource(R.drawable.photo_camera_24px)
        holder.image.alpha = 0.2f
        holder.image.setOnClickListener { }

        holder.imageJob = adapterScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                val articleImage = Database.getArticleImage(article.articleId, false)
                articleImage?.imageSmall?.let { bytes ->
                    try {
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    } catch (e: Exception) {
                        null
                    }
                }
            }

            if (isActive) {
                if (bitmap != null) {
                    holder.image.setImageBitmap(bitmap)
                    holder.image.alpha = 1.0f
                    holder.image.setOnClickListener {
                        val intent = Intent(holder.image.context, ArticleImageActivity::class.java)
                        intent.putExtra("ArticleId", article.articleId)
                        holder.image.context.startActivity(intent)
                    }
                } else {
                    holder.image.setImageResource(R.drawable.photo_camera_24px)
                    holder.image.alpha = 0.2f
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: ArticleViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holder.imageJob?.cancel()
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        adapterScope.cancel()
    }

    @SuppressLint("RestrictedApi")
    fun showContextMenu(holder: ArticleViewHolder) {
        val popupMenu = PopupMenu(holder.itemView.context, holder.option)
        popupMenu.menuInflater.inflate(R.menu.article_list_contextmenu, popupMenu.menu)
        
        if (popupMenu.menu is MenuBuilder) {
            (popupMenu.menu as MenuBuilder).setOptionalIconsVisible(true)
        }

        popupMenu.setOnMenuItemClickListener { menuItem: MenuItem ->
            val articleId = holder.itemView.tag as Int
            when (menuItem.itemId) {
                R.id.ArticleList_ContextMenu_Lagerbestand -> {
                    val storageDetails = Intent(holder.itemView.context, StorageItemInventoryActivity::class.java)
                    storageDetails.putExtra("ArticleId", articleId)
                    holder.itemView.context.startActivity(storageDetails)
                    true
                }
                R.id.ArticleList_ContextMenu_AufEinkaufszettel -> {
                    AddToShoppingListDialog.showDialog(
                        holder.itemView.context as Activity,
                        articleId,
                        holder.minQuantity,
                        holder.prefQuantity,
                        null)
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    override fun getItemCount(): Int = articles.size
}
