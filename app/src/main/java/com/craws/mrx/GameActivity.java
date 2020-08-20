package com.craws.mrx;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.craws.mrx.engine.GameView;

public class GameActivity extends AppCompatActivity {

    private GameView gameView;
    //private Button switchBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_game);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        gameView = (GameView)findViewById(R.id.gameView);
        //switchBtn = (Button)findViewById(R.id.btn_switch);

        /**switchBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gameView.switcheroo();
            }
        }); */
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
    }
}
