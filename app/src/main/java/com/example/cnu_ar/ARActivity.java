package com.example.cnu_ar;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import uk.co.appoly.arcorelocation.LocationMarker;
import uk.co.appoly.arcorelocation.LocationScene;
import uk.co.appoly.arcorelocation.rendering.LocationNode;
import uk.co.appoly.arcorelocation.rendering.LocationNodeRender;
import uk.co.appoly.arcorelocation.utils.ARLocationPermissionHelper;
import uk.co.appoly.arcorelocation.utils.LocationUtils;

public class ARActivity extends FragmentActivity implements OnMapReadyCallback {
    private boolean installRequested;
    private boolean hasFinishedLoading = false;

    // 카메라 화면 객체
    private ArSceneView arSceneView;

    // View를 AR로 만든 객체
    private ViewRenderable exampleLayoutRenderable;

    // 특정 GPS 좌표에 AR을 표시하기 위한 객체
    private LocationScene locationScene;

    // 화살표 ImageView 객체 및 변수
    private ImageView arrImg;
    private double tarLati, tarLong;
    private float bearing;

    // 건물 명 변수
    private String build;

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ar);
        arSceneView = findViewById(R.id.ar_scene_view);

        // 지도를 비동기적으로 실행하기 위한 메소드. 지도 로딩 완료시 콜백이 실행된다.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.cam_map);
        mapFragment.getMapAsync(this);

        // 화살표 ImageView
        arrImg = findViewById(R.id.arrImg);

        // 인텐트로 건물 이름 및 좌표를 받아옴
        Intent intent = getIntent();
        build = intent.getExtras().getString("buildName");
        tarLati = Double.valueOf(intent.getExtras().getDouble("lati"));
        tarLong = Double.valueOf(intent.getExtras().getDouble("long"));

        // View를 3D AR로 구성하기 위한 메소드
        CompletableFuture<ViewRenderable> exampleLayout = ViewRenderable.builder()
                        .setView(this, R.layout.ar_layout)
                        .build();

        // 화면에 AR을 렌더링 하도록 로드하는 메소드
        CompletableFuture.allOf(
                exampleLayout)
                .handle(
                        (notUsed, throwable) -> {
                            if (throwable != null) {
                                Utils.displayError(this, "이미지 받아오기 실패", throwable);
                                return null;
                            }

                            try {
                                exampleLayoutRenderable = exampleLayout.get();
                                hasFinishedLoading = true;

                            } catch (InterruptedException | ExecutionException ex) {
                                Utils.displayError(this, "이미지 받아오기 실패", ex);
                            }

                            return null;
                        });

        // 화면을 인식하면 매 프레임 새로 화면을 그리는 리스너를 지정
        arSceneView
                .getScene()
                .addOnUpdateListener(
                        frameTime -> {
                            if (!hasFinishedLoading) {
                                return;
                            }

                            // 처음 화면이 실행시 초기 설정을 실행한다
                            if (locationScene == null) {
                                // 화면 객체 지정
                                locationScene = new LocationScene(this, arSceneView);
                                locationScene.setAnchorRefreshInterval(10000);

                                // 지정한 좌표(tarLong, tarLati)에 AR을 추가한다.
                                LocationMarker layoutLocationMarker = new LocationMarker(
                                        tarLong,
                                        tarLati,
                                        getView()
                                );

                                // 매 프레임 AR을 업데이트 하는 이벤트.
                                layoutLocationMarker.setRenderEvent(new LocationNodeRender() {
                                    @Override
                                    public void render(LocationNode node) {
                                        View eView = exampleLayoutRenderable.getView();
                                        TextView buildName = eView.findViewById(R.id.textView1);
                                        buildName.setText(build); // 건물 이름 지정
                                        TextView distanceTextView = eView.findViewById(R.id.textView2); // 건물과의 거리 표시
                                        distanceTextView.setText(node.getDistance() + "M"); // getDistance() 메소드로 거리를 계산한다.

                                        // 화살표 네비게이션의 회전을 위해 현재 지점과 AR이 표시된 지점의 방위각을 구한다.
                                        bearing = (float) LocationUtils.bearing(
                                                locationScene.deviceLocation.currentBestLocation.getLatitude(), // 현재 위도
                                                locationScene.deviceLocation.currentBestLocation.getLongitude(), // 현재 경도
                                                tarLati, // AR의 위도와 경도
                                                tarLong);

                                        /*
                                         * getOrientation() 메소드로 현재 카메라의 방향을 구한다.
                                         * 방위각과 현재 카메라가 회전한 방향의 차이만큼 화살표를 회전시킨다.
                                         */
                                        float markerBearing = bearing - locationScene.deviceOrientation.getOrientation();
                                        markerBearing = markerBearing + 360;
                                        markerBearing = markerBearing % 360;
                                        double rotation = Math.floor(markerBearing);
                                        arrImg.setRotation((float)rotation-90);
                                    }
                                });
                                locationScene.mLocationMarkers.add(layoutLocationMarker);
                            }

                            Frame frame = arSceneView.getArFrame();
                            if (frame == null) {
                                return;
                            }

                            // ARcore는 AR 표시를 위해 평면 인식과 화면 밝기등을 확인한다.
                            if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                                Log.d("트래킹 상태", String.valueOf(frame.getCamera().getTrackingState()));
                                Log.d("실패 이유", String.valueOf(frame.getCamera().getTrackingFailureReason()));
                                return;
                            }

                            if (locationScene != null) {
                                locationScene.processFrame(frame);
                            }

                        });


        // GPS와 카메라의 권한이 없을 경우 요청한다.
        ARLocationPermissionHelper.requestPermission(this);
    }

    private Node getView() {
        Node base = new Node();
        base.setRenderable(exampleLayoutRenderable);
        Context c = this;
        View eView = exampleLayoutRenderable.getView();

        return base;
    }

    // 어플리케이션이 중지, 다시 실행됐을 때 실행할 메소드
    @Override
    protected void onResume() {
        super.onResume();

        if (locationScene != null) {
            locationScene.resume();
        }

        if (arSceneView.getSession() == null) {
            try {
                Session session = Utils.createArSession(this, installRequested);
                if (session == null) {
                    installRequested = ARLocationPermissionHelper.hasPermission(this);
                    return;
                } else {
                    arSceneView.setupSession(session);
                }
            } catch (UnavailableException e) {
                Utils.handleSessionException(this, e);
            }
        }

        try {
            arSceneView.resume();
        } catch (CameraNotAvailableException ex) {
            Utils.displayError(this, "카메라 실행 불가", ex);
            finish();
            return;
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        if (locationScene != null) {
            locationScene.pause();
        }

        arSceneView.pause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        arSceneView.destroy();
    }

    // 어플리케이션 권한 설정 메소드. 권한이 없으면 요청한다.
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] results) {
        if (!ARLocationPermissionHelper.hasPermission(this)) {
            if (!ARLocationPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                ARLocationPermissionHelper.launchPermissionSettings(this);
            } else {
                Toast.makeText(
                        this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG)
                        .show();
            }
            finish();
        }
    }

    // 전체화면을 설정하기 위한 메소드
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow()
                    .getDecorView()
                    .setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    // 지도 로딩시 콜백할 메소드. 충남대 중앙을 기준으로 지도를 띄우고 건물에 마커를 생성한다.
    @Override
    public void onMapReady(GoogleMap googleMap) {
        GoogleMap mMap = googleMap;

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(36.3666232, 127.3432415), 16));

        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        LatLng target = new LatLng(tarLati, tarLong);
        mMap.addMarker(new MarkerOptions().position(target).title(build));
    }
}
