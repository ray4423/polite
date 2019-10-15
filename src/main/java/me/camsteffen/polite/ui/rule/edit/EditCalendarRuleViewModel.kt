package me.camsteffen.polite.ui.rule.edit

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import me.camsteffen.polite.data.db.entity.AudioPolicy
import me.camsteffen.polite.data.model.CalendarEventMatchBy
import me.camsteffen.polite.data.model.CalendarRule
import javax.inject.Inject

class EditCalendarRuleViewModel
@Inject constructor(application: Application) : EditRuleViewModel<CalendarRule>(application) {

    val busyOnly = MutableLiveData<Boolean>(false)

    val calendarIds = MutableLiveData<Set<Long>>(emptySet())

    val inverseMatch: LiveData<Boolean>
        get() = inverseMatchMutable

    val matchBy = MutableLiveData<CalendarEventMatchBy>()

    val keywords: LiveData<Set<String>>
        get() = keywordsLiveData

    private val keywordSet: MutableSet<String> = hashSetOf()
    private val keywordsLiveData = MutableLiveData<Set<String>>(keywordSet)

    private val inverseMatchMutable = MutableLiveData<Boolean>()

    val showKeywords: LiveData<Boolean> = Transformations.map(matchBy) { matchBy ->
        matchBy != CalendarEventMatchBy.ALL
    }

    override fun setRule(rule: CalendarRule) {
        super.setRule(rule)
        busyOnly.value = rule.busyOnly
        inverseMatchMutable.value = rule.inverseMatch
        matchBy.value = rule.matchBy
        calendarIds.value = rule.calendarIds
        keywordSet.clear()
        keywordSet.addAll(rule.keywords)
        invalidateKeywordsLiveData()
    }

    override fun doCreateRule(id: Long, name: String, audioPolicy: AudioPolicy): CalendarRule {
        return CalendarRule(
            id = id,
            name = name,
            enabled = enabled.get(),
            audioPolicy = audioPolicy,
            busyOnly = busyOnly.value!!,
            matchBy = matchBy.value!!,
            inverseMatch = inverseMatch.value!!,
            calendarIds = calendarIds.value!!,
            keywords = keywords.value!!
        )
    }

    fun addKeyword(keyword: String): Boolean {
        val added = keywordSet.add(keyword)
        if (added) {
            invalidateKeywordsLiveData()
        }
        return added
    }

    fun setInverseMatch(inverseSelect: Boolean) {
        inverseMatchMutable.value = inverseSelect
    }

    fun removeWord(word: String) {
        if (keywordSet.remove(word)) {
            invalidateKeywordsLiveData()
        }
    }

    private fun invalidateKeywordsLiveData() {
        keywordsLiveData.value = keywordSet
    }
}
