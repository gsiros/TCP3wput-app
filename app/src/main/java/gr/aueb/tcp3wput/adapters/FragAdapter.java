package gr.aueb.tcp3wput.adapters;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class FragAdapter extends FragmentPagerAdapter {

    private final ArrayList<Fragment> FRAGMENTS = new ArrayList<>();
    private final ArrayList<String> FRAGMENT_TITLES = new ArrayList<>();


    public FragAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return this.FRAGMENTS.get(position);
    }

    @Override
    public int getCount() {
        return this.FRAGMENTS.size();
    }

    public void addFragment(Fragment frag, String title){
        FRAGMENTS.add(frag);
        FRAGMENT_TITLES.add(title);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return FRAGMENT_TITLES.get(position);
    }
}
