package com.wordsaretoys.freebeard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class LaunchActivity extends Activity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
		startService(
				new Intent(
					LaunchActivity.this, 
					PlayerService.class));
        finish();
	}

}
