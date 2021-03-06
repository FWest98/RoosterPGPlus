package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.Data.Rooster;
import com.thomasdh.roosterpgplus.Data.RoosterBuilder;
import com.thomasdh.roosterpgplus.Helpers.Apache.BasicNameValuePair;
import com.thomasdh.roosterpgplus.Helpers.HelperFunctions;
import com.thomasdh.roosterpgplus.Helpers.Apache.NameValuePair;
import com.thomasdh.roosterpgplus.MainApplication;
import com.thomasdh.roosterpgplus.Models.Lesuur;
import com.thomasdh.roosterpgplus.R;
import com.thomasdh.roosterpgplus.Settings.Constants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public abstract class RoosterViewFragment extends android.support.v4.app.Fragment implements ViewPager.OnPageChangeListener, RoosterBuilder.OnDagScrollListener {
    @Getter
    ViewPager viewPager;
    @Getter
    SwipeRefreshLayout swipeRefreshLayout;
    @Getter @Setter private View rootView;

    public enum LoadType { OFFLINE, ONLINE, NEWONLINE, REFRESH }

    @Getter(value = AccessLevel.PACKAGE) private int week;
    @Getter @Setter private int dag = 0;
    private boolean isScrollingViewPager = false;
    private boolean[] isScrollingScrollView = new boolean[10];

    private boolean hadInternetConnection = true;

    //region Types
    public static Class<? extends RoosterViewFragment>[] types = new Class[]{
            PersoonlijkRoosterFragment.class,
            KlassenRoosterFragment.class,
            DocentenRoosterFragment.class,
            LokalenRoosterFragment.class,
            LeerlingRoosterFragment.class,
            EntityRoosterFragment.class,
            PGTVRoosterFragment.class
    };

    //endregion
    //region Creating

    // Nieuwe instantie van het opgegeven type
    public static <T extends RoosterViewFragment> T newInstance(Class<T> type, int week) {
        T fragment;
        try {
            fragment = type.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        fragment.setWeek(week, false);

        return fragment;
    }

    //endregion
    //region LifeCycle

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getContext());
        tracker.setScreenName(getAnalyticsTitle());
        tracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return null;
    }

    public void setupSwipeRefreshLayout() {
        if(swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimaryDark);
            swipeRefreshLayout.setOnRefreshListener(() -> loadRooster(true));
        }
    }

    @Override
    public void OnScroll(int scrollY, int dag) {
        if (swipeRefreshLayout != null && dag == getDag()) {
            isScrollingScrollView[dag] = scrollY != 0;
            swipeRefreshLayout.setEnabled(!isScrollingViewPager && !isScrollingScrollView[dag]);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        setInternetConnectionState(HelperFunctions.hasInternetConnection(getContext()));
    }

    //endregion
    //region Listeners

    // Pas een rooster laden als er een week bekend is (of herladen als je wil)
    public void setWeek(int week) {
        setWeek(week, true);
    }
    public void setWeek(int week, boolean loadRooster) {
        this.week = week;
        if(week == Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)) {
            setDag(Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 2);
            if(getDag() < 0) setDag(0); // voor weekenden
        } else {
            setDag(0);
        }
        if(loadRooster) {
            loadRooster();
        }
    }

    @Override
    public void onPageScrolled(int i, float v, int i2) {}

    @Override
    public void onPageScrollStateChanged(int i) {
        isScrollingViewPager = i != ViewPager.SCROLL_STATE_IDLE;
        if(swipeRefreshLayout != null && getDag() >= 0)
            swipeRefreshLayout.setEnabled(!isScrollingViewPager && !isScrollingScrollView[getDag()]);
    }

    @Override
    public void onPageSelected(int i) { setDag(i); }

    //endregion
    //region Roosters

    protected abstract boolean canLoadRooster();
    protected abstract List<NameValuePair> getURLQuery(List<NameValuePair> query);
    protected abstract LoadType getLoadType();
    protected abstract long getLoad();
    protected abstract void setLoad();
    public abstract String getAnalyticsTitle();

    public void loadRooster() {
        loadRooster(false);
    }

    public void loadRooster(boolean reload) {
        if(!canLoadRooster() || getWeek() == -1 || getActivity() == null) return;

        Tracker tracker = MainApplication.getTracker(MainApplication.TrackerName.APP_TRACKER, getContext().getApplicationContext());
        tracker.send(new HitBuilders.EventBuilder()
                .setCategory(Constants.ANALYTICS_CATEGORIES_ROOSTER)
                .setAction(reload ? Constants.ANALYTICS_ACTIVITY_ROOSTER_ACTION_REFRESH : Constants.ANALYTICS_ACTIVITY_ROOSTER_ACTION_LOAD)
                .setLabel(getAnalyticsTitle())
                .build());

        if(swipeRefreshLayout != null)
            swipeRefreshLayout.setRefreshing(true);

        List<NameValuePair> query = new ArrayList<>();
        query.add(new BasicNameValuePair("week", Integer.toString(getWeek())));
        query = getURLQuery(query);

        LoadType loadType = reload ? LoadType.REFRESH : getLoadType();

        Rooster.getRooster(query, loadType, getContext(), (result, urenCount) -> {
            if (getActivity() == null) return; // oude context
            if (loadType == LoadType.ONLINE || loadType == LoadType.REFRESH || loadType == LoadType.NEWONLINE && HelperFunctions.hasInternetConnection(getContext())) {
                setLoad();
            }
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            buildRooster(urenCount).build((List<Lesuur>) result);
        }, exception -> {
            if (swipeRefreshLayout != null) swipeRefreshLayout.setRefreshing(false);
            if (loadType != LoadType.REFRESH) {
                buildRooster(0).build((List<Lesuur>) null);
            }
        });
    }

    RoosterBuilder buildRooster(int urenCount) {
        if(getViewPager().getAdapter() == null) getViewPager().setAdapter(new AnimatedPagerAdapter());
        getViewPager().getAdapter().notifyDataSetChanged();
        getViewPager().addOnPageChangeListener(this);
        return new RoosterBuilder(getContext())
                .in(getViewPager())
                .setShowDag(getDag())
                .setShowVervangenUren(true)
                .setLastLoad(getLoad())
                .setOnDagScrollListener(this)
                .setUrenCount(urenCount);
    }

    public void setInternetConnectionState(boolean hasInternetConnection) {
        if(!hadInternetConnection && hasInternetConnection) {
            loadRooster(true);
        }
        hadInternetConnection = hasInternetConnection;
    }

    //endregion
}
