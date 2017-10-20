package com.felkertech.cumulustv.Intro;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.felkertech.n.cumulustv.R;

public class FirstSlide extends Fragment {

  @BindView(R.id.intro_title) protected TextView introTitle;
  @BindView(R.id.intro_description) protected TextView introDescription;
  @BindView(R.id.intro_image) protected ImageView introImage;
  @BindView(R.id.root_view_intro) protected LinearLayout rootView;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View v = inflater.inflate(R.layout.intro_1, container, false);

    ButterKnife.bind(this, v);

    setDataUi(R.string.intro_title_1, R.string.intro_msg_1, R.drawable.image_intro_1,
        R.color.colorPrimary);
    return v;
  }

  protected void setDataUi(int tileResource, int descriptionResource, int imageResource,
      int colorResource) {
    introTitle.setText(tileResource);
    introDescription.setText(descriptionResource);

    introImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), imageResource));
    rootView.setBackgroundColor(ContextCompat.getColor(getActivity(), colorResource));
  }
}