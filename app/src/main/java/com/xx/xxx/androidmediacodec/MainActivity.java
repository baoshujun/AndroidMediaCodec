package com.xx.xxx.androidmediacodec;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClicked(View view) {
        switch (view.getId()){
            case R.id.button://解码
                toAcitivity(DecodeActivityList.class);
                break;
            case R.id.button2://编码

               break;
        }
    }


    private void toAcitivity(Class clazz){
        Intent intent = new Intent(this,clazz);
        startActivity(intent);
    }
}
