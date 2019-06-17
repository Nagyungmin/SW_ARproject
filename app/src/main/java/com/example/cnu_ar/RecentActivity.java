package com.example.cnu_ar;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

// 최근 기록 표시 클래스
public class RecentActivity extends AppCompatActivity {

    private List<String> buildList;          // 데이터를 넣은 리스트변수
    private List<String> pointList;
    private ListView listView;          // 검색을 보여줄 리스트변수
    private ListAdapter adapter;      // 리스트뷰에 연결할 아답터
    private ArrayList<String> arraylist;
    private Button delBtn;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recent_list);
        setTitle("최근 기록");
        listView = (ListView) findViewById(R.id.recentList);
        delBtn = (Button) findViewById(R.id.recentDelBtn);

        pref = getSharedPreferences("list", 0);

        buildList = new ArrayList<String>();
        pointList = new ArrayList<String>();

        settingList();

        // 리스트뷰 클릭 리스너. 리스트를 누르면 건물명과 좌표로 AR을 실행한다.
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String tempBuild = buildList.get(i);
                if (tempBuild.equals("최근기록이 없습니다!")) return;

                Intent intent = new Intent(getApplicationContext(), ARActivity.class);
                intent.putExtra("buildName", tempBuild);
                intent.putExtra("lati", Double.valueOf(pointList.get(i).split("#")[0]));
                intent.putExtra("long", Double.valueOf(pointList.get(i).split("#")[1]));
                startActivity(intent);
            }
        });

        // 최근 기록 삭제 버튼 클릭시 저장된 기록을 모두 지우고 액티비티 재실행
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = pref.edit();
                editor.clear();
                editor.commit();

                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        });
    }

    // 데이터를 쉐어드프리퍼런스에서 받아와 리스트에 넣음
    private void settingList() {

        String recent = pref.getString("recent", "최근기록이 없습니다!");
        String point = pref.getString("point", "최근기록이 없습니다!");

        String[] tempBuilding = recent.split("#");
        String[] tempPoint = point.split("/");
        for (int i = 0; i < tempBuilding.length; i++) {
            buildList.add(tempBuilding[i]);
            pointList.add(tempPoint[i]);
        }

        adapter = new ListAdapter(buildList, getApplicationContext());
        listView.setAdapter(adapter);
    }
}
