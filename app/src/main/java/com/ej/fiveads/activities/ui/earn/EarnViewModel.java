package com.ej.fiveads.activities.ui.earn;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EarnViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public EarnViewModel() {
        mText = new MutableLiveData<>();
    }

    public LiveData<String> getText() {
        return mText;
    }
}