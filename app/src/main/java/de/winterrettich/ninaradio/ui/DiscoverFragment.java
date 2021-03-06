package de.winterrettich.ninaradio.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.List;

import de.winterrettich.ninaradio.R;
import de.winterrettich.ninaradio.RadioApplication;
import de.winterrettich.ninaradio.event.BufferEvent;
import de.winterrettich.ninaradio.event.DatabaseEvent;
import de.winterrettich.ninaradio.event.DiscoverErrorEvent;
import de.winterrettich.ninaradio.event.PlaybackEvent;
import de.winterrettich.ninaradio.event.SelectStationEvent;
import de.winterrettich.ninaradio.model.Station;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DiscoverFragment extends Fragment implements StationAdapter.StationClickListener, SearchView.OnQueryTextListener {
    private static final String TAG = DiscoverFragment.class.getSimpleName();
    private StationAdapter mAdapter;
    private SearchView mSearchView;
    private View mProgressIndicator;
    private Call<List<Station>> mSearchCall;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_discover, container, false);

        mAdapter = new StationAdapter(this);
        mAdapter.showFavorites(true);

        mProgressIndicator = rootView.findViewById(R.id.progress_indicator);

        RecyclerView favoritesList = (RecyclerView) rootView.findViewById(R.id.result_list);
        favoritesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        favoritesList.setAdapter(mAdapter);
        favoritesList.setHasFixedSize(true);

        mSearchView = (SearchView) rootView.findViewById(R.id.search_view);
        mSearchView.setOnQueryTextListener(this);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        RadioApplication.sBus.register(this);

        // Playback state and stations may have changed while paused
        handlePlaybackEvent(RadioApplication.sDatabase.playbackState);
        handleSelectStationEvent(new SelectStationEvent(RadioApplication.sDatabase.selectedStation));
    }

    @Override
    public void onPause() {
        super.onPause();
        RadioApplication.sBus.unregister(this);
    }

    @Override
    public void onClick(Station station) {
        if (!station.equals(RadioApplication.sDatabase.selectedStation)) {
            // play
            RadioApplication.sBus.post(new SelectStationEvent(station));
            RadioApplication.sBus.post(PlaybackEvent.PLAY);
        }
    }

    @Override
    public boolean onLongClick(Station station) {
        return false;
    }

    @Override
    public void onFavoriteChanged(Station station, boolean favorite) {
        if (favorite) {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.CREATE_STATION, station));
        } else {
            RadioApplication.sBus.post(new DatabaseEvent(DatabaseEvent.Operation.DELETE_STATION, station));
        }
    }

    @Subscribe
    public void handlePlaybackEvent(PlaybackEvent event) {
        if (event == PlaybackEvent.STOP) {
            mAdapter.clearSelection();
        }
        mAdapter.updateStation(RadioApplication.sDatabase.selectedStation);
    }

    @Subscribe
    public void handleBufferEvent(BufferEvent event) {
        mAdapter.updateStation(RadioApplication.sDatabase.selectedStation);
    }

    @Subscribe
    public void handleSelectStationEvent(SelectStationEvent event) {
        mAdapter.setSelection(event.station);
    }

    @Subscribe
    public void handleDatabaseEvent(DatabaseEvent event) {
        mAdapter.updateStation(event.station);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        // focus/unfocus the Searchview when scrolling to/from the fragment
        if (mSearchView == null) {
            return;
        }
        if (isVisibleToUser) {
            mSearchView.requestFocus();
            InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(mSearchView.findFocus(), InputMethodManager.SHOW_IMPLICIT);
        } else {
            mSearchView.clearFocus();
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        mSearchView.clearFocus();

        resetSearch();

        // show progressbar
        mProgressIndicator.setVisibility(View.VISIBLE);

        // search for stations
        mSearchCall = RadioApplication.sDiscovererService.search(query);
        mSearchCall.enqueue(new Callback<List<Station>>() {
            @Override
            public void onResponse(Call<List<Station>> call, Response<List<Station>> response) {
                mProgressIndicator.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    String message = getString(R.string.error_discovering_stations);
                    String rawMessage = response.raw().message();
                    if (rawMessage != null) {
                        message += " (" + response.raw().message() + ")";
                    }
                    RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                    return;
                }

                List<Station> stations = response.body();

                // show stations
                mAdapter.setStations(stations);

                if (stations.isEmpty()) {
                    String message = getString(R.string.no_stations_discovered);
                    RadioApplication.sBus.post(new DiscoverErrorEvent(message));
                } else {
                    // a station may already be playing
                    mAdapter.setSelection(RadioApplication.sDatabase.selectedStation);
                }
            }

            @Override
            public void onFailure(Call<List<Station>> call, Throwable t) {
                mProgressIndicator.setVisibility(View.GONE);
                String message = getString(R.string.error_discovering_stations);
                Log.e(TAG, message, t);
                RadioApplication.sBus.post(new DiscoverErrorEvent(message));
            }
        });
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.length() == 0) {
            resetSearch();
            return true;
        }
        return false;
    }

    private void resetSearch() {
        if (mSearchCall != null) {
            // cancel running search calls
            mSearchCall.cancel();
        }
        mProgressIndicator.setVisibility(View.GONE);
        mAdapter.clearSelection();
        mAdapter.setStations(Collections.<Station>emptyList());
    }

}
