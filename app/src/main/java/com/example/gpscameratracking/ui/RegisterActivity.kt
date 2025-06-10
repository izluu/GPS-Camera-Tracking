package com.example.gpscameratracking.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.gpscameratracking.data.reponsitory.UserRepository
import com.example.gpscameratracking.databinding.ActivityRegisterBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    private fun initView() {
        binding.tvGoToLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmailRegister.text
            val password = binding.etPasswordRegister.text
            val confirmPassword = binding.etConfirmPassword.text
            if (email.isNullOrEmpty() || password.isNullOrEmpty() || confirmPassword.isNullOrEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.toString() != confirmPassword.toString()) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            registerVM.register(email.toString(), password.toString())


        }
        lifecycleScope.launch {
            registerVM.registerState.collectLatest { result ->
                result?.onFailure {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thất bại", Toast.LENGTH_SHORT)
                        .show()
                }?.onSuccess {
                    Toast.makeText(this@RegisterActivity, "Đăng ký thành công", Toast.LENGTH_SHORT)
                        .show()
                    val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }

            }
        }
    }

    private val registerVM: RegisterViewModel by viewModels {
        object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return RegisterViewModel(UserRepository()) as T
            }
        }
    }
}