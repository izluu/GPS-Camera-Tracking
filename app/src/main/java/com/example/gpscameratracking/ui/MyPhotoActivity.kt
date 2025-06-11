package com.example.gpscameratracking.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.gpscameratracking.data.adapter.Adapter
import com.example.gpscameratracking.databinding.ActivityMyPhotoBinding

class MyPhotoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMyPhotoBinding
    private lateinit var adapter: Adapter
    private lateinit var viewModel: MyPhotoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMyPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        adapter = Adapter(this, listOf())
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 3)
        viewModel = ViewModelProvider(this)[MyPhotoViewModel::class.java]
        viewModel.fetchUrls()

        viewModel.urls.observe(this) { urls ->
            adapter.setData(urls)
        }
        adapter.onSelectionChanged = { count ->
            if (count > 0) {
                binding.selectionHeader.visibility = android.view.View.VISIBLE
                binding.txtSelectedCount.text = "$count mục đã chọn"
                binding.btnCloseSelection.visibility = android.view.View.VISIBLE
            } else {
                binding.selectionHeader.visibility = android.view.View.GONE
                binding.btnCloseSelection.visibility = android.view.View.GONE
            }
        }
        binding.btnBack.setOnClickListener {
            val intent = android.content.Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnCloseSelection.setOnClickListener {
            adapter.clearSelection()
            binding.selectionHeader.visibility = android.view.View.GONE
        }
        binding.btnDownload.setOnClickListener {
            viewModel.download(this, adapter.getSelectedUrls())
            adapter.clearSelection()
            binding.selectionHeader.visibility = android.view.View.GONE
        }
        viewModel.downloadResult.observe(this) { result ->
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        }
    }
}
