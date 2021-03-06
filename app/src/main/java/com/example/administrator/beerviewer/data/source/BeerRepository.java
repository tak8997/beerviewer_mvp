package com.example.administrator.beerviewer.data.source;

import android.util.Log;

import com.example.administrator.beerviewer.data.model.BeerModel;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.SingleSource;

@Singleton
public class BeerRepository implements BeerDataSource {

    private static final String TAG = BeerRepository.class.getSimpleName();

    private BeerDataSource beerRemoteDataSource;
    private BeerDataSource beerLocalDataSource;
    private boolean isCache = true;

    @Inject
    public BeerRepository(@Remote BeerDataSource beerRemoteDataSource,
                          @Local BeerDataSource beerLocalDataSource) {
        this.beerRemoteDataSource = beerRemoteDataSource;
        this.beerLocalDataSource = beerLocalDataSource;
    }


    @Override
    public void getBeer(final int beerId, final GetBeerCallback callback) {
        beerLocalDataSource.getBeer(beerId, new GetBeerCallback() {
            @Override
            public void onBeerLoaded(BeerModel beer) {
                Log.d(TAG, "get beer local call");
                callback.onBeerLoaded(beer);
            }

            @Override
            public void onDataNotAvailable() {
                beerRemoteDataSource.getBeer(beerId, new GetBeerCallback() {
                    @Override
                    public void onBeerLoaded(BeerModel beer) {
                        //TODO : do memory local cache
                        Log.d(TAG, "get beer remote call");
                        callback.onBeerLoaded(beer);
                    }

                    @Override
                    public void onDataNotAvailable() {
                        Log.d(TAG, "get beer remote call fail");
                        callback.onDataNotAvailable();
                    }
                });
            }
        });
    }

    @Override
    public void saveBeers(List<BeerModel> beers) {
        beerLocalDataSource.saveBeers(beers);
    }

    @Override
    public Maybe<List<BeerModel>> getBeers() {
        return beerRemoteDataSource.getBeers()
                .filter(beers-> {
                    if (!beers.isEmpty()) {
                        saveBeers(beers);    //save local cache
                        return true;
                    } else
                        return false;
                });
    }

    /**
     * local cache check
     * @param pageStart
     * @param perPage
     */
    @Override
    public Single<List<BeerModel>> getBeers(int pageStart, int perPage) {
        return beerLocalDataSource.getBeers(pageStart, perPage)
                .filter(beers-> !beers.isEmpty())
                .switchIfEmpty(getBeersFromRemote(pageStart, perPage));     //if local is empty, get from remote
    }

    private SingleSource<? extends List<BeerModel>> getBeersFromRemote(int pageStart, int perPage) {
        return beerRemoteDataSource.getBeers(pageStart, perPage)
                .filter(beers-> {
                    if (!beers.isEmpty()) {
                        saveBeers(beers);
                        return true;
                    } else
                        return false;
                })
                .toSingle();
    }
}
