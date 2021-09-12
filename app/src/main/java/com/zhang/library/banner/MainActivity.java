package com.zhang.library.banner;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zhang.lib.banner.view.XMBannerView;
import com.zhang.library.adapter.callback.OnItemClickCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        XMBannerView viewBanner = findViewById(R.id.view_banner);
        viewBanner.setAdapter(getAdapter());

        List<String> list = new ArrayList<>();
        for (int index = 0; index < 1; index++) {
            list.add("test  " + index);
        }
        getAdapter().getDataHolder().setDataList(list);
    }


    private BannerAdapter adapter;

    public BannerAdapter getAdapter() {
        if (adapter == null) {
            adapter = new BannerAdapter();
            adapter.getCallbackHolder().addOnItemClickCallback(new OnItemClickCallback<String>() {
                @Override
                public void onItemClick(View itemView, String data, int position) {
                    Toast.makeText(MainActivity.this, data, Toast.LENGTH_SHORT).show();
                    int index = new Random().nextInt(30);
                    getAdapter().getDataHolder().addData("新增的【" + index + "】");
                    getAdapter().notifyDataSetChanged();
                }
            });
        }
        return adapter;
    }
}
