package me.camsteffen.polite.rule.master

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.android.support.DaggerFragment
import me.camsteffen.polite.HelpFragment
import me.camsteffen.polite.MainActivity
import me.camsteffen.polite.Polite
import me.camsteffen.polite.R
import me.camsteffen.polite.RuleService
import me.camsteffen.polite.databinding.RulesFragmentBinding
import me.camsteffen.polite.model.CalendarRule
import me.camsteffen.polite.model.Rule
import me.camsteffen.polite.model.ScheduleRule
import me.camsteffen.polite.rule.RenameDialogFragment
import me.camsteffen.polite.rule.RuleMasterDetailViewModel
import me.camsteffen.polite.rule.edit.EditCalendarRuleFragment
import me.camsteffen.polite.rule.edit.EditRuleFragment
import me.camsteffen.polite.rule.edit.EditScheduleRuleFragment
import me.camsteffen.polite.settings.AppPreferences
import me.camsteffen.polite.settings.SettingsFragment
import javax.inject.Inject

class RulesFragment : DaggerFragment() {

    @Inject lateinit var preferences: AppPreferences
    @Inject lateinit var ruleService: RuleService
    @Inject lateinit var viewModelProviderFactory: ViewModelProvider.Factory

    lateinit var model: RuleMasterDetailViewModel
    val polite: Polite
        get() = activity!!.application as Polite
    private val mainActivity: MainActivity
        get() = activity as MainActivity
    private val fab: FloatingActionButton
        get() = activity!!.findViewById(R.id.fab) as FloatingActionButton

    private lateinit var adapter: RuleMasterAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        retainInstance = true

        model = ViewModelProviders.of(activity!!, viewModelProviderFactory)[RuleMasterDetailViewModel::class.java]
        adapter = RuleMasterAdapter(this::openRule, this::onRuleCheckedChange)

        model.ruleMasterList.observe(this, Observer {
            adapter.submitList(it)
        })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<RulesFragmentBinding>(inflater, R.layout.rules_fragment, container, false)
        binding.lifecycleOwner = this
        binding.handlers = this
        binding.model = model
        registerForContextMenu(binding.rulesView)
        binding.rulesView.adapter = adapter
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        checkNotificationPolicyAccess()
    }

    private fun checkNotificationPolicyAccess() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            return
        val notificationManager = mainActivity.notificationManager
        if (notificationManager.isNotificationPolicyAccessGranted) {
            notificationManager.cancelNotificationPolicyAccessRequired()
        } else if (preferences.enable) {
            AlertDialog.Builder(activity!!)
                    .setTitle(R.string.notification_policy_access_required)
                    .setMessage(R.string.notification_policy_access_explain)
                    .setNegativeButton(R.string.disable_polite) { dialog, _ ->
                        dialog.dismiss()
                        preferences.enable = false
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                        startActivity(intent)
                    }
                    .create()
                    .show()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity.supportActionBar!!.setTitle(R.string.app_name)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> {
                val intent = Intent(Intent.ACTION_SEND)
                val text = getString(R.string.share_text, getString(R.string.play_store_url))
                intent.putExtra(Intent.EXTRA_TEXT, text)
                intent.type = "text/plain"
                startActivity(Intent.createChooser(intent, getString(R.string.share_polite)))
            }
            R.id.settings -> openSettings()
            R.id.help -> {
                fragmentManager!!.beginTransaction()
                        .replace(R.id.fragment_container, HelpFragment())
                        .addToBackStack(null)
                        .commit()
                fab.hide()
            }
            else -> return false
        }
        return true
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenu.ContextMenuInfo?) {
        activity!!.menuInflater.inflate(R.menu.rule_context, menu)
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val info = item.menuInfo as RuleMasterRecyclerView.RuleContextMenuInfo
        val rule = info.rule
        when(item.itemId) {
            R.id.rename -> {
                RenameDialogFragment.newInstance(rule.id, rule.name)
                        .show(fragmentManager!!, RenameDialogFragment.FRAGMENT_TAG)
            }
            R.id.delete -> {
                ruleService.deleteRuleAsync(rule.id)
            }
            else -> return false
        }
        return true
    }

    fun onClickDisabledNotice() {
        openSettings()
    }

    private fun openSettings() {
        fragmentManager!!.beginTransaction()
                .replace(R.id.fragment_container, SettingsFragment(), SettingsFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
        fab.hide()
    }

    fun openRule(rule: Rule) {
        val fragment = when(rule) {
            is CalendarRule -> {
                if(!mainActivity.checkCalendarPermission())
                    return
                EditCalendarRuleFragment()
            }
            is ScheduleRule -> EditScheduleRuleFragment()
        }
        model.selectedRule.value = rule
        fragmentManager!!.beginTransaction()
                .setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left, R.animator.slide_in_left, R.animator.slide_out_right)
                .replace(R.id.fragment_container, fragment, EditRuleFragment.FRAGMENT_TAG)
                .addToBackStack(null)
                .commit()
        fab.hide()
    }

    private fun onRuleCheckedChange(rule: Rule, isChecked: Boolean) {
        if (rule.enabled != isChecked) {
            if (isChecked && rule is CalendarRule)
                mainActivity.checkCalendarPermission()
            ruleService.updateRuleEnabledAsync(rule.id, isChecked)
        }
    }

    companion object {
        const val FRAGMENT_TAG = "rules"
    }
}
