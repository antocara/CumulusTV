package com.felkertech.cumulustv.Intro;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import com.felkertech.cumulustv.managers.NavigationManager;
import com.felkertech.cumulustv.commons.helper.ActivityUtils;
import com.felkertech.cumulustv.commons.helper.DriveSettingsManager;
import com.felkertech.n.cumulustv.R;
import com.github.paolorotolo.appintro.AppIntro;

public class Intro extends AppIntro {

  @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    addSlide(new FirstSlide());
    addSlide(new SecondSlide());
    addSlide(new SecondOneSlide());
    addSlide(new ThirdSlide());
    addSlide(new FourthSlide());
    setFadeAnimation();
  }

  private void loadMainActivity() {
    DriveSettingsManager sm = new DriveSettingsManager(this);
    sm.setInt(R.string.sm_last_version, ActivityUtils.LAST_GOOD_BUILD);
    NavigationManager.navigateToMainActivity(this);
  }

  @Override public void onSkipPressed(Fragment currentFragment) {
    super.onSkipPressed(currentFragment);
    // Do something when users tap on Skip button.
    loadMainActivity();
  }

  @Override public void onDonePressed(Fragment currentFragment) {
    super.onDonePressed(currentFragment);
    loadMainActivity();
  }

  @Override
  public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
    super.onSlideChanged(oldFragment, newFragment);
    // Do something when the slide changes.
  }
}
