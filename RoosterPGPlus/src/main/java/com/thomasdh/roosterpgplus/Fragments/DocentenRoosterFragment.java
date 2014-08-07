package com.thomasdh.roosterpgplus.Fragments;

import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.thomasdh.roosterpgplus.Adapters.AnimatedPagerAdapter;
import com.thomasdh.roosterpgplus.CustomViews.DefaultSpinner;
import com.thomasdh.roosterpgplus.Data.RoosterInfo;
import com.thomasdh.roosterpgplus.Helpers.FragmentTitle;
import com.thomasdh.roosterpgplus.Models.Leraar;
import com.thomasdh.roosterpgplus.Models.Vak;
import com.thomasdh.roosterpgplus.R;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@FragmentTitle(title = R.string.action_bar_dropdown_docentenrooster)
public class DocentenRoosterFragment extends RoosterViewFragment implements AdapterView.OnItemSelectedListener {
    private static final Long MIN_REFRESH_WAIT_TIME = (long) 3600000;

    @Getter @Setter private Vak vak;
    @Getter @Setter private String leraar;
    @Getter @Setter private ArrayList<Vak> vakken;

    private DefaultSpinner leraarSpinner;
    private DefaultSpinner vakSpinner;


    @Override
    public Type getType() {
        return Type.DOCENTENROOSTER;
    }

    @Override
    public boolean canLoadRooster() { return getLeraar() != null; }

    @Override
    public List<NameValuePair> getURLQuery(List<NameValuePair> query) {
        query.add(new BasicNameValuePair("docent", getLeraar()));
        return query;
    }

    @Override
    public void setLoad() { RoosterInfo.setLoad("docent"+getLeraar(), System.currentTimeMillis(), getActivity()); }

    @Override
    public LoadType getLoadType() {
        Long lastLoad = RoosterInfo.getLoad("docent"+getLeraar(), getActivity());
        if(lastLoad == null) {
            return LoadType.ONLINE;
        } else if(System.currentTimeMillis() > lastLoad + MIN_REFRESH_WAIT_TIME) {
            return LoadType.NEWONLINE;
        } else {
            return LoadType.OFFLINE;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        setRootView(inflater.inflate(R.layout.fragment_main_docenten, container, false));
        viewPager = (ViewPager) getRootView().findViewById(R.id.viewPager_docent);
        viewPager.setAdapter(new AnimatedPagerAdapter());

        leraarSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_naam);
        vakSpinner = (DefaultSpinner) getRootView().findViewById(R.id.main_fragment_spinner_docent_vak);

        RoosterInfo.getLeraren(getActivity(), s -> onLerarenLoaded((ArrayList<Vak>) s));

        return getRootView();
    }

    public void onLerarenLoaded(ArrayList<Vak> result) {
        setVakken(result);

        if(getVakken() == null) return;

        ArrayList<String> vakNamen = new ArrayList<>();
        for(Vak vak : vakken) { vakNamen.add(vak.getNaam()); }

        ArrayAdapter<String> vakAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, vakNamen.toArray(new String[vakNamen.size()]));
        vakAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        vakSpinner.setAdapter(vakAdapter);
        vakSpinner.setOnItemSelectedListener(this);
        leraarSpinner.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(parent.equals(vakSpinner)) {
            onVakSelected(position);
        } else if(parent.equals(leraarSpinner)) {
            onLeraarSelected(position);
        }
    }

    public void onVakSelected(int position) {
        setVak(vakken.get(position));

        ArrayList<String> leraarNamen = new ArrayList<>();
        for(Leraar leraar : getVak().leraren) { leraarNamen.add(leraar.naam); }

        ArrayAdapter<String> leraarAdapter = new ArrayAdapter<>(getActivity(), R.layout.spinner_title, leraarNamen.toArray(new String[leraarNamen.size()]));
        leraarAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        leraarSpinner.setAdapter(leraarAdapter);
    }

    public void onLeraarSelected(int position) {
        setLeraar(getVak().getLeraren().get(position).getCode());
        loadRooster();
    }

    @Override public void onNothingSelected(AdapterView<?> parent) {}
}
