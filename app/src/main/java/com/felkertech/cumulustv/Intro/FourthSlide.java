package com.felkertech.cumulustv.Intro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import com.felkertech.n.cumulustv.R;

public class FourthSlide extends FirstSlide {
  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.intro_1, container, false);

    ButterKnife.bind(this, v);

    setDataUi(R.string.intro_title_4, R.string.intro_msg_4, R.drawable.livechannels3,
        R.color.colorPrimary);
    return v;
  }
}