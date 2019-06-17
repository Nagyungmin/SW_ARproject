package com.example.cnu_ar;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

// 지도에서 건물을 선택하는 클래스
public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    // 지도 객체
    private GoogleMap mMap;

    // FireBase DB를 사용하기 위한 객체
    final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
    final DatabaseReference ref = mDatabase.getReference();

    // 액티비티가 시작되면 지도를 비 동기적으로 로딩후 콜백 실행
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // 지도 생성 콜백 메소드
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // 충남대 중앙으로 지도 이동
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.3666232, 127.3432415), 16));

        // DB에서 건물명과 좌표를 받아오고, 받아온 좌표에 지도 마커를 생성한다.
        Query query = ref.orderByKey();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for(DataSnapshot data : dataSnapshot.getChildren()) {
                    LatLng temp = new LatLng(Double.valueOf(String.valueOf(data.child("latitude").getValue())), Double.valueOf(String.valueOf(data.child("longitude").getValue())));
                    mMap.addMarker(new MarkerOptions().position(temp).title(String.valueOf(data.child("college").getValue())));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        // 마커를 클릭시 실행되는 이벤트
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {

                // SharedPreferences를 사용해 최근 기록을 String으로 저장하고 구분자로 구분한다.
                String tempBuildList = marker.getTitle();
                String tempPointList = marker.getPosition().latitude + "#" + marker.getPosition().longitude;

                SharedPreferences pref = getSharedPreferences("list", 0);
                SharedPreferences.Editor editor = pref.edit();

                String temp = pref.getString("recent", "");
                temp += tempBuildList + "#";
                editor.putString("recent", temp);

                String tempPoint = pref.getString("point", "");
                tempPoint += tempPointList + "/";
                editor.putString("point", tempPoint);
                editor.commit();

                // 건물 이름과 좌표를 인텐트를 활용해 AR이 실행될 정보로 넘긴다.
                Intent intent = new Intent(getApplicationContext(), ARActivity.class);
                intent.putExtra("buildName", marker.getTitle());
                intent.putExtra("lati", marker.getPosition().latitude);
                intent.putExtra("long", marker.getPosition().longitude);
                startActivity(intent);
            }
        });

        // 현재 위치를 표시하는 메소드. GPS 권한이 필요함
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }
}
