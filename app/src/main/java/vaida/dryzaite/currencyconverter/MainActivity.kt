package vaida.dryzaite.currencyconverter

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import vaida.dryzaite.currencyconverter.databinding.ActivityMainBinding
import vaida.dryzaite.currencyconverter.util.setStatusBar

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        binding = ActivityMainBinding.inflate(layoutInflater)
        window.setStatusBar(this)

        setContentView(binding.root)
    }
}