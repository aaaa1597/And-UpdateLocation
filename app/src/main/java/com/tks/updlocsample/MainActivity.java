package com.tks.updlocsample;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
	private final static int REQUEST_LOCATION_SETTINGS = 1211;
	private final static int REQUEST_PERMISSIONS = 2222;
	private final LocationListAdapter mLocationListAdapter = new LocationListAdapter();
	private boolean						mIsSettingLocationON = false;
	private FusedLocationProviderClient mFusedLocationClient;
	private final LocationRequest mLocationRequest = LocationRequest.create().setInterval(1000)
			.setFastestInterval(1000/2)
			.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			mLocationListAdapter.addLocation(new Date(), location.getLongitude(), location.getLatitude());
			RecyclerView rvw = findViewById(R.id.rvw_locations);
			rvw.scrollToPosition(mLocationListAdapter.getItemCount()-1);
			mLocationListAdapter.notifyDataSetChanged();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		findViewById(R.id.btnStartStop).setOnClickListener(v -> {
			boolean isStart = ((Button) v).getText().equals("位置更新 開始");
			if (isStart) {
				((Button) v).setText("位置更新 停止");
				/* ↓Androidでエラーになるから追加するコード。実際はここでエラーにはならない */
				if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
				&& ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
					return;
				/* ↑Androidでエラーになるから追加するコード。実際はここでエラーにはならない */
				mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
			}
			else {
				((Button)v).setText("位置更新 開始");
				mFusedLocationClient.removeLocationUpdates(mLocationCallback);
			}
		});

		/* RecyclerView定義 */
		mLocationListAdapter.addLocation(new Date(), 0.0, 0.0);
		RecyclerView rvw = findViewById(R.id.rvw_locations);
		rvw.addItemDecoration(new DividerItemDecoration(getApplicationContext(), new LinearLayoutManager(getApplicationContext()).getOrientation()));
		rvw.setHasFixedSize(true);
		rvw.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		rvw.setAdapter(mLocationListAdapter);

		mFusedLocationClient= LocationServices.getFusedLocationProviderClient(this);

		/* 地図権限とBluetooth権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* 設定の位置情報ON/OFFチェック */
		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build();
		SettingsClient settingsClient = LocationServices.getSettingsClient(this);
		settingsClient.checkLocationSettings(locationSettingsRequest)
				.addOnSuccessListener(this, locationSettingsResponse -> {
					mIsSettingLocationON = true;
//					/* 開始ポイント */
//					startLocation();
				})
				.addOnFailureListener(this, exception -> {
					int statusCode = ((ApiException)exception).getStatusCode();
					switch (statusCode) {
						case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
							try {
								ResolvableApiException rae = (ResolvableApiException)exception;
								rae.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SETTINGS);
							}
							catch (IntentSender.SendIntentException sie) {
								TLog.d("PendingIntent unable to execute request.");
							}
							break;
						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
							ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").Show(MainActivity.this);
							break;
					}
				});

//		/* 開始ポイント */
//		startLocation();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
		switch (resultCode) {
			case Activity.RESULT_OK:
				mIsSettingLocationON = true;
//				/* 開始ポイント */
//				startLocation();
				break;
			case Activity.RESULT_CANCELED:
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").Show(MainActivity.this);
				break;
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		/* 対象外なので、無視 */
		if (requestCode != REQUEST_PERMISSIONS) return;

		/* 権限リクエストの結果を取得する. */
		long ngcnt = Arrays.stream(grantResults).filter(value -> value != PackageManager.PERMISSION_GRANTED).count();
		if (ngcnt > 0) {
			ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").Show(MainActivity.this);
			return;
		}
		else {
//			/* 開始ポイント */
//			startLocation();
		}
	}

	private void startLocation() {
		/* 権限が許可されていない */
		if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d("位置情報の権限なし.何もしない.");
			return;
		}

		/* 設定の位置情報がOFF */
		if( !mIsSettingLocationON) {
			TLog.d("設定の位置情報がOFF.何もしない.");
			return;
		}
		TLog.d("位置情報 取得開始 正常.");
	}


	/** ******************
	 * LocationListAdapter
	 ** ******************/
	public static class LocationListAdapter extends RecyclerView.Adapter<LocationListAdapter.ViewHolder> {
		static class ViewHolder extends RecyclerView.ViewHolder {
			TextView	mTxtDateTime;
			TextView	mTxtLongitude;
			TextView	mTxtLatitude;
			ViewHolder(View view) {
				super(view);
				mTxtDateTime	= view.findViewById(R.id.txtDatetime);
				mTxtLongitude	= view.findViewById(R.id.txtLongitude);
				mTxtLatitude	= view.findViewById(R.id.txtLatitude);
			}
		}

		private static class LocationModel {
			public String	mDateTime;
			public String	mLongitude;
			public String	mLatitude;
			public LocationModel(Date datetime, double longitude, double latitude) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.JAPAN);
				mDateTime		=  simpleDateFormat.format(datetime);
				mLongitude		= String.valueOf(longitude);
				mLatitude		= String.valueOf(latitude);
			}
		}

		private ArrayList<LocationModel> mLocList = new ArrayList<>();

		@NonNull
		@Override
		public LocationListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
			View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_location, parent, false);
			return new ViewHolder(view);
		}

		@Override
		public void onBindViewHolder(@NonNull LocationListAdapter.ViewHolder holder, int position) {
			LocationModel model = mLocList.get(position);
			holder.mTxtDateTime.setText(model.mDateTime);
			holder.mTxtLongitude.setText(model.mLongitude);
			holder.mTxtLatitude.setText(model.mLatitude);
		}

		@Override
		public int getItemCount() {
			return mLocList.size();
		}

		public void addLocation(Date date, double longitude, double latitude) {
			mLocList.add(new LocationModel(date, longitude, latitude));
		}
	}
}
