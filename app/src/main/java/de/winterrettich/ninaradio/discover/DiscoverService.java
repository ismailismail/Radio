package de.winterrettich.ninaradio.discover;

import java.util.List;

import de.winterrettich.ninaradio.model.Station;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface DiscoverService {
    @GET("Search.ashx?types=station&render=json")
    Call<List<Station>> search(@Query("query") String query);
}
