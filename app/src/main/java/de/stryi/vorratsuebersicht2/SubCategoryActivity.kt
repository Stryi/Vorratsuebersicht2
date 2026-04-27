package de.stryi.vorratsuebersicht2

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import de.stryi.vorratsuebersicht2.database.Database
import de.stryi.vorratsuebersicht2.databinding.SubCategoryActivityBinding

class SubCategoryActivity : AppCompatActivity() {

    private lateinit var binding: SubCategoryActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SubCategoryActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        this.setSupportActionBar(binding.SubCategoryActivityAppBar)
        binding.SubCategoryActivityAppBar.setNavigationOnClickListener { finish() }

        val category = intent.getStringExtra("Category")

        this.title = category

        val subCategories: MutableList<String> = mutableListOf()
        subCategories.add(this.resources.getString(R.string.AnySubCategory_ItemEntry))
        subCategories.addAll(Database.getSubcategoriesOf(category, true))

        val listAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, subCategories)
        binding.SubCategoryList.adapter = listAdapter

        binding.SubCategoryList.setOnItemClickListener { _, _, position, _ ->

            val intent = Intent(this, StorageItemListActivity::class.java)

            intent.putExtra("Category",    category)

            if (position > 0)
            {
                intent.putExtra("SubCategory", subCategories[position])
            }

            startActivity(intent)
        }
    }
}