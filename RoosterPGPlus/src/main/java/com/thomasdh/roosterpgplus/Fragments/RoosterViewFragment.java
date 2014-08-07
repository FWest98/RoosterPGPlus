package com.thomasdh.roosterpgplus.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.thomasdh.roosterpgplus.Account;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Helpers.RoosterWeek;
import com.thomasdh.roosterpgplus.MainActivity;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.RoosterBuilderOld;
import com.thomasdh.roosterpgplus.RoosterDownloaderOld;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import roboguice.fragment.RoboFragment;

/**
 * Created by Floris on 7-7-2014.
 */
public abstract class RoosterViewFragment extends RoboFragment implements ViewPager.OnPageChangeListener {
    private static final String STATE_FRAGMENT = "fragmentType";

    @Deprecated public static String leraarLeerlingselected;

    @Getter public ViewPager viewPager;
    @Getter @Setter private View rootView;
    public enum LoadType { OFFLINE, ONLINE, NEWONLINE; }

    @Setter public Account user;
    @Getter(value = AccessLevel.PRIVATE) private int week;
    @Getter @Setter private int dag = 0;

    //region Types
    public static Class<? extends RoosterViewFragment>[] types = new Class[]{
            PersoonlijkRoosterFragment.class,
            KlassenRoosterFragment.class,
            DocentenRoosterFragment.class,
            LokalenRoosterFragment.class,
            LeerlingRoosterFragment.class
    };

    @Deprecated
    public abstract Type getType();

    //endregion
    //region Creating

    // Nieuwe instantie van het opgegeven type
    public static <T extends RoosterViewFragment> T newInstance(Class<T> type, Account user, int week) {
        T fragment = null;
        try {
            fragment = type.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Bundle args = new Bundle();
        args.putSerializable(STATE_FRAGMENT, fragment.getType());
        fragment.setArguments(args);

        fragment.setWeek(week);
        fragment.setUser(user);

        return fragment;
    }

    //endregion
    //region LifeCycle

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    //endregion
    //region Listeners

    // Pas een rooster laden als er een week bekend is (of herladen als je wil)
    public void setWeek(int week) {
        this.week = week;
        if(week == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) {
            setDag(Calendar.getInstance().get(Calendar.DAY_OF_WEEK));
        } else {
            setDag(0);
        }
        loadRooster();
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {}

    @Override
    public void onPageScrollStateChanged(int i) {}

    @Override
    public void onPageSelected(int i) { setDag(i); }

    //endregion
    //region Roosters

    protected abstract boolean canLoadRooster();
    public abstract List<NameValuePair> getURLQuery(List<NameValuePair> query);
    public abstract LoadType getLoadType();
    public abstract void setLoad();
    // TODO add dingen


    public void loadRooster() {
        if(!canLoadRooster()) return;

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(getWeek())));
        query = getURLQuery(query);

        LoadType loadType = getLoadType();

        Rooster.getRooster(query, loadType, getActivity(), result -> {
            if(loadType == LoadType.ONLINE || loadType == LoadType.NEWONLINE) {
                setLoad();
            }
            RoosterBuilder.build((List<Lesuur>) result, getDag(), getViewPager(), getActivity());
            // Rooster buildsels
            //new RoosterBuilderOld(getActivity(), getViewPager(), getWeek(), getType()).buildLayout(new RoosterWeek((String) result, getActivity()));
        });
    }

    //endregion

    @Deprecated
    public void laadRooster(Context context, View rootView, Type type) {
        if (getWeek() == -1){
            return;
        }
        int selectedWeek = MainActivity.getSelectedWeek();
        WeakReference<MenuItem> refreshItem = MainActivity.getRefreshItem();

        Log.d("MainActivity", "Rooster aan het laden van week " + selectedWeek);
        if (type == Type.PERSOONLIJK_ROOSTER) {
            //Probeer de string uit het geheugen te laden
            RoosterWeek roosterWeek = RoosterWeek.laadUitGeheugen(selectedWeek, getActivity());

            //Als het de goede week is, gebruik hem
            if (roosterWeek != null && roosterWeek.getWeek() == selectedWeek) {
                new RoosterBuilderOld(context, (ViewPager) rootView.findViewById(R.id.viewPager), selectedWeek, type).buildLayout(roosterWeek);
                Log.d("MainActivity", "Het uit het geheugen geladen rooster is van de goede week");
                new RoosterDownloaderOld(context, rootView, false, refreshItem.get(), selectedWeek).execute();
            } else {
                if (roosterWeek == null) {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is null");
                } else {
                    Log.d("MainActivity", "Het uit het geheugen geladen rooster is van week " + roosterWeek.getWeek() + ", de gewilde week is " + MainActivity.getSelectedWeek());
                }
                new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek).execute();
            }
        } else if (type == Type.KLASROOSTER) {
            new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek, leraarLeerlingselected, type).execute();
        } else if (type == Type.DOCENTENROOSTER) {
            new RoosterDownloaderOld(context, rootView, true, refreshItem.get(), selectedWeek, leraarLeerlingselected, type).execute();
        }
    }

    public enum Type {
        PERSOONLIJK_ROOSTER (0),
        KLASROOSTER (1),
        DOCENTENROOSTER (2),
        LOKALENROOSTER (3),
        LEERLINGROOSTER (4);

        Type(int id) {
            this.id = id;
        }
        private int id;
    }
}
