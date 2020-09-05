package com.nex3z.flowers.modelmaker.ui.settings

import android.os.Bundle
import android.view.View
import androidx.activity.addCallback
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.nex3z.flowers.modelmaker.BuildConfig
import com.nex3z.flowers.modelmaker.R
import com.nex3z.flowers.modelmaker.ui.camera.ClassifierViewModel
import kotlinx.android.synthetic.main.fragment_permission.*

class SettingsFragment : PreferenceFragmentCompat() {

    private lateinit var classifierViewModel: ClassifierViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().navigate(R.id.action_settings_to_camera)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        classifierViewModel = ViewModelProvider(requireActivity())
                .get(ClassifierViewModel::class.java)

        findPreference<Preference>("key_app_version")?.apply {
            summary = BuildConfig.VERSION_NAME
        }
        findPreference<Preference>("key_model_version")?.apply {
            summary = classifierViewModel.model.version
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        toolbar_fp.setupWithNavController(navController, appBarConfiguration)
    }
}