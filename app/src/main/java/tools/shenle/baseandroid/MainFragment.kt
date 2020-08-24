package tools.shenle.baseandroid

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.fragment_main.*
import tools.shenle.baseandroid.home.fragment.*
import tools.shenle.baseandroid.R
import tools.shenle.slbaseandroid.baseall.BaseFragmentSl

/**
 * Created by shenle on 2020/3/31.
 */
class MainFragment:BaseFragmentSl() {
    override val layoutId: Int
        get() = R.layout.fragment_main

    private val fragmentList = arrayListOf<Fragment>()
    private val homeFragment by lazy { HomeFragment() }
    private val lunTanFragment by lazy { LunTanFragment() }
    private val ziXunFragment by lazy { ZiXunFragment() }
    private val zyscFragment by lazy { ZyscFragment() }
    private val personFragment by lazy { PersonFragment() }

    init {
        fragmentList.run {
            add(homeFragment)
            add(lunTanFragment)
            add(ziXunFragment)
            add(zyscFragment)
            add(personFragment)
        }
    }


    override fun initView() {
        initViewPager()
        bnv.itemIconTintList = null
        bnv.setOnNavigationItemSelectedListener(onNavigationItemSelected)
    }

    override fun initData() {
    }


    private val onNavigationItemSelected = BottomNavigationView.OnNavigationItemSelectedListener {
        when (it.itemId) {
            R.id.main0 -> {
                switchFragment(0)
            }
            R.id.main1 -> {
                switchFragment(1)
            }
            R.id.main2 -> {
                switchFragment(2)
            }
            R.id.main3 -> {
                switchFragment(3)
            }
            R.id.main4 -> {
                switchFragment(4)
            }
        }
        true
    }

    private fun switchFragment(position: Int): Boolean {
        vp2.setCurrentItem(position, false)
        return true
    }

    private fun initViewPager() {
        vp2.isUserInputEnabled = true
        vp2.offscreenPageLimit = 2
        vp2.adapter = object : FragmentStateAdapter(this) {
            override fun createFragment(position: Int) = fragmentList[position]

            override fun getItemCount() = fragmentList.size
        }
        vp2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                bnv.selectedItemId = when (position) {
                    0 -> {
                        R.id.main0
                    }
                    1 -> {
                        R.id.main1
                    }
                    2 -> {
                        R.id.main2
                    }
                    3 -> {
                        R.id.main3
                    }
                    4 -> {
                        R.id.main4
                    }
                    else->R.id.main0
                }
            }
        })
    }
}