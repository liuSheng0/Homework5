package com.camp.bit.todolist;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.camp.bit.todolist.beans.Note;
import com.camp.bit.todolist.beans.State;
import com.camp.bit.todolist.db.TodoContract;
import com.camp.bit.todolist.db.TodoDbHelper;
import com.camp.bit.todolist.debug.DebugActivity;
import com.camp.bit.todolist.ui.NoteListAdapter;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ADD = 1002;

    private RecyclerView recyclerView;
    private NoteListAdapter notesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(
                        new Intent(MainActivity.this, NoteActivity.class),
                        REQUEST_CODE_ADD);
            }
        });

        recyclerView = findViewById(R.id.list_todo);
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        notesAdapter = new NoteListAdapter(new NoteOperator() {
            @Override
            public void deleteNote(Note note) {
                MainActivity.this.deleteNote(note);
            }

            @Override
            public void updateNote(Note note) {
                MainActivity.this.updateNode(note);
            }
        });
        recyclerView.setAdapter(notesAdapter);

        notesAdapter.refresh(loadNotesFromDatabase());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_settings:
                return true;
            case R.id.action_debug:
                startActivity(new Intent(this, DebugActivity.class));
                return true;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD
                && resultCode == Activity.RESULT_OK) {
            notesAdapter.refresh(loadNotesFromDatabase());
        }
    }

    private List<Note> loadNotesFromDatabase() {
        // TODO 从数据库中查询数据，并转换成 JavaBeans
        TodoDbHelper mDbHelper = new TodoDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();

        if (db == null){
            return Collections.emptyList();
        }
        List<Note> result = new LinkedList<>();
        Cursor cursor = null;
        try{
            cursor = db.query(TodoContract.TodoEntry.TABLE_NAME, null,
                    null,null,null,null,
                    TodoContract.TodoEntry.COLUMN_TIME + " DESC, " + TodoContract.TodoEntry.COLUMN_CONTENT);

            while (cursor.moveToNext()){
                String content = cursor.getString(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_CONTENT));
                long dateMs = cursor.getLong(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_TIME));
                long longId = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry._ID));
                int intState = cursor.getInt(cursor.getColumnIndex(TodoContract.TodoEntry.COLUMN_STATE));
                Note note = new Note(longId);
                note.setContent(content);
                note.setDate(new Date(dateMs));
                note.setState(State.from(intState));

                result.add(note);
            }
        }finally {
            if (cursor != null){
                cursor.close();
            }
        }
        return result;
    }

    private void deleteNote(Note note) {
        // TODO 删除数据
        TodoDbHelper mDbHelper = new TodoDbHelper(this);
        SQLiteDatabase db = mDbHelper.getReadableDatabase();
        String selection = TodoContract.TodoEntry._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(note.id)};
        int deletedRows = db.delete(TodoContract.TodoEntry.TABLE_NAME, selection, selectionArgs);
        notesAdapter.refresh(loadNotesFromDatabase());
    }

    private void updateNode(Note note) {
        TodoDbHelper mDbHelper = new TodoDbHelper(this);
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(TodoContract.TodoEntry.COLUMN_STATE ,note.getState().toString());

        String selection = TodoContract.TodoEntry._ID + " LIKE ?";
        String[] selectionArgs = {String.valueOf(note.id)};
        int deletedRows = db.update(TodoContract.TodoEntry.TABLE_NAME, values, selection, selectionArgs);
        notesAdapter.notifyDataSetChanged();
        // 更新数据
    }

}
