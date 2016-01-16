package com.threezj.fuli.activity;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.widget.Toast;

import com.threezj.fuli.ApiUrl;
import com.threezj.fuli.R;
import com.threezj.fuli.Util.HttpUtil;
import com.threezj.fuli.Util.ResponseHandleUtil;
import com.threezj.fuli.adapter.ImageRecyclerViewAdapter;
import com.threezj.fuli.model.ImageFuli;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity {

    private StaggeredGridLayoutManager gaggeredGridLayoutManager;
    private ArrayList<ImageFuli> imagesList = new ArrayList<ImageFuli>();
    private ImageRecyclerViewAdapter imageRecyclerViewAdapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private boolean onLoading = false;
    private int loadImageCount = 15;
    private int loadTimes=1;
    private boolean isFirst = true;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();

        if(!findFromDb()){
            swipeRefreshLayout.post(new Runnable() {
                @Override
                public void run() {
                    swipeRefreshLayout.setRefreshing(true);
                }
            });
            getImagesDataFromHttp();
        }
        Log.d("test", "addScrolListener");
        recyclerView.addOnScrollListener(getOnBottomListener(gaggeredGridLayoutManager));
    }

    private void init() {
        recyclerView = (RecyclerView)findViewById(R.id.content);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.refresher);
        swipeRefreshLayout.setColorSchemeColors(R.color.colorPrimary);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.d("test", "onRefresh");
                getImagesDataFromHttp();
            }
        });

        recyclerView.setHasFixedSize(true);
        gaggeredGridLayoutManager = new StaggeredGridLayoutManager(2, 1);
        recyclerView.setLayoutManager(gaggeredGridLayoutManager);
        imageRecyclerViewAdapter = new ImageRecyclerViewAdapter(MainActivity.this, imagesList);
        recyclerView.setAdapter(imageRecyclerViewAdapter);

    }

    private void getImagesDataFromHttp() {
        Log.d("test","getDataFromHttp");
        HttpUtil.httpRequestToGank(ApiUrl.gankApiUrl + (loadImageCount + 5 * loadTimes) + "/1", new HttpUtil.HttpUtilCallBack() {
            @Override
            public void onFinsh(String response) {
                try {
                    ResponseHandleUtil.HandleResponseFromHttp(response, MainActivity.this,imagesList);
                    loadTimes++;
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        recyclerView.getAdapter().notifyDataSetChanged();


                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });

            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "加载失败！", Toast.LENGTH_SHORT).show();
                        if (swipeRefreshLayout.isRefreshing()) {
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    }
                });

            }
        });
    }

    public boolean findFromDb(){
        Realm realm = Realm.getInstance(this);
        RealmResults<ImageFuli> images = realm.allObjects(ImageFuli.class);
        if(images.size()==0){
            //realm.close();
            return false;
        }
        else{
            isFirst=false;
            imageRecyclerViewAdapter = new ImageRecyclerViewAdapter(MainActivity.this, images);
            recyclerView.setAdapter(imageRecyclerViewAdapter);

            recyclerView.getAdapter().notifyDataSetChanged();
            //realm.close();
            return true;
        }
    }

    private static final int PRELOAD_SIZE = 6;
    private boolean mIsFirstTimeTouchBottom = true;
    private int mPage = 1;

    private RecyclerView.OnScrollListener getOnBottomListener(final StaggeredGridLayoutManager layoutManager) {
        return new RecyclerView.OnScrollListener() {
            @Override public void onScrolled(RecyclerView rv, int dx, int dy) {
                boolean isBottom = layoutManager.findLastCompletelyVisibleItemPositions(new int[2])[1] >= imageRecyclerViewAdapter.getItemCount() - PRELOAD_SIZE;
                if (!swipeRefreshLayout.isRefreshing() && isBottom) {

                    swipeRefreshLayout.setRefreshing(true);
                    mPage += 1;
                    getImagesDataFromHttp();

                }
            }
        };
    }

}
