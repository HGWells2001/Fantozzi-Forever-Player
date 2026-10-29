package it.fantozziforever.player;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String PROGRAM = "https://www.raiplaysound.it/programmi/fantozziforever";
    // Titoli e durate della prima parte del catalogo verificati sulla fonte ufficiale.
    private static final String[] TITLES = {
        "La nascita di Fantozzi: dal cabaret alla televisione",
        "Paolo Villaggio in tv: Fantozzi, Kranz e Fracchia",
        "Fantozzi diventa un libro",
        "Fantozzi al cinema: l'inizio di una lunga saga",
        "Fantozzi e i coprotagonisti della saga",
        "Fantozzi: anatomia di una maschera"
    };
    private static final int[] MINUTES = {23,24,24,28,27,25};
    private final List<Integer> filtered = new ArrayList<>();
    private SharedPreferences saved;
    private BaseAdapter adapter;
    private ListView list;
    private EditText search;
    private TextView count, selected;
    private Button favorites;
    private boolean onlyFavorites;
    private UpdateChecker updater;
    private int active = -1;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        saved = getSharedPreferences("catalogo_fantozzi", MODE_PRIVATE);
        updater = new UpdateChecker(this);
        list = findViewById(R.id.episodes);
        count = findViewById(R.id.count);
        selected = findViewById(R.id.selected);
        search = findViewById(R.id.search);
        favorites = findViewById(R.id.favorites);
        active = saved.getInt("selezionata", -1);
        adapter = new BaseAdapter() {
            @Override public int getCount() { return filtered.size(); }
            @Override public Object getItem(int i) { return filtered.get(i); }
            @Override public long getItemId(int i) { return filtered.get(i); }
            @Override public View getView(int pos, View recycled, ViewGroup parent) {
                View row = recycled == null ? LayoutInflater.from(MainActivity.this).inflate(R.layout.episode_row, parent, false) : recycled;
                int index = filtered.get(pos);
                ((TextView)row.findViewById(R.id.title)).setText((index+1)+". "+TITLES[index]);
                ((TextView)row.findViewById(R.id.meta)).setText("27 marzo 2025  ·  "+MINUTES[index]+" min"+(active==index?"  ·  Selezionata":""));
                TextView star = row.findViewById(R.id.star);
                star.setText(saved.getBoolean("preferito_"+index,false)?"★":"☆");
                star.setOnClickListener(v -> {
                    saved.edit().putBoolean("preferito_"+index,!saved.getBoolean("preferito_"+index,false)).apply();
                    updateList();
                });
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((p,v,pos,id)->choose(filtered.get(pos)));
        findViewById(R.id.listen).setOnClickListener(v -> {
            if (active < 0) { android.widget.Toast.makeText(this,"Seleziona una puntata",android.widget.Toast.LENGTH_SHORT).show(); return; }
            startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(PROGRAM)));
        });
        findViewById(R.id.previous).setOnClickListener(v -> choose(Math.max(0,active-1)));
        findViewById(R.id.next).setOnClickListener(v -> choose(active<0?0:Math.min(TITLES.length-1,active+1)));
        favorites.setOnClickListener(v -> { onlyFavorites=!onlyFavorites; updateList(); });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int start,int c,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){updateList();}
            @Override public void afterTextChanged(Editable e){}
        });
        findViewById(R.id.menu).setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this,v);
            menu.getMenu().add("Cerca puntate").setOnMenuItemClickListener(item -> {
                search.setVisibility(search.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
                if(search.getVisibility()==View.VISIBLE) search.requestFocus();
                return true;
            });
            menu.getMenu().add("Mostra tutte le puntate").setOnMenuItemClickListener(item -> {
                onlyFavorites=false;search.setText("");updateList();return true;
            });
            menu.getMenu().add("Aggiornamenti").setOnMenuItemClickListener(item -> {
                updater.check(true); return true;
            });
            menu.getMenu().add("Informazioni").setOnMenuItemClickListener(item -> {
                new android.app.AlertDialog.Builder(this).setTitle("Fantozzi Forever Player")
                    .setMessage("Interfaccia nativa ispirata a Successo Player. Il catalogo è in fase di integrazione e l'ascolto si apre ancora sul sito ufficiale. App non affiliata a Rai.")
                    .setPositiveButton("Chiudi",null).show();return true;
            });
            menu.show();
        });
        if(active>=0 && active<TITLES.length) choose(active);
        updateList();
        updater.check(false);
    }
    @Override protected void onResume() {
        super.onResume();
        if (updater != null) updater.onResume();
    }
    private void choose(int i) {
        if(i<0 || i>=TITLES.length)return;
        active=i;
        saved.edit().putInt("selezionata",i).apply();
        selected.setText((i+1)+". "+TITLES[i]);
        updateList();
    }
    private void updateList(){
        filtered.clear();
        String query=search.getText().toString().toLowerCase(Locale.ITALIAN).trim();
        for(int i=0;i<TITLES.length;i++)
            if((!onlyFavorites || saved.getBoolean("preferito_"+i,false)) &&
               TITLES[i].toLowerCase(Locale.ITALIAN).contains(query)) filtered.add(i);
        favorites.setText(onlyFavorites?"★ Preferite":"☆ Tutte");
        count.setText(filtered.size()+" puntate · catalogo parziale");
        adapter.notifyDataSetChanged();
    }
}
