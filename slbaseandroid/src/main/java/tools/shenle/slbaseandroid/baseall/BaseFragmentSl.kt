package tools.shenle.slbaseandroid.baseall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


abstract class BaseFragmentSl : androidx.fragment.app.Fragment() {
    private var mRootView: View? = null

    abstract val layoutId: Int
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView = inflater.inflate(layoutId, container, false)
        return mRootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onCreateVM()
        initView()
        initData()
        super.onViewCreated(view, savedInstanceState)
    }
    open fun onCreateVM() {}
    abstract fun initView()

    abstract fun initData()
    override fun onDestroyView() {
        super.onDestroyView()
        mRootView = null
    }
}