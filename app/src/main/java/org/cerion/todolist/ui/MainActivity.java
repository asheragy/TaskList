package org.cerion.todolist.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.common.AccountPicker;

import org.cerion.todolist.data.Database;
import org.cerion.todolist.data.Prefs;
import org.cerion.todolist.R;
import org.cerion.todolist.data.Sync;
import org.cerion.todolist.data.Task;
import org.cerion.todolist.data.TaskList;
import org.cerion.todolist.dialogs.AlertDialogFragment;
import org.cerion.todolist.dialogs.TaskListDialogFragment;
import org.cerion.todolist.dialogs.TaskListDialogFragment.TaskListDialogListener;


import java.util.ArrayList;
import java.util.Date;

/* Checklist

Add a new task when there are 0 task lists, should we always have a default?
All fields both ways Tasks

Task add delete update WEB
Task add delete update Device
List add delete update WEB
List add delete update Device

conflict delete/update both directions
conflict delete both

conflict both modified but 1 completed, both directions, completed should stick

Add list on web with tasks (should be fine)
Add list on db with tasks then sync (ids should all be correct)
Add list on db with tasks then delete task and sync (no leftover deletions)
delete list on Web, all tasks should get deleted

*/

public class MainActivity extends ActionBarActivity
        implements TaskListDialogListener, AdapterView.OnItemClickListener
{
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int EDIT_TASK_REQUEST = 0;
    private static final int PICK_ACCOUNT_REQUEST = 1;

    private static final String NEW_LISTID = "new";
    private TextView mStatus;
    private ProgressBar mProgressBar;
    //private GestureDetector mGestureDetector;
    private ActionBar mActionBar;
    private ArrayList<TaskList> mTaskLists;
    private ArrayAdapter<TaskList> mActionBarAdapter;

    private static TaskList mCurrList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate " + (savedInstanceState == null ? "null" : "saveState"));
        setContentView(R.layout.activity_main);

        mActionBar = getSupportActionBar();
        mStatus = (TextView) findViewById(R.id.status);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        getListView().setEmptyView(findViewById(android.R.id.empty));
        getListView().setOnItemClickListener(this);
        registerForContextMenu(getListView());

        findViewById(R.id.syncImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSync();
            }
        });
        findViewById(R.id.logdb).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Database db = Database.getInstance(MainActivity.this);
                db.log();
                Prefs.logPrefs(MainActivity.this);
            }
        });

        updateLastSync();
        refreshLists();


        /* Not using for now, maybe switch to tab list or navigation drawer
        LinearLayout layout = (LinearLayout) findViewById(R.id.root);
        mGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            public static final int MAJOR_MOVE = 50; to do change at runtime using DisplayMetrics() class

            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                //Log.d(TAG,"onFling");
                int dx = (int) (e2.getX() - e1.getX());
                if (Math.abs(dx) > MAJOR_MOVE && Math.abs(velocityX) > Math.abs(velocityY)) {
                    int index = mActionBar.getSelectedNavigationIndex();

                    if (velocityX > 0) {
                        Log.d(TAG, "onPrevious");
                        index--;
                    } else {
                        Log.d(TAG, "onNext");
                        index++;
                    }

                    index = (index + mActionBar.getNavigationItemCount()) % mActionBar.getNavigationItemCount();
                    mActionBar.setSelectedNavigationItem(index);

                    return true;
                } else {
                    return false;
                }
            }
        });

        layout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View view, final MotionEvent event) {
                mGestureDetector.onTouchEvent(event);
                return true;
            }
        });
        */



    }

    //----- List Activity functions
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Task task = (Task) getListView().getItemAtPosition(position);
        onOpenTask(task);
    }

    private ListView getListView() {
        return (ListView) findViewById(android.R.id.list);
    }

    private Adapter getListAdapter() {
        return getListView().getAdapter();
    }

    private void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    //----- END List Activity functions

    public void onSync() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        boolean isConnected = networkInfo != null && networkInfo.isConnected();

        if (isConnected) {
            setInSync(true);
            Sync.syncTaskLists(this, new Sync.Callback() {
                @Override
                public void onAuthError(Exception e) {
                    setInSync(false);

                    if(e == null) {
                        onChooseAccount();
                    }
                    else {
                        DialogFragment dialog = AlertDialogFragment.newInstance("Auth Error", e.getMessage());
                        dialog.show(getFragmentManager(), "dialog");
                    }
                }

                @Override
                public void onSyncFinish(boolean bSuccess, Exception e) {
                    setInSync(false);

                    if(bSuccess) {
                        updateLastSync(); //Update last sync time only if successful
                    }
                    else {
                        String message = "Sync Failed, unknown error";
                        if (e != null)
                            message = e.getMessage();

                        DialogFragment dialog = AlertDialogFragment.newInstance("Sync failed", message);
                        dialog.show(getFragmentManager(), "dialog");
                    }

                    refreshAll(); //refresh since data may have changed
                }

            });
        } else {
            DialogFragment dialog = AlertDialogFragment.newInstance("Error", "Internet not available");
            dialog.show(getFragmentManager(), "dialog");
        }

    }

    public void updateLastSync() {
        String sText = "Last Sync: ";
        Date lastSync = Prefs.getPrefDate(this, Prefs.KEY_LAST_SYNC);
        if (lastSync == null || lastSync.getTime() == 0)
            sText += "Never";
        else
            sText += lastSync;

        mStatus.setText(sText);
    }

    public void setInSync(boolean bSyncing) {
        mProgressBar.setVisibility(bSyncing ? View.VISIBLE : View.INVISIBLE);
        getListView().setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);
        findViewById(R.id.syncImage).setVisibility(bSyncing ? View.INVISIBLE : View.VISIBLE);
    }

    public void onOpenTask(Task task) {
        Intent intent = new Intent(this, TaskActivity.class);
        if(task != null)
            intent.putExtra(TaskActivity.EXTRA_TASK, task);

        TaskList defaultList = TaskList.getDefault(mTaskLists);

        intent.putExtra(TaskActivity.EXTRA_DEFAULT_LIST, defaultList);
        intent.putExtra(TaskActivity.EXTRA_TASKLIST, mCurrList);
        startActivityForResult(intent, EDIT_TASK_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: " + resultCode);

        if(resultCode == RESULT_OK) {
            if (requestCode == EDIT_TASK_REQUEST)
                refreshTasks();
            else if (requestCode == PICK_ACCOUNT_REQUEST) {
                String currentAccount = Prefs.getPref(this,Prefs.KEY_ACCOUNT_NAME);
                String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);

                //If current account is set and different than selected account, logout first
                if(currentAccount.length() > 0 && !currentAccount.contentEquals(accountName))
                    onLogout();

                Prefs.savePref(this,Prefs.KEY_ACCOUNT_NAME,accountName);
            }
        }
    }

    /*
    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        boolean bResult = super.onTouchEvent(event);
        if (!bResult && mGestureDetector != null)
            bResult = mGestureDetector.onTouchEvent(event);
        return bResult;
    }
    */

    @Override
    public void onFinishTaskListDialog() {
        refreshLists();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_add) {
            onAddTaskList();
        }
        else if(id == R.id.action_rename) {
            TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_RENAME,mCurrList);
            dialog.show(getFragmentManager(), "dialog");
        }
        else if(id == R.id.action_add_task) {
            onOpenTask(null);
        }
        else if(id == R.id.action_account) {
            onChooseAccount();
        }
        else if(id == R.id.action_logout) {
            onLogout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void onLogout() {
        Log.d(TAG,"onLogout");
        Database db = Database.getInstance(MainActivity.this);
        db.clearSyncKeys();

        //Move unsynced task to this default list
        TaskList defaultList = TaskList.getDefault(mTaskLists);

        //Delete all non-temp Id records, also remove records marked as deleted
        ArrayList<Task> tasks = db.tasks.getList(null);
        for(Task task : tasks)
        {
            if(!task.hasTempId() || task.deleted)
                db.tasks.delete(task);
            else {
                //Since we are also removing synced lists, check if we need to move this task to an unsynced list
                TaskList list = new TaskList(task.listId,"");
                if(!list.hasTempId()) {
                    //Move this task to default list
                    db.setTaskIds(task,task.id,defaultList.id);
                }
            }
        }

        ArrayList<TaskList> lists = db.taskLists.getList();
        for(TaskList list : lists)
        {
            if(!list.hasTempId()) //don't delete unsynced lists
            {
                if(list.bDefault) //Keep default but assign temp id
                    db.setTaskListId(list,TaskList.generateId());
                else
                    db.taskLists.delete(list);
            }
        }

        //Remove prefs related to sync/account
        Prefs.remove(this,Prefs.KEY_LAST_SYNC);
        Prefs.remove(this,Prefs.KEY_ACCOUNT_NAME);
        Prefs.remove(this,Prefs.KEY_AUTHTOKEN);
        Prefs.remove(this,Prefs.KEY_AUTHTOKEN_DATE);

        refreshAll();
        updateLastSync();

        //Log data which should be empty except for un-synced records
        db.log();
        Prefs.logPrefs(MainActivity.this);
    }

    public void onChooseAccount() {
        //Find current account
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts = accountManager.getAccountsByType("com.google");
        String accountName = Prefs.getPref(this,Prefs.KEY_ACCOUNT_NAME);
        Account account = null;
        for(Account tmpAccount: accounts) {
            if(tmpAccount.name.contentEquals(accountName))
                account = tmpAccount;
        }

        //Display account picker
        Intent intent = AccountPicker.newChooseAccountIntent(account, null, new String[]{"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, PICK_ACCOUNT_REQUEST);
    }

    public void onAddTaskList() {
        TaskListDialogFragment dialog = TaskListDialogFragment.newInstance(TaskListDialogFragment.TYPE_ADD,null);
        dialog.show(getFragmentManager(), "dialog");
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId()==android.R.id.list) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.list_context_menu, menu);
        }
    }

    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Task task = (Task)getListAdapter().getItem(info.position);

        Database db;

        switch (item.getItemId())
        {
            //TODO, change delete text depending on record type
            case R.id.delete:
                Log.d(TAG,"onDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(true);
                db.tasks.update(task);
                refreshTasks();
                return true;
            case R.id.undelete:
                Log.d(TAG,"onUnDelete: " + task.title);
                db = Database.getInstance(this);
                task.setDeleted(false);
                db.tasks.update(task);
                refreshTasks();
                return true;

            //TODO, add complete and view
            default:
                return super.onContextItemSelected(item);
        }
    }

    public void refreshAll()
    {
        refreshLists();
        refreshTasks();
    }

    public void refreshLists()
    {
        Log.d(TAG, "refreshLists");
        Database db = Database.getInstance(this);
        ArrayList<TaskList> dbLists = db.taskLists.getList();
        if(dbLists.size() == 0)
        {
            Log.d(TAG,"No lists, adding default");
            TaskList defaultList = new TaskList("Default");
            defaultList.bDefault = true;
            db.taskLists.add(defaultList);
            dbLists = db.taskLists.getList(); //re-get list
        }

        if (mTaskLists == null)
            mTaskLists = new ArrayList<>();
        else
            mTaskLists.clear();

        TaskList allTasks = new TaskList(null, "All Tasks"); //null is placeholder for "all lists"
        if(mCurrList == null)
            mCurrList = allTasks;

        mTaskLists.add(allTasks);
        for(TaskList list : dbLists)
            mTaskLists.add(list);
        mTaskLists.add(new TaskList(NEW_LISTID, "<Add List>"));

        if(mActionBarAdapter == null) {
            mActionBarAdapter = new ArrayAdapter<>(mActionBar.getThemedContext(), android.R.layout.simple_spinner_dropdown_item, mTaskLists);
            ActionBar.OnNavigationListener navigationListener = new ActionBar.OnNavigationListener() {
                @Override
                public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                    Log.d(TAG,"position = " + itemPosition + " index = " + mActionBar.getSelectedNavigationIndex() );
                    if(itemPosition == mActionBar.getNavigationItemCount() - 1) {
                        onAddTaskList();
                        //Prefered action is to prevent current selection, not select the old one...
                        mActionBar.setSelectedNavigationItem( getListPosition(mCurrList) );
                    }
                    else {
                        Log.d(TAG,"navigation listener, refreshing tasks");
                        mCurrList = mTaskLists.get(itemPosition);
                        refreshTasks();
                    }

                    return false;
                }
            };

            mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            mActionBar.setListNavigationCallbacks(mActionBarAdapter, navigationListener);
        }
        else
            mActionBarAdapter.notifyDataSetChanged();

        mActionBar.setSelectedNavigationItem(getListPosition(mCurrList));
    }

    public void refreshTasks()
    {
        Log.d(TAG,"refreshTasks");
        Database db = Database.getInstance(this);
        ArrayList<Task> tasks = db.tasks.getList(mCurrList.id);

        TaskListAdapter myAdapter = new TaskListAdapter(this, tasks);
        setListAdapter(myAdapter);
    }

    private int getListPosition(TaskList list)
    {
        String id = list.id;
        int index = 0;
        if(id != null) {
            for (int i = 1; i < mActionBarAdapter.getCount() - 1; i++) { //Skip first and last list
                TaskList curr = mActionBarAdapter.getItem(i);
                if (curr.id.contentEquals(id))
                    index = i;
            }
        }

        return index;
    }

}
