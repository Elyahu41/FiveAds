package com.ej.fiveads.activities.ui.spend;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SpendViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public SpendViewModel() {

    }

    public LiveData<String> getText() {
        return mText;
    }
}