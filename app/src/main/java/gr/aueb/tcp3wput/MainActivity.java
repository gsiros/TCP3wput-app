package gr.aueb.tcp3wput;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.material.tabs.TabLayout;

import gr.aueb.tcp3wput.adapters.FragAdapter;
import gr.aueb.tcp3wput.fragments.ClientFragment;
import gr.aueb.tcp3wput.fragments.ServerFragment;
import gr.aueb.tcp3wput.server.TCP3wputServer;
import gr.aueb.tcp3wput.singleton.ServerBackend;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ServerBackend.initialize(getApplicationContext());

        tabLayout = (TabLayout) findViewById(R.id.tabLayout);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        tabLayout.setupWithViewPager(viewPager);

        FragAdapter fgAdapter = new FragAdapter(getSupportFragmentManager(), FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        fgAdapter.addFragment(new ClientFragment(), "Client");
        fgAdapter.addFragment(new ServerFragment(), "Server");
        viewPager.setAdapter(fgAdapter);
    }
}