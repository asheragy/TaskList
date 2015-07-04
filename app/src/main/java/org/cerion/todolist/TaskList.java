package org.cerion.todolist;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class TaskList
{
    public String id;
    public String title;
    public Date updated;
    private int renamed;

    public TaskList(String id, String title)
    {
        this(id,title,-1);
    }

    public TaskList(String id, String title, int renamed)
    {
        this.id = id;
        this.title = title;
        this.renamed = renamed;
    }

    public String toString()
    {
        return title;
    }

    public void setRenamed(boolean bRenamed)
    {
        if(renamed == -1)
            Log.e("TaskList","renamed undefined in TaskList");

        if(bRenamed)
            renamed = 1;
        else
            renamed = 0;
    }

    //TODO, can probably get rid of this, local lists have field and web lists do not
    public boolean hasRenamed()
    {
        return (renamed >= 0);
    }

    public boolean isRenamed()
    {
        if(renamed == -1)
            Log.e("TaskList","renamed undefined in TaskList");

        return (renamed == 1);
    }

    public static String generateId()
    {
        Random rand = new Random();
        int i = rand.nextInt();
        return "temp_" + i;
    }

    public boolean hasTempId()
    {
        return id.startsWith("temp_");
    }

    public static TaskList get(ArrayList<TaskList> lists, String sId)
    {
        for(TaskList list : lists)
        {
            if(list.id == null)
                continue;

            if(list.id.contentEquals(sId))
                return list;
        }

        return null;
    }
}