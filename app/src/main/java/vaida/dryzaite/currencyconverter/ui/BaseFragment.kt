package vaida.dryzaite.currencyconverter.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.toolbar.view.*
import vaida.dryzaite.currencyconverter.BR
import vaida.dryzaite.currencyconverter.R
import vaida.dryzaite.currencyconverter.util.NavigationSettings
import vaida.dryzaite.currencyconverter.util.setStatusBar

abstract class BaseFragment <VM : ViewModel, T : ViewDataBinding> : Fragment() {
    abstract val navigationSettings: NavigationSettings?
    abstract val layoutId: Int

    lateinit var binding: T
    lateinit var viewModel: VM
    private lateinit var mainView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val navSettings = navigationSettings
        requireActivity().window.setStatusBar(requireContext())

        if (navSettings == null) {
            binding = DataBindingUtil.inflate(inflater, layoutId, container, false)
            mainView = binding.root
        } else {
            // TODO switch to VB
            val baseFragment = inflater.inflate(R.layout.toolbar, container, false) as ViewGroup
            binding = DataBindingUtil.inflate(inflater, layoutId, baseFragment, false)
            baseFragment.toolbarFragmentContent.removeAllViews()
            baseFragment.toolbarFragmentContent.addView(binding.root)
            mainView = baseFragment

            navSettings.title?.let {
                baseFragment.toolbarTitle.text = it
            }
        }

        viewModel = ViewModelProvider(this).get(getViewModelClass())
        binding.setVariable(BR.viewModel, viewModel)
        setupUI()

        return mainView
    }

    abstract fun getViewModelClass(): Class<VM>
    abstract fun setupUI()
}