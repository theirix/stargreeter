package ru.omniverse.android.stargreeter;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends Activity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        findViewById(R.id.launch_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(Utils.TAG, "Clicked button");
                Intent intent = new Intent(MainActivity.this, StarGreeterActivity.class);
                startActivity(intent);
            }
        });
    }


}